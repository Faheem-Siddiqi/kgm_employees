package com.kgm.service;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.dao.EmployeeRegistrationDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
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
    public static final String DATE_FORMAT_HINT = "yyyy-MM-dd";

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_HINT);
    private static final List<String> FALLBACK_CORE_COLUMN_ORDER = EmployeeBasicFieldUtil.BASIC_COLUMNS;
    private static final Set<String> FALLBACK_REQUIRED_STANDARD_COLUMNS = EmployeeBasicFieldUtil.REQUIRED_COLUMNS;
    private static final Set<String> REQUIRED_LEGACY_COLUMNS = Set.of("EMPLOYEE_CODE");
    private static final Set<String> DATE_COLUMNS = EmployeeBasicFieldUtil.DATE_COLUMNS;
    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd H:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy H:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm")
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
        int imported = 0;

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
                    registrationDao.insertEmployee(rowData.employee(), rowData.importColumns());
                    imported++;
                } catch (RowImportException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, exception.getMessage());
                } catch (RuntimeException exception) {
                    addSkippedRow(skippedRows, rowIndex + 1, friendlyRuntimeMessage(exception));
                }
            }
            if (!hasRows) {
                throw new IllegalArgumentException("No employee rows found. Add employee records below the header row before importing.");
            }
        } catch (SQLException exception) {
            throw new IOException("Database error while importing Excel: " + exception.getMessage(), exception);
        }

        return new ImportResult(imported, skippedRows);
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
        for (EmployeeFieldDefinition definition : EmployeeBasicFieldUtil.basicDefinitions(definitions)) {
            String column = definition.columnName().toUpperCase(Locale.ROOT);
            byColumn.remove(column);
            columns.add(templateColumn(column, definition));
        }

        List<EmployeeFieldDefinition> remaining = new ArrayList<>(byColumn.values());
        remaining.sort(Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER));
        for (EmployeeFieldDefinition definition : remaining) {
            columns.add(templateColumn(definition.columnName(), definition));
        }
        return columns;
    }

    private static List<TemplateColumn> fallbackTemplateColumns() {
        List<TemplateColumn> columns = new ArrayList<>();
        for (String column : FALLBACK_CORE_COLUMN_ORDER) {
            columns.add(templateColumn(column, null));
        }
        return columns;
    }

    private static TemplateColumn templateColumn(String columnName, EmployeeFieldDefinition definition) {
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
                templateHeader(column, definition),
                column,
                dateField,
                required,
                sampleValue(column, dateField, dropdownOptions),
                String.join(", ", dropdownOptions)
        );
    }

    private static String templateHeader(String column, EmployeeFieldDefinition definition) {
        if (definition != null && definition.label() != null && !definition.label().isBlank()) {
            return definition.label();
        }
        return switch (column) {
            case "EMPLOYEE_CODE" -> "Employee ID";
            case "EMP_NAME" -> "Name";
            case "NID" -> "CNIC";
            case "EMP_CONTNO" -> "Phone";
            case "PERSONAL_EMAIL" -> "Email";
            case "JOINING_DATE" -> "Date of Joining";
            case "RESIGN_DATE" -> "Date of Resignation";
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
            case "EMP_NAME" -> "Ali Khan";
            case "FATHER_NAME" -> "Ahmed Khan";
            case "NID" -> "3520212345671";
            case "EMP_CONTNO" -> "03001234567";
            case "PERSONAL_EMAIL" -> "ali.khan@example.com";
            case "DEPARTMENT" -> "HR";
            case "DESIGNATION" -> "Officer";
            case "SECTION" -> "Admin";
            case "GRADE" -> "G-5";
            case "SHIFT" -> "Morning";
            case "DOB" -> "1990-01-15";
            case "GENDER" -> "Male";
            case "RESIGN_REASON" -> "Retirement";
            case "JOINING_DATE" -> "2020-01-01";
            case "RESIGN_DATE" -> "2024-01-01";
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

        for (Map.Entry<String, List<String>> entry : EXTRA_ALIASES.entrySet()) {
            EmployeeFieldDefinition definition = byColumn.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            for (String alias : entry.getValue()) {
                addAlias(byAlias, alias, definition);
            }
        }
        return new FieldCatalog(byColumn, byAlias, documentAliases, standardRequiredColumns);
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
        if (headerRow == null) {
            validateHeaders(headers, unknownHeaders, importType, catalog);
            return headers;
        }

        short lastCell = headerRow.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
            String headerText = cellText(headerRow.getCell(cellIndex), formatter, evaluator);
            if (headerText.isBlank()) {
                continue;
            }

            String normalized = normalizeHeader(headerText);
            if (catalog.documentAliases().contains(normalized)) {
                unknownHeaders.add(headerText);
                continue;
            }

            EmployeeFieldDefinition definition = catalog.byAlias().get(normalized);
            if (definition == null) {
                unknownHeaders.add(headerText);
                continue;
            }

            String column = definition.columnName().toUpperCase(Locale.ROOT);
            headers.putIfAbsent(column, new HeaderBinding(definition, cellIndex));
        }

        validateHeaders(headers, unknownHeaders, importType, catalog);
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
            ImportType importType,
            FieldCatalog catalog
    ) {
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
            List<String> parts = new ArrayList<>();
            if (!missing.isEmpty()) {
                parts.add("Missing: " + String.join(", ", missing));
            }
            if (!unknownHeaders.isEmpty()) {
                parts.add("Unknown or unsupported: " + String.join(", ", unknownHeaders));
            }
            throw new HeaderImportException("Header issue detected. " + String.join(". ", parts)
                    + ". Upload the correct Excel sample file with the correct headers.");
        }
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
        if (isBlankValue(cnic) || cnic.replaceAll("\\D", "").length() != 13) {
            throw new RowImportException("CNIC must contain exactly 13 digits.");
        }

        Date joining = parseDbDate(rowData.values().get("JOINING_DATE"));
        Date resignation = parseDbDate(rowData.values().get("RESIGN_DATE"));
        if (joining != null && resignation != null && !joining.before(resignation)) {
            throw new RowImportException("Date of Resignation must be after Date of Joining.");
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
                return digitsOnly(value.getStringValue());
            }
        }
        return digitsOnly(cellText(cell, formatter, evaluator));
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
        return aliases;
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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
            boolean dateField,
            boolean required,
            String sampleValue,
            String dropdownOptions
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
            Set<String> standardRequiredColumns
    ) {
    }

    private record HeaderBinding(EmployeeFieldDefinition definition, int cellIndex) {
    }

    private record RowData(Employee employee, List<String> importColumns, Map<String, String> values) {
    }

    private static class RowImportException extends Exception {
        private RowImportException(String message) {
            super(message);
        }
    }
}
