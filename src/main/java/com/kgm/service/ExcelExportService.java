package com.kgm.service;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.util.EmployeeBasicFieldUtil;
import com.kgm.util.EmployeeDocumentUtil;
import com.kgm.util.EmployeeFieldDefinitionCache;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ExcelExportService {
    private static final String EMPLOYEE_TABLE = "employees";
    private static final String EXPORT_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    private static final List<String> EXPORT_DATE_READ_FORMATS = List.of(
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy H:mm:ss",
            "MM/dd/yyyy HH:mm:ss",
            "M/d/yyyy HH:mm:ss",
            "M/d/yyyy H:mm:ss",
            "M/d/yy HH:mm:ss",
            "M/d/yy H:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd H:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd H:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "M/d/yyyy",
            "M/d/yy"
    );
    private static final List<String> FIXED_HEADERS = List.of(
            "UNT_CODE",
            "DESCR",
            "EMPLOYEE_CODE",
            "EMP_NAME",
            "FATHER_NAME",
            "DEPT_CODE",
            "DEPARTMENT",
            "DESIG_CODE",
            "DESIGNATION",
            "GENDER",
            "GRADE",
            "JOINING_DATE",
            "GROSS_SALARY",
            "PAY_SHEET",
            "PAY_CATEGORY",
            "BASIC",
            "COLA1",
            "COLA2",
            "COLA3",
            "COLA4",
            "COLA5",
            "COLA6_7",
            "COLA8",
            "COLA9",
            "COLA10",
            "COLA11",
            "H_RENT",
            "H_MAINTENANCE",
            "PB_SPECIAL1_2",
            "PB_SPECIAL3",
            "PB_SPECIAL4",
            "SPECIAL",
            "OTHER1",
            "OTHER2",
            "OTHER3",
            "MEDICAL",
            "CONVEYANCE",
            "UTILITY",
            "ENTERTAINMENT",
            "PAY_GROUP",
            "PAY_GROUP_DESC",
            "ORG_ID",
            "DIVISION",
            "FLAG",
            "EMP_STATUS",
            "SHIFT",
            "PROB_PERIOD",
            "CONFIRMING_ON",
            "PAY_AT_JOINING",
            "DOB",
            "NATIONALITY",
            "RELIGION",
            "BLOOD_GROUP",
            "M_STATUS",
            "BANK_NAME",
            "BANK_AC_NO",
            "SS_NO",
            "EOBI_NO",
            "CITY_VILLAGE",
            "DISTRICT",
            "CURRENT_ADR",
            "PERMANENT_ADR",
            "NID",
            "REST_DAY",
            "STAFF",
            "SS",
            "COLONY_RESIDENT",
            "TAX_NO",
            "PFUND_DEDUCTION",
            "EFU",
            "EFU_NO",
            "EOBI_STATUS",
            "DED_UNION",
            "APPLICATION_DATE",
            "PF_INTREST",
            "EXTRA_DUTY",
            "REFERENCE",
            "RELATIVE_DETAIL",
            "REFEMP_NAME",
            "REFEMP_DESIG",
            "REFEMP_DEPT",
            "EMP_CONTNO",
            "COMPANY_CAR",
            "PRE_WORKEXP",
            "PERSONAL_HOUSE_RENT",
            "RESIGN_REASON",
            "RESIGN_DATE",
            "COLONY_HOUSE_NUMBER",
            "CNIC_EXP_DATE",
            "CARDNO",
            "PAYROLL_FLAG",
            "REP_UNT",
            "REP_EMP_ID",
            "REP_EMP_DESIG_CODE",
            "REP_EMP_DEPT_CODE",
            "CHEST_CARD_STATUS",
            "DIS_CERTIFICATE",
            "EXTRA_DUTY_ALLOWANCE_DATE",
            "EMERGENCY_NO",
            "CNIC_FAMILY_NO",
            "REHIRING_STATUS",
            "CNIC_ISSUANCE_DATE",
            "CLEARANCE_STATUS",
            "HOD_CHECK",
            "REPORT_TO_EMP_ID",
            "REPORT_TO_UNT",
            "TAILOR_CATEGORY",
            "REP_EMP_TYPE",
            "WELLNESS_CLUB",
            "WELLNESS_CARD_ISSUE",
            "WELLNESS_CARD_NO",
            "SS_CARD_COPY",
            "EOBI_CARD_COPY",
            "ATT_CATEG",
            "PERSONAL_EMAIL",
            "OFFICIAL_EMAIL",
            "NIC_VERIFY",
            "NIC_VERIFY_DATE",
            "SEC_HEAD_CHK",
            "USER_ID",
            "PFUND_CODE",
            "MOTHER_NAME",
            "CLIPPER_PFUND_CODE",
            "IT_EQUIPMENT",
            "IT_EMAIL",
            "IT_INTERNET",
            "INTERNET_JUSTIFY",
            "IT_SERVICE_ALERT",
            "CITY_OF_BIRTH",
            "VAC_ID",
            "FIRST_DOSE",
            "SECOND_DOSE",
            "FIRST_VACC_DATE",
            "SECOND_VACC_DATE",
            "WELLNESS_CLUB_VALID_DATE",
            "BRANCH_CODE",
            "BRANCH_NAME",
            "ALT_SAT_TEAM",
            "ALT_SAT_START_DATE",
            "ALT_SAT_END_DATE",
            "ALT_SAT_NEXT_YEAR",
            "ALT_SAT_SHUFFLE",
            "ALT_SAT_UNLOCK_NEXT_YEAR",
            "EXP_IN_KTML"
    );
    private static final Map<String, String> SOURCE_ALIASES = sourceAliases();

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(String message, int completedRows, int totalRows, int percent);
    }

    static List<String> fixedHeaders() {
        return FIXED_HEADERS;
    }

    static String sourceAliasForHeader(String header) {
        return SOURCE_ALIASES.get(normalizeColumn(header));
    }

    public ExportResult exportEmployees(File file) throws IOException {
        return exportEmployees(file, null);
    }

    public ExportResult exportEmployees(File file, ProgressListener progressListener) throws IOException {
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        
        // Determine file format and create temp file with appropriate extension
        String fileName = file.getName().toLowerCase();
        String tempExtension = fileName.endsWith(".xls") ? ".xls" : ".xlsx";
        Path temporaryFile = parent == null
                ? Files.createTempFile("employee_export_", tempExtension)
                : Files.createTempFile(parent, "employee_export_", tempExtension);

        try {
            ExportResult result;
            reportProgress(progressListener, "Preparing export workbook...", 0, 0, 1);
            try (Connection connection = DatabaseConnection.getConnection()) {
                result = writeWorkbook(connection, temporaryFile.toFile(), tempExtension, progressListener);
            } catch (SQLException exception) {
                throw new IOException("Database error while exporting Excel: " + exception.getMessage(), exception);
            }
            reportProgress(progressListener, "Replacing exported file...", result.employeeCount(), result.employeeCount(), 96);
            makeEditableFile(target);
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            makeEditableFile(target);
            reportProgress(progressListener, "Checking saved workbook...", result.employeeCount(), result.employeeCount(), 98);
            validateGeneratedWorkbook(target);
            reportProgress(progressListener, "Export finished.", result.employeeCount(), result.employeeCount(), 100);
            return result;
        } catch (FileSystemException exception) {
            throw new IOException(
                    "Could not save the export because the file is open or locked. Close it in Excel and try again: "
                            + target.getFileName(),
                    exception
            );
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private ExportResult writeWorkbook(
            Connection connection,
            File temporaryFile,
            String fileExtension,
            ProgressListener progressListener
    ) throws SQLException, IOException {
        Map<String, String> dbColumns = employeeColumns(connection);
        Set<String> documentColumns = documentColumns(connection);
        List<ExportColumn> columns = exportColumns(dbColumns, documentColumns);
        boolean[] dateColumns = exportDateColumns(columns, metadataDateColumns());
        int[] widths = initialWidths(columns);
        int totalRows = employeeCount(connection);

        try (Workbook workbook = createWorkbook(fileExtension);
             FileOutputStream output = new FileOutputStream(temporaryFile)) {
            Sheet sheet = workbook.createSheet("Employee Export");
            sheet.createFreezePane(0, 1);

            CellStyle fixedHeaderStyle = headerStyle(workbook, IndexedColors.GREEN);
            CellStyle dynamicHeaderStyle = headerStyle(workbook, IndexedColors.TEAL);
            CellStyle normalCellStyle = textStyle(workbook, IndexedColors.AUTOMATIC, false);
            CellStyle dynamicCellStyle = textStyle(workbook, IndexedColors.LIGHT_TURQUOISE, true);

            Row header = sheet.createRow(0);
            for (int index = 0; index < columns.size(); index++) {
                ExportColumn column = columns.get(index);
                textCell(header, index, column.header(), column.dynamic() ? dynamicHeaderStyle : fixedHeaderStyle);
            }

            int rowCount = 0;
            if (totalRows == 0) {
                reportProgress(progressListener, "No employee rows to export yet; writing headers...", 0, 0, 12);
            } else {
                reportRowProgress(progressListener, "Exporting", 0, totalRows, 12);
            }
            reportProgress(progressListener, "Fetching employee rows from database...", 0, totalRows, 14);
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM " + quoteIdentifier(EMPLOYEE_TABLE) + " ORDER BY ID DESC")) {
                ResultSetMetaData metaData = rs.getMetaData();
                Map<String, Integer> resultColumns = resultColumns(metaData);
                while (rs.next()) {
                    Row row = sheet.createRow(++rowCount);
                    for (int index = 0; index < columns.size(); index++) {
                        ExportColumn column = columns.get(index);
                        String value = valueFor(rs, resultColumns, column.sourceColumn());
                        value = formatExportValue(dateColumns[index], value);
                        updateWidth(widths, index, value);
                        textCell(row, index, value, column.dynamic() ? dynamicCellStyle : normalCellStyle);
                    }
                    reportRowProgress(
                            progressListener,
                            "Exporting",
                            rowCount,
                            totalRows,
                            scaledProgress(rowCount, totalRows, 12, 88)
                    );
                }
            }

            reportProgress(progressListener, "Employee rows fetched. Preparing workbook layout...", rowCount, totalRows, 89);
            reportProgress(progressListener, "Sizing columns...", rowCount, totalRows, 90);
            for (int index = 0; index < columns.size(); index++) {
                sheet.setColumnWidth(index, widths[index]);
            }

            reportProgress(progressListener, "Writing workbook file...", rowCount, totalRows, 94);
            workbook.write(output);
            output.flush();
            return new ExportResult(rowCount, columns.size(), dynamicColumnCount(columns));
        }
    }

    private Workbook createWorkbook(String fileExtension) {
        if (".xls".equalsIgnoreCase(fileExtension)) {
            return new HSSFWorkbook();
        } else {
            return new XSSFWorkbook();
        }
    }

    private List<ExportColumn> exportColumns(Map<String, String> dbColumns, Set<String> documentColumns) {
        List<ExportColumn> columns = new ArrayList<>();
        Set<String> usedHeaders = new LinkedHashSet<>();
        Set<String> usedSourceColumns = new LinkedHashSet<>();

        for (String header : FIXED_HEADERS) {
            String normalizedHeader = normalizeColumn(header);
            String sourceColumn = sourceColumnForFixedHeader(header, dbColumns);
            columns.add(new ExportColumn(header, sourceColumn, false));
            usedHeaders.add(normalizedHeader);
            if (sourceColumn != null) {
                usedSourceColumns.add(normalizeColumn(sourceColumn));
            }
        }

        for (Map.Entry<String, String> dbColumn : dbColumns.entrySet()) {
            String normalizedColumn = dbColumn.getKey();
            if (usedSourceColumns.contains(normalizedColumn)
                    || usedHeaders.contains(normalizedColumn)
                    || documentColumns.contains(normalizedColumn)
                    || "ID".equals(normalizedColumn)) {
                continue;
            }
            columns.add(new ExportColumn(dbColumn.getValue().toUpperCase(Locale.ROOT), dbColumn.getValue(), true));
        }
        return columns;
    }

    private String sourceColumnForFixedHeader(String header, Map<String, String> dbColumns) {
        String source = dbColumns.get(normalizeColumn(header));
        if (source != null) {
            return source;
        }
        String alias = SOURCE_ALIASES.get(normalizeColumn(header));
        return alias == null ? null : dbColumns.get(normalizeColumn(alias));
    }

    private Map<String, String> employeeColumns(Connection connection) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, EMPLOYEE_TABLE, null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (column != null && !column.isBlank()) {
                    columns.put(normalizeColumn(column), column);
                }
            }
        }
        return columns;
    }

    private int employeeCount(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + quoteIdentifier(EMPLOYEE_TABLE))) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Set<String> documentColumns(Connection connection) {
        Set<String> columns = new LinkedHashSet<>();
        columns.add("EMP_IMG");
        for (EmployeeDocumentUtil.DocumentType type : EmployeeDocumentUtil.documentTypes()) {
            columns.add(normalizeColumn(type.employeeFieldName()));
        }
        try {
            for (EmployeeFieldDefinition definition : EmployeeFieldDefinitionCache.fields()) {
                if (definition.documentField()) {
                    columns.add(normalizeColumn(definition.columnName()));
                }
            }
        } catch (RuntimeException exception) {
            return columns;
        }
        return columns;
    }

    private Map<String, Integer> resultColumns(ResultSetMetaData metaData) throws SQLException {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String label = metaData.getColumnLabel(index);
            if (label == null || label.isBlank()) {
                label = metaData.getColumnName(index);
            }
            if (label != null && !label.isBlank()) {
                columns.put(normalizeColumn(label), index);
            }
        }
        return columns;
    }

    private String valueFor(ResultSet rs, Map<String, Integer> resultColumns, String sourceColumn) throws SQLException {
        if (sourceColumn == null || sourceColumn.isBlank()) {
            return "";
        }
        Integer index = resultColumns.get(normalizeColumn(sourceColumn));
        if (index == null) {
            return "";
        }
        String value = rs.getString(index);
        return isBlankExportValue(value) ? "" : value.trim();
    }

    private String formatExportValue(boolean dateColumn, String value) {
        if (!dateColumn) {
            return value;
        }
        return formatExportDateValue(value);
    }

    private boolean[] exportDateColumns(List<ExportColumn> columns, Set<String> metadataDateColumns) {
        boolean[] dateColumns = new boolean[columns.size()];
        for (int index = 0; index < columns.size(); index++) {
            dateColumns[index] = isExportDateColumn(columns.get(index), metadataDateColumns);
        }
        return dateColumns;
    }

    private boolean isExportDateColumn(ExportColumn column, Set<String> metadataDateColumns) {
        if (column == null) {
            return false;
        }
        String sourceColumn = normalizeColumn(column.sourceColumn());
        String header = normalizeColumn(column.header());
        return isDateColumnName(sourceColumn)
                || isDateColumnName(header)
                || metadataDateColumns.contains(sourceColumn);
    }

    private Set<String> metadataDateColumns() {
        Set<String> dateColumns = new LinkedHashSet<>();
        try {
            for (EmployeeFieldDefinition definition : EmployeeFieldDefinitionCache.fields()) {
                if (EmployeeBasicFieldUtil.isDateField(definition)) {
                    dateColumns.add(normalizeColumn(definition.columnName()));
                }
            }
        } catch (RuntimeException ignored) {
        }
        return dateColumns;
    }

    private static boolean isDateColumnName(String column) {
        String normalized = normalizeColumn(column);
        return "DOB".equals(normalized)
                || normalized.endsWith("_DATE")
                || normalized.endsWith("DATE");
    }

    static String formatExportDateValue(String value) {
        if (isBlankExportValue(value)) {
            return "";
        }
        String text = value.trim();
        for (String pattern : EXPORT_DATE_READ_FORMATS) {
            Date parsed = parseDate(text, pattern);
            if (parsed != null) {
                return dateFormatter(EXPORT_DATE_FORMAT).format(parsed);
            }
        }
        return text;
    }

    private static Date parseDate(String value, String pattern) {
        SimpleDateFormat format = dateFormatter(pattern);
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(value, position);
        return parsed != null && position.getIndex() == value.length() ? parsed : null;
    }

    private static SimpleDateFormat dateFormatter(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setLenient(false);
        return format;
    }

    private int[] initialWidths(List<ExportColumn> columns) {
        int[] widths = new int[columns.size()];
        for (int index = 0; index < columns.size(); index++) {
            widths[index] = widthFor(columns.get(index).header());
        }
        return widths;
    }

    private void updateWidth(int[] widths, int index, String value) {
        widths[index] = Math.max(widths[index], widthFor(value));
    }

    private int widthFor(String value) {
        int characters = Math.max(10, Math.min(42, value == null ? 0 : value.length() + 2));
        return characters * 256;
    }

    private int dynamicColumnCount(List<ExportColumn> columns) {
        int count = 0;
        for (ExportColumn column : columns) {
            if (column.dynamic()) {
                count++;
            }
        }
        return count;
    }

    private CellStyle headerStyle(Workbook workbook, IndexedColors fill) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(fill.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle textStyle(Workbook workbook, IndexedColors fill, boolean highlighted) {
        CellStyle style = workbook.createCellStyle();
        if (highlighted) {
            style.setFillForegroundColor(fill.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private void textCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static Map<String, String> sourceAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("PF_INTREST", "PF_INTEREST");
        return aliases;
    }

    private static String normalizeColumn(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void reportRowProgress(
            ProgressListener progressListener,
            String action,
            int completedRows,
            int totalRows,
            int percent
    ) {
        reportProgress(
                progressListener,
                action + " " + formattedRowProgress(completedRows, totalRows) + " rows...",
                completedRows,
                totalRows,
                percent
        );
    }

    private void reportProgress(
            ProgressListener progressListener,
            String message,
            int completedRows,
            int totalRows,
            int percent
    ) {
        if (progressListener == null) {
            return;
        }
        try {
            progressListener.onProgress(message, completedRows, totalRows, percent);
        } catch (RuntimeException ignored) {
        }
    }

    private static int scaledProgress(int completedRows, int totalRows, int start, int end) {
        if (totalRows <= 0) {
            return start;
        }
        int progress = start + (int) Math.round((end - start) * (completedRows / (double) totalRows));
        return Math.max(start, Math.min(end, progress));
    }

    private static String formattedRowProgress(int completedRows, int totalRows) {
        int width = Math.max(2, String.valueOf(Math.max(0, totalRows)).length());
        return String.format(Locale.ROOT, "%0" + width + "d/%d", Math.max(0, completedRows), Math.max(0, totalRows));
    }

    private static boolean isBlankExportValue(String value) {
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

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static void makeEditableFile(Path file) {
        try {
            if (Files.exists(file)) {
                file.toFile().setWritable(true, false);
                Files.setAttribute(file, "dos:readonly", false);
            }
        } catch (Exception ignored) {
        }
    }

    private static void validateGeneratedWorkbook(Path file) throws IOException {
        try (Workbook ignored = WorkbookFactory.create(file.toFile())) {
            // Opening the generated workbook catches incomplete files before users open Excel.
        }
    }

    public record ExportResult(int employeeCount, int columnCount, int dynamicColumnCount) {
    }

    private record ExportColumn(String header, String sourceColumn, boolean dynamic) {
    }
}
