package com.kgm.service;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.dao.EmployeeRegistrationDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.util.CnicFormatter;
import com.kgm.util.EmployeeBasicFieldUtil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExcelImportService {
    public static final String DATE_FORMAT_HINT = "M/d/yyyy HH:mm:ss";
    private static final String INTERNAL_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private static final String PHONE_FORMAT_EXAMPLE = "0307-5011252";

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat(INTERNAL_DATE_FORMAT);
    private static final List<String> FALLBACK_CORE_COLUMN_ORDER = EmployeeBasicFieldUtil.BASIC_COLUMNS;
    private static final Set<String> FALLBACK_REQUIRED_STANDARD_COLUMNS = EmployeeBasicFieldUtil.REQUIRED_COLUMNS;
    private static final Set<String> REQUIRED_LEGACY_COLUMNS = Set.of("EMPLOYEE_CODE");
    private static final Set<String> DATE_COLUMNS = EmployeeBasicFieldUtil.DATE_COLUMNS;
    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yy HH:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yy H:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm:ss")
    );
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy")
    );
    private static final Map<String, List<String>> EXTRA_ALIASES = aliases();

    public enum ImportType {
        STANDARD("Import New / Standard Employee Data"),
        LEGACY("Import Legacy / Old Employee Data");

        private final String label;

        ImportType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public ImportResult importEmployees(File file) throws IOException {
        return importEmployees(file, ImportType.STANDARD);
    }

    public ImportResult importEmployees(File file, ImportType importType) throws IOException {
        if (!isExcelFile(file)) {
            throw new IllegalArgumentException("Only Excel workbooks can be imported.");
        }

        ImportType selectedType = importType == null ? ImportType.STANDARD : importType;
        List<String> skippedRows = new ArrayList<>();
        List<PendingImportRow> pendingRows = new ArrayList<>();

        try (Workbook workbook = openWorkbook(file);
             Connection connection = DatabaseConnection.getConnection()) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheet to import.");
            }

            EmployeeFieldDefinitionDao fieldDao = new EmployeeFieldDefinitionDao(connection);
            FieldCatalog catalog = loadFieldCatalog(fieldDao);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, HeaderBinding> headers = readHeaders(
                    sheet,
                    formatter,
                    evaluator,
                    catalog,
                    selectedType
            );

            EmployeeRegistrationDao registrationDao = new EmployeeRegistrationDao(connection);
            boolean hasRows = false;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter, evaluator)) {
                    continue;
                }
                hasRows = true;

                try {
                    RowData rowData = employeeFromRow(row, headers, formatter, evaluator);
                    validateEmployeeRow(rowData, selectedType, catalog);
                    pendingRows.add(new PendingImportRow(rowIndex + 1, rowData));
                } catch (RowImportException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, exception.getMessage());
                } catch (RuntimeException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, friendlyRuntimeMessage(exception));
                }
            }
            if (!hasRows) {
                throw new IllegalArgumentException("No employee rows found. Add employee records below the header row before importing.");
            }

            rejectDuplicateEmployees(connection, pendingRows, skippedRows);
            int imported = savePendingRows(registrationDao, pendingRows, skippedRows);
            return new ImportResult(imported, skippedRows);
        } catch (SQLException exception) {
            throw new IOException("Database error while importing Excel: " + exception.getMessage(), exception);
        }
    }

    public static boolean isExcelFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        return (name.endsWith(".xlsx") || name.endsWith(".xls")) && !name.startsWith("~$");
    }

    public static List<TemplateColumn> templateColumns() {
        try {
            return templateColumns(new EmployeeFieldDefinitionDao().listFields());
        } catch (RuntimeException exception) {
            return fallbackTemplateColumns();
        }
    }

    private static List<TemplateColumn> templateColumns(List<EmployeeFieldDefinition> definitions) {
        Map<String, EmployeeFieldDefinition> byColumn = new LinkedHashMap<>();
        for (EmployeeFieldDefinition definition : definitions) {
            if (definition.documentField()
                    || "ID".equalsIgnoreCase(definition.columnName())) {
                continue;
            }
            byColumn.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
        }

        List<TemplateColumn> columns = new ArrayList<>();
        Set<String> usedHeaders = new LinkedHashSet<>();
        Set<String> usedColumns = new LinkedHashSet<>();
        for (EmployeeFieldDefinition definition : EmployeeBasicFieldUtil.basicDefinitions(definitions)) {
            String column = definition.columnName().toUpperCase(Locale.ROOT);
            byColumn.remove(column);
            addImportTemplateColumn(columns, usedHeaders, usedColumns, definition);
        }

        List<EmployeeFieldDefinition> remaining = new ArrayList<>(byColumn.values());
        remaining.sort(Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER));
        for (EmployeeFieldDefinition definition : remaining) {
            addImportTemplateColumn(columns, usedHeaders, usedColumns, definition);
        }

        Map<String, EmployeeFieldDefinition> importableColumns = byColumnFromDefinitions(definitions);
        for (String fixedHeader : ExcelExportService.fixedHeaders()) {
            String normalizedHeader = normalizeColumn(fixedHeader);
            if (usedHeaders.contains(normalizedHeader)) {
                continue;
            }
            String sourceColumn = sourceColumnForFixedHeader(fixedHeader, importableColumns);
            if (sourceColumn != null && usedColumns.contains(normalizeColumn(sourceColumn))) {
                continue;
            }
            columns.add(ignoredTemplateColumn(fixedHeader));
            usedHeaders.add(normalizedHeader);
        }
        return columns;
    }

    private static void addImportTemplateColumn(
            List<TemplateColumn> columns,
            Set<String> usedHeaders,
            Set<String> usedColumns,
            EmployeeFieldDefinition definition
    ) {
        String column = definition.columnName().toUpperCase(Locale.ROOT);
        String header = exportHeaderForColumn(column);
        columns.add(templateColumn(header, column, definition));
        usedHeaders.add(normalizeColumn(header));
        usedColumns.add(column);
    }

    private static Map<String, EmployeeFieldDefinition> byColumnFromDefinitions(List<EmployeeFieldDefinition> definitions) {
        Map<String, EmployeeFieldDefinition> byColumn = new LinkedHashMap<>();
        for (EmployeeFieldDefinition definition : definitions) {
            if (definition.documentField()
                    || "ID".equalsIgnoreCase(definition.columnName())) {
                continue;
            }
            byColumn.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
        }
        return byColumn;
    }

    private static String exportHeaderForColumn(String column) {
        String normalizedColumn = normalizeColumn(column);
        for (String fixedHeader : ExcelExportService.fixedHeaders()) {
            if (normalizeColumn(fixedHeader).equals(normalizedColumn)) {
                return fixedHeader;
            }
            String alias = ExcelExportService.sourceAliasForHeader(fixedHeader);
            if (alias != null && normalizeColumn(alias).equals(normalizedColumn)) {
                return fixedHeader;
            }
        }
        return column.toUpperCase(Locale.ROOT);
    }

    private static String sourceColumnForFixedHeader(
            String fixedHeader,
            Map<String, EmployeeFieldDefinition> byColumn
    ) {
        EmployeeFieldDefinition direct = byColumn.get(normalizeColumn(fixedHeader));
        if (direct != null) {
            return direct.columnName();
        }
        String alias = ExcelExportService.sourceAliasForHeader(fixedHeader);
        if (alias == null) {
            return null;
        }
        EmployeeFieldDefinition aliased = byColumn.get(normalizeColumn(alias));
        return aliased == null ? null : aliased.columnName();
    }

    private static List<TemplateColumn> fallbackTemplateColumns() {
        List<TemplateColumn> columns = new ArrayList<>();
        for (String column : FALLBACK_CORE_COLUMN_ORDER) {
            columns.add(templateColumn(column, column, null));
        }
        return columns;
    }

    private static TemplateColumn templateColumn(String header, String columnName, EmployeeFieldDefinition definition) {
        String column = columnName.toUpperCase(Locale.ROOT);
        boolean dateField = DATE_COLUMNS.contains(column) || (definition != null && EmployeeBasicFieldUtil.isDateField(definition));
        boolean required = definition == null
                ? FALLBACK_REQUIRED_STANDARD_COLUMNS.contains(column)
                : EmployeeBasicFieldUtil.isRequired(definition);
        List<String> dropdownOptions = definition != null && EmployeeBasicFieldUtil.isDropdownField(definition)
                ? List.of(EmployeeBasicFieldUtil.dropdownOptions(definition, false))
                : EmployeeBasicFieldUtil.isComboField(column)
                        ? List.of(EmployeeBasicFieldUtil.comboOptions(column, false))
                        : List.of();
        return new TemplateColumn(
                header,
                column,
                definition == null ? "" : definition.heading(),
                dateField,
                required,
                sampleValue(column, dateField, dropdownOptions),
                String.join(", ", dropdownOptions),
                true
        );
    }

    private static TemplateColumn ignoredTemplateColumn(String header) {
        return new TemplateColumn(
                header,
                "",
                "Export / ERP only",
                false,
                false,
                "",
                "",
                false
        );
    }

    private static String templateHeader(String column, EmployeeFieldDefinition definition) {
        if (definition != null && definition.label() != null && !definition.label().isBlank()) {
            return definition.label();
        }
        return switch (column) {
            case "EMPLOYEE_CODE" -> "Employee ID";
            case "UNT_CODE" -> "Unit Code";
            case "DESCR" -> "DESCR";
            case "EMP_NAME" -> "Name";
            case "NID" -> "CNIC";
            case "EMP_CONTNO" -> "Phone";
            case "PERSONAL_EMAIL" -> "Email";
            case "JOINING_DATE" -> "Date of Joining";
            case "RESIGN_DATE" -> "Date of Resignation";
            case "CURRENT_ADR" -> "Current Address";
            case "PERMANENT_ADR" -> "Permanent Address";
            case "DOB" -> "Date of Birth";
            default -> definition == null ? titleFromColumn(column) : definition.label();
        };
    }

    private static String sampleValue(String column, boolean dateField, List<String> dropdownOptions) {
        if (!dropdownOptions.isEmpty() && !EmployeeBasicFieldUtil.isBasicField(column)) {
            return dropdownOptions.get(0);
        }
        return switch (column) {
            case "EMPLOYEE_CODE" -> "EMP-1001";
            case "UNT_CODE" -> "KGM";
            case "DESCR" -> "KGM";
            case "EMP_NAME" -> "Ali Khan";
            case "FATHER_NAME" -> "Ahmed Khan";
            case "NID" -> CnicFormatter.FORMAT_EXAMPLE;
            case "EMP_CONTNO" -> PHONE_FORMAT_EXAMPLE;
            case "PERSONAL_EMAIL" -> "ali.khan@example.com";
            case "DEPARTMENT" -> "HR";
            case "DESIGNATION" -> "Officer";
            case "SECTION" -> "N/A";
            case "GRADE" -> "G-5";
            case "SHIFT" -> "Morning";
            case "DOB" -> "3/8/1984 00:00:00";
            case "GENDER" -> "Male";
            case "RESIGN_REASON" -> "Retirement";
            case "JOINING_DATE" -> "8/23/2010 00:00:00";
            case "RESIGN_DATE" -> "1/1/2024 00:00:00";
            case "CURRENT_ADR" -> "House 1, Lahore";
            case "PERMANENT_ADR" -> "House 1, Lahore";
            default -> dateField ? DATE_FORMAT_HINT : "N/A";
        };
    }

    private Workbook openWorkbook(File file) throws IOException {
        try {
            byte[] workbookBytes = Files.readAllBytes(file.toPath());
            return WorkbookFactory.create(new ByteArrayInputStream(workbookBytes));
        } catch (FileSystemException exception) {
            throw new IOException(
                    "Could not read the Excel file because it is open or locked by another process. Close it in Excel and try again: "
                            + file.getName(),
                    exception
            );
        }
    }

    private FieldCatalog loadFieldCatalog(EmployeeFieldDefinitionDao fieldDao) {
        Map<String, EmployeeFieldDefinition> byColumn = new LinkedHashMap<>();
        Map<String, EmployeeFieldDefinition> byAlias = new HashMap<>();
        Set<String> documentAliases = new LinkedHashSet<>();
        Set<String> standardRequiredColumns = new LinkedHashSet<>();
        Set<String> ignoredHeaderAliases = new LinkedHashSet<>();

        for (EmployeeFieldDefinition definition : fieldDao.listFields()) {
            String column = definition.columnName().toUpperCase(Locale.ROOT);
            if ("ID".equals(column)) {
                continue;
            }
            if (definition.documentField()) {
                documentAliases.add(normalizeHeader(definition.label()));
                documentAliases.add(normalizeHeader(definition.columnName()));
                continue;
            }
            byColumn.put(column, definition);
            if (EmployeeBasicFieldUtil.isFundamentalsField(definition)) {
                standardRequiredColumns.add(column);
            }
            addAlias(byAlias, definition.label(), definition);
            addAlias(byAlias, definition.columnName(), definition);
        }
        if (standardRequiredColumns.isEmpty()) {
            standardRequiredColumns.addAll(FALLBACK_REQUIRED_STANDARD_COLUMNS);
        }
        List<TemplateColumn> templateColumns = templateColumns(new ArrayList<>(byColumn.values()));
        List<String> standardHeaderColumns = standardHeaderColumns(templateColumns);
        for (TemplateColumn templateColumn : templateColumns) {
            if (!templateColumn.importable()) {
                ignoredHeaderAliases.add(normalizeHeader(templateColumn.header()));
            }
        }

        for (Map.Entry<String, List<String>> entry : EXTRA_ALIASES.entrySet()) {
            EmployeeFieldDefinition definition = byColumn.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            for (String alias : entry.getValue()) {
                addAlias(byAlias, alias, definition);
            }
        }
        return new FieldCatalog(
                byColumn,
                byAlias,
                documentAliases,
                ignoredHeaderAliases,
                standardRequiredColumns,
                standardHeaderColumns
        );
    }

    private List<String> standardHeaderColumns(List<TemplateColumn> templateColumns) {
        List<String> columns = new ArrayList<>();
        for (TemplateColumn templateColumn : templateColumns) {
            columns.add(normalizeHeader(templateColumn.header()));
        }
        return columns;
    }

    private Map<String, HeaderBinding> readHeaders(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            FieldCatalog catalog,
            ImportType importType
    ) {
        Row headerRow = sheet.getRow(0);
        Map<String, HeaderBinding> headers = new LinkedHashMap<>();
        List<String> unknownHeaders = new ArrayList<>();
        List<String> actualHeaderColumns = new ArrayList<>();
        if (headerRow == null) {
            validateHeaders(headers, unknownHeaders, actualHeaderColumns, importType, catalog);
            return headers;
        }

        short lastCell = headerRow.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
            String headerText = cellText(headerRow.getCell(cellIndex), formatter, evaluator);
            if (headerText.isBlank()) {
                continue;
            }

            String normalized = normalizeHeader(headerText);
            if (catalog.ignoredHeaderAliases().contains(normalized)) {
                actualHeaderColumns.add(normalized);
                continue;
            }

            if (catalog.documentAliases().contains(normalized)) {
                unknownHeaders.add(headerText);
                actualHeaderColumns.add(normalized);
                continue;
            }

            EmployeeFieldDefinition definition = catalog.byAlias().get(normalized);
            if (definition == null) {
                unknownHeaders.add(headerText);
                actualHeaderColumns.add(normalized);
                continue;
            }

            String column = definition.columnName().toUpperCase(Locale.ROOT);
            actualHeaderColumns.add(normalized);
            headers.putIfAbsent(column, new HeaderBinding(definition, cellIndex));
        }

        validateHeaders(headers, unknownHeaders, actualHeaderColumns, importType, catalog);
        return headers;
    }

    private RowData employeeFromRow(
            Row row,
            Map<String, HeaderBinding> headers,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) throws RowImportException {
        Employee employee = new Employee();
        List<String> importColumns = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();

        for (HeaderBinding binding : headers.values()) {
            EmployeeFieldDefinition definition = binding.definition();
            String column = definition.columnName().toUpperCase(Locale.ROOT);
            Cell cell = row.getCell(binding.cellIndex());
            String value;
            if (isDateColumn(definition)) {
                Date date = dateValue(cell, formatter, evaluator);
                String raw = cellText(cell, formatter, evaluator);
                if (!raw.isBlank() && date == null) {
                    issues.add(templateHeader(column, definition) + " must be a valid date using " + DATE_FORMAT_HINT + ".");
                    continue;
                }
                value = date == null ? "" : DB_DATE_FORMAT.format(date);
            } else if ("NID".equals(column)) {
                value = cnicValue(cell, formatter, evaluator);
            } else {
                value = cellText(cell, formatter, evaluator);
            }

            writeValue(employee, column, value);
            values.put(column, value);
            importColumns.add(column);
        }

        if (!issues.isEmpty()) {
            throw new RowImportException(String.join(" ", issues));
        }
        return new RowData(employee, importColumns, values);
    }

    private void validateHeaders(
            Map<String, HeaderBinding> headers,
            List<String> unknownHeaders,
            List<String> actualHeaderColumns,
            ImportType importType,
            FieldCatalog catalog
    ) {
        if (importType == ImportType.STANDARD
                && !sameColumns(actualHeaderColumns, catalog.standardHeaderColumns())) {
            throw new HeaderImportException(
                    "Row 1 must match the latest sample headers and order."
            );
        }

        Set<String> required = importType == ImportType.LEGACY
                ? REQUIRED_LEGACY_COLUMNS
                : catalog.standardRequiredColumns();
        List<String> missing = new ArrayList<>();
        for (String column : required) {
            if (!headers.containsKey(column)) {
                missing.add(templateHeader(column, catalog.byColumn().get(column)));
            }
        }
        if (!missing.isEmpty() || !unknownHeaders.isEmpty()) {
            if (importType == ImportType.LEGACY && missing.isEmpty()) {
                return;
            }
            throw new HeaderImportException(
                    "Row 1 has missing or unsupported headers."
            );
        }
    }

    private boolean sameColumns(List<String> actualColumns, List<String> expectedColumns) {
        if (actualColumns.size() != expectedColumns.size()) {
            return false;
        }
        for (int index = 0; index < expectedColumns.size(); index++) {
            if (!expectedColumns.get(index).equalsIgnoreCase(actualColumns.get(index))) {
                return false;
            }
        }
        return true;
    }

    private void validateEmployeeRow(
            RowData rowData,
            ImportType importType,
            FieldCatalog catalog
    ) throws RowImportException {
        Set<String> required = importType == ImportType.LEGACY
                ? REQUIRED_LEGACY_COLUMNS
                : catalog.standardRequiredColumns();
        List<String> missing = new ArrayList<>();
        for (String column : required) {
            if (isBlankValue(rowData.values().get(column))) {
                missing.add(templateHeader(column, catalog.byColumn().get(column)));
            }
        }
        if (!missing.isEmpty()) {
            throw new RowImportException("Missing required fields: " + String.join(", ", missing));
        }

        String cnic = rowData.values().get("NID");
        if (isBlankValue(cnic) || !CnicFormatter.isValid(cnic)) {
            throw new RowImportException("CNIC must use format " + CnicFormatter.FORMAT_EXAMPLE + ".");
        }

        validatePhone(rowData, catalog, "EMP_CONTNO");
        validatePhone(rowData, catalog, "EMERGENCY_NO");

        Date joining = parseDbDate(rowData.values().get("JOINING_DATE"));
        Date resignation = parseDbDate(rowData.values().get("RESIGN_DATE"));
        if (joining != null && resignation != null && !joining.before(resignation)) {
            throw new RowImportException("Date of Resignation must be after Date of Joining.");
        }
    }

    private void rejectDuplicateEmployees(
            Connection connection,
            List<PendingImportRow> pendingRows,
            List<String> skippedRows
    ) throws SQLException {
        Set<String> seenInWorkbook = new LinkedHashSet<>();
        Set<String> duplicateInWorkbook = new LinkedHashSet<>();
        List<String> employeeCodes = new ArrayList<>();
        for (PendingImportRow pendingRow : pendingRows) {
            String employeeCode = normalizedEmployeeCode(pendingRow.rowData().values().get("EMPLOYEE_CODE"));
            if (employeeCode.isBlank()) {
                continue;
            }
            if (!seenInWorkbook.add(employeeCode)) {
                duplicateInWorkbook.add(employeeCode);
            }
            employeeCodes.add(employeeCode);
        }

        Set<String> duplicateInDatabase = existingEmployeeCodes(connection, employeeCodes);
        pendingRows.removeIf(pendingRow -> {
            String employeeCode = normalizedEmployeeCode(pendingRow.rowData().values().get("EMPLOYEE_CODE"));
            if (duplicateInWorkbook.contains(employeeCode)) {
                addSkippedRow(skippedRows, pendingRow.rowNumber(), "Employee ID is duplicated in this workbook.");
                return true;
            }
            if (duplicateInDatabase.contains(employeeCode)) {
                addSkippedRow(skippedRows, pendingRow.rowNumber(), "Employee ID already exists.");
                return true;
            }
            return false;
        });
    }

    private Set<String> existingEmployeeCodes(Connection connection, List<String> employeeCodes) throws SQLException {
        Set<String> existing = new LinkedHashSet<>();
        List<String> uniqueCodes = new ArrayList<>(new LinkedHashSet<>(employeeCodes));
        for (int start = 0; start < uniqueCodes.size(); start += 500) {
            List<String> batch = uniqueCodes.subList(start, Math.min(start + 500, uniqueCodes.size()));
            if (batch.isEmpty()) {
                continue;
            }
            String sql = "SELECT EMPLOYEE_CODE FROM employees WHERE EMPLOYEE_CODE IN (" + placeholders(batch.size()) + ")";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int index = 0; index < batch.size(); index++) {
                    ps.setString(index + 1, batch.get(index));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        existing.add(normalizedEmployeeCode(rs.getString("EMPLOYEE_CODE")));
                    }
                }
            }
        }
        return existing;
    }

    private int savePendingRows(
            EmployeeRegistrationDao registrationDao,
            List<PendingImportRow> pendingRows,
            List<String> skippedRows
    ) {
        if (pendingRows.isEmpty()) {
            return 0;
        }

        List<Employee> employees = new ArrayList<>();
        List<List<String>> importColumns = new ArrayList<>();
        for (PendingImportRow pendingRow : pendingRows) {
            employees.add(pendingRow.rowData().employee());
            importColumns.add(pendingRow.rowData().importColumns());
        }

        try {
            return registrationDao.insertEmployees(employees, importColumns);
        } catch (RuntimeException batchException) {
            int imported = 0;
            for (PendingImportRow pendingRow : pendingRows) {
                try {
                    registrationDao.insertEmployee(
                            pendingRow.rowData().employee(),
                            pendingRow.rowData().importColumns()
                    );
                    imported++;
                } catch (RuntimeException rowException) {
                    addSkippedRow(skippedRows, pendingRow.rowNumber(), friendlyRuntimeMessage(rowException));
                }
            }
            return imported;
        }
    }

    private static boolean isPhoneNumber(String value) {
        return value != null && value.trim().matches("03\\d{2}-\\d{7}");
    }

    private void validatePhone(RowData rowData, FieldCatalog catalog, String column) throws RowImportException {
        String phone = rowData.values().get(column);
        if (!isBlankValue(phone) && !isPhoneNumber(phone)) {
            throw new RowImportException(templateHeader(column, catalog.byColumn().get(column))
                    + " must use format " + PHONE_FORMAT_EXAMPLE + ".");
        }
    }

    private boolean isDateColumn(EmployeeFieldDefinition definition) {
        String column = definition.columnName().toUpperCase(Locale.ROOT);
        return DATE_COLUMNS.contains(column) || EmployeeBasicFieldUtil.isDateField(definition);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        short lastCell = row.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
            if (!cellText(row.getCell(cellIndex), formatter, evaluator).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Date dateValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            return DateUtil.getJavaDate(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
            CellValue value = evaluator.evaluate(cell);
            if (value == null) {
                return null;
            }
            if (value.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(value.getNumberValue())) {
                return DateUtil.getJavaDate(value.getNumberValue());
            }
            if (value.getCellType() == CellType.STRING) {
                return dateTextValue(value.getStringValue());
            }
        }
        return dateTextValue(cellText(cell, formatter, evaluator));
    }

    private Date dateTextValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(text, format);
                return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(text, format);
                return Date.from(date.atTime(LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Date parseDbDate(String value) {
        if (isBlankValue(value)) {
            return null;
        }
        try {
            return DB_DATE_FORMAT.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String cellText(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private String cnicValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return numericIdentifier(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
            CellValue value = evaluator.evaluate(cell);
            if (value == null) {
                return "";
            }
            if (value.getCellType() == CellType.NUMERIC) {
                return numericIdentifier(value.getNumberValue());
            }
            if (value.getCellType() == CellType.STRING) {
                return value.getStringValue().trim();
            }
        }
        return cellText(cell, formatter, evaluator);
    }

    private String numericIdentifier(double value) {
        if (!Double.isFinite(value)) {
            return "";
        }
        return BigDecimal.valueOf(value)
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString()
                .replaceAll("\\D", "");
    }

    private void writeValue(Employee employee, String column, String value) {
        try {
            Field field = Employee.class.getDeclaredField(column);
            field.setAccessible(true);
            field.set(employee, value);
        } catch (ReflectiveOperationException exception) {
            employee.setDynamicField(column, value);
        }
    }

    private String friendlyRuntimeMessage(RuntimeException exception) {
        Throwable cursor = exception;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("duplicate")) {
            return "Employee ID already exists.";
        }
        return message == null || message.isBlank() ? "Employee row could not be imported." : message;
    }

    private void addSkippedRow(List<String> skippedRows, int rowNumber, String reason) {
        skippedRows.add("Row " + rowNumber + ": " + reason);
    }

    private static void addAlias(Map<String, EmployeeFieldDefinition> aliases, String alias, EmployeeFieldDefinition definition) {
        String normalized = normalizeHeader(alias);
        if (!normalized.isBlank()) {
            aliases.putIfAbsent(normalized, definition);
        }
    }

    private static Map<String, List<String>> aliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put("EMPLOYEE_CODE", List.of("Employee ID", "Employee Code", "ID", "Emp ID", "Emp Code"));
        aliases.put("UNT_CODE", List.of("Unit Code", "UNT Code"));
        aliases.put("DESCR", List.of("DESCR", "Description"));
        aliases.put("EMP_NAME", List.of("Name", "Employee Name", "Worker Name"));
        aliases.put("FATHER_NAME", List.of("Father Name", "Father"));
        aliases.put("NID", List.of("CNIC", "CNIC / NID", "NID", "NIC"));
        aliases.put("EMP_CONTNO", List.of("Phone", "Contact", "Contact Number", "Mobile"));
        aliases.put("PERSONAL_EMAIL", List.of("Email", "Personal Email"));
        aliases.put("DEPARTMENT", List.of("Department", "Dept"));
        aliases.put("DESIGNATION", List.of("Designation", "Position"));
        aliases.put("SECTION", List.of("Section"));
        aliases.put("GRADE", List.of("Grade"));
        aliases.put("SHIFT", List.of("Shift"));
        aliases.put("DOB", List.of("DOB", "Date of Birth", "Birth Date"));
        aliases.put("GENDER", List.of("Gender", "Sex"));
        aliases.put("RESIGN_REASON", List.of("Reason", "Resign Reason", "Leaving Reason", "Reason of Leaving"));
        aliases.put("JOINING_DATE", List.of("Date of Joining", "Joining Date", "Date of Arrival", "Arrival Date"));
        aliases.put("RESIGN_DATE", List.of("Date of Resignation", "Resign Date", "Leaving Date", "Date of Leaving"));
        aliases.put("PERMANENT_ADR", List.of("Permanent Address", "Permanent_Adress", "Permanent_Adresss", "Address"));
        aliases.put("CURRENT_ADR", List.of("Current Address"));
        aliases.put("PF_INTEREST", List.of("PF_INTREST"));
        return aliases;
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String normalizeColumn(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static boolean isBlankValue(String value) {
        if (value == null) {
            return true;
        }
        String text = value.trim();
        return text.isEmpty()
                || text.equalsIgnoreCase("N/A")
                || text.equalsIgnoreCase("NA")
                || text.equalsIgnoreCase("NULL")
                || text.equals("-");
    }

    private static String normalizedEmployeeCode(String value) {
        return value == null ? "" : value.trim();
    }

    private static String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private static String titleFromColumn(String column) {
        String[] parts = column.toLowerCase(Locale.ROOT).split("_+");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return title.isEmpty() ? column : title.toString();
    }

    public record TemplateColumn(
            String header,
            String dbColumn,
            String category,
            boolean dateField,
            boolean required,
            String sampleValue,
            String dropdownOptions,
            boolean importable
    ) {
    }

    public record ImportResult(int importedCount, List<String> skippedRows) {
    }

    public static class HeaderImportException extends IllegalArgumentException {
        private HeaderImportException(String message) {
            super(message);
        }
    }

    private record FieldCatalog(
            Map<String, EmployeeFieldDefinition> byColumn,
            Map<String, EmployeeFieldDefinition> byAlias,
            Set<String> documentAliases,
            Set<String> ignoredHeaderAliases,
            Set<String> standardRequiredColumns,
            List<String> standardHeaderColumns
    ) {
    }

    private record HeaderBinding(EmployeeFieldDefinition definition, int cellIndex) {
    }

    private record RowData(Employee employee, List<String> importColumns, Map<String, String> values) {
    }

    private record PendingImportRow(int rowNumber, RowData rowData) {
    }

    private static class RowImportException extends Exception {
        private RowImportException(String message) {
            super(message);
        }
    }
}
