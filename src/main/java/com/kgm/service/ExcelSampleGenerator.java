package com.kgm.service;

import com.kgm.util.CnicFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public final class ExcelSampleGenerator {
    private static final int SAMPLE_ROW_COUNT = 5;
    private static final String[] SAMPLE_EMPLOYEE_CODES = {"00050", "00123", "000272", "000273", "000274"};
    private static final String[] SAMPLE_NAMES = {"Ali Khan", "Sana Malik", "Bilal Ahmed", "Ayesha Noor", "Usman Raza"};
    private static final String[] SAMPLE_FATHER_NAMES = {"Ahmed Khan", "Tariq Malik", "Naveed Ahmed", "Imran Noor", "Raza Ali"};
    private static final String[] SAMPLE_CNICS = {
            "35202-1234567-1", "42101-1234567-2", "61101-1234567-3", "37401-1234567-4", "17301-1234567-5"
    };
    private static final String[] SAMPLE_PHONES = {
            "0307-5011252", "0334-5040248", "0312-3456789", "0321-7654321", "0345-1234567"
    };
    private static final String[] SAMPLE_JOINING_DATES = {
            "8/23/2010 00:00:00", "2/19/2011 00:00:00", "7/30/2021 00:00:00",
            "12/30/2021 00:00:00", "11/1/2023 00:00:00"
    };
    private static final String[] SAMPLE_RESIGN_DATES = {
            "1/1/2024 00:00:00", "2/15/2024 00:00:00", "3/31/2024 00:00:00",
            "4/30/2024 00:00:00", "5/31/2024 00:00:00"
    };
    private static final String[] SAMPLE_DOBS = {
            "3/8/1984 00:00:00", "5/14/1990 00:00:00", "9/20/1988 00:00:00",
            "1/6/1992 00:00:00", "10/11/1986 00:00:00"
    };

    private ExcelSampleGenerator() {
    }

    public static void writeSampleWorkbook(File file) throws IOException {
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        Path temporaryFile = parent == null
                ? Files.createTempFile("employee_import_sample_", ".xlsx")
                : Files.createTempFile(parent, "employee_import_sample_", ".xlsx");
        try {
            List<ExcelImportService.TemplateColumn> columns = ExcelImportService.templateColumns();
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream output = new FileOutputStream(temporaryFile.toFile())) {
                CellStyle headerStyle = headerStyle(workbook);
                CellStyle textStyle = unlockedStyle(workbook);

                writeImportSheet(workbook, columns, headerStyle, textStyle);
                writeValidValuesSheet(workbook, columns, headerStyle, textStyle);
                workbook.write(output);
                output.flush();
            }
            makeEditableFile(target);
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            makeEditableFile(target);
            validateGeneratedWorkbook(target);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void writeImportSheet(
            Workbook workbook,
            List<ExcelImportService.TemplateColumn> columns,
            CellStyle headerStyle,
            CellStyle textStyle
    ) {
        Sheet sheet = workbook.createSheet("Employee Import");
        unlockColumns(sheet, textStyle, columns.size());

        Row header = sheet.createRow(0);
        for (int index = 0; index < columns.size(); index++) {
            ExcelImportService.TemplateColumn column = columns.get(index);
            textCell(header, index, column.header(), headerStyle);
        }

        for (int sampleIndex = 0; sampleIndex < SAMPLE_ROW_COUNT; sampleIndex++) {
            Row sample = sheet.createRow(sampleIndex + 1);
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                ExcelImportService.TemplateColumn column = columns.get(columnIndex);
                textCell(sample, columnIndex, sampleValue(column, sampleIndex), textStyle);
            }
        }

        for (int index = 0; index < columns.size(); index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private static void writeValidValuesSheet(
            Workbook workbook,
            List<ExcelImportService.TemplateColumn> columns,
            CellStyle headerStyle,
            CellStyle textStyle
    ) {
        Sheet sheet = workbook.createSheet("Valid Values");
        String[] headers = {"Field", "DB Column", "Category", "Required", "Type", "Valid / Sample Value", "Rule / Comment"};
        unlockColumns(sheet, textStyle, headers.length);

        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            textCell(header, index, headers[index], headerStyle);
        }

        int rowIndex = 1;
        for (ExcelImportService.TemplateColumn column : columns) {
            Row row = sheet.createRow(rowIndex++);
            textCell(row, 0, column.header(), textStyle);
            textCell(row, 1, column.dbColumn().isBlank() ? "-" : column.dbColumn(), textStyle);
            textCell(row, 2, column.category().isBlank() ? "Details" : column.category(), textStyle);
            textCell(row, 3, column.required() ? "Yes" : "No", textStyle);
            textCell(row, 4, column.importable()
                    ? column.dateField() ? "Date" : column.dropdownOptions().isBlank() ? "Text" : "Dropdown"
                    : "Ignored", textStyle);
            textCell(row, 5, validValue(column), textStyle);
            textCell(row, 6, ruleComment(column), textStyle);
        }

        rowIndex++;
        textCell(sheet.createRow(rowIndex++), 0, "Rules", headerStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Only .xlsx or .xls files can be imported.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "CNIC must use format " + CnicFormatter.FORMAT_EXAMPLE + ", including both hyphens.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Phone must use format 0307-5011252, including the hyphen.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Date of Joining must be before Date of Resignation.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Date format: " + ExcelImportService.DATE_FORMAT_HINT + " (example: 8/23/2010 00:00:00).", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Document fields are not part of Excel import.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "The sample is rebuilt from current Field Management settings each time it is downloaded.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Required fields come from Field Management > Required Fields.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "The first five rows are examples. Replace them with employee records before import.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Do not add, remove, or rename headers. Unknown headers are rejected during import.", textStyle);

        rowIndex++;
        textCell(sheet.createRow(rowIndex++), 0, "Gender", headerStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Male", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Female", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Other", textStyle);

        rowIndex++;
        textCell(sheet.createRow(rowIndex++), 0, "Common Reasons", headerStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Layoff", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Retirement", textStyle);
        textCell(sheet.createRow(rowIndex), 0, "Other", textStyle);

        for (int index = 0; index < headers.length; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private static String sampleValue(ExcelImportService.TemplateColumn column, int sampleIndex) {
        String dbColumn = column.dbColumn();
        if (!column.importable()) {
            return "";
        }
        return switch (dbColumn.toUpperCase(Locale.ROOT)) {
            case "EMPLOYEE_CODE" -> SAMPLE_EMPLOYEE_CODES[sampleIndex];
            case "UNT_CODE", "DESCR" -> "KGM";
            case "EMP_NAME" -> SAMPLE_NAMES[sampleIndex];
            case "FATHER_NAME" -> SAMPLE_FATHER_NAMES[sampleIndex];
            case "NID" -> SAMPLE_CNICS[sampleIndex];
            case "EMP_CONTNO", "EMERGENCY_NO" -> SAMPLE_PHONES[sampleIndex];
            case "PERSONAL_EMAIL" -> "employee" + (sampleIndex + 1) + "@example.com";
            case "DEPARTMENT" -> sampleFrom(new String[]{"HR", "Finance", "Production", "Admin", "IT"}, sampleIndex);
            case "DESIGNATION" -> sampleFrom(new String[]{"Officer", "Assistant Manager", "Supervisor", "Clerk", "Manager"}, sampleIndex);
            case "SECTION" -> "N/A";
            case "GRADE" -> sampleFrom(new String[]{"G-5", "M-14", "S-2", "G-7", "M-10"}, sampleIndex);
            case "SHIFT" -> sampleFrom(new String[]{"Morning", "G", "Evening", "Night", "General"}, sampleIndex);
            case "DOB" -> SAMPLE_DOBS[sampleIndex];
            case "JOINING_DATE" -> SAMPLE_JOINING_DATES[sampleIndex];
            case "RESIGN_DATE" -> SAMPLE_RESIGN_DATES[sampleIndex];
            case "GENDER" -> sampleFrom(new String[]{"Male", "Female", "Male", "Female", "Male"}, sampleIndex);
            case "RESIGN_REASON" -> sampleFrom(new String[]{"Retirement", "Layoff", "Other", "Retirement", "Other"}, sampleIndex);
            case "PERMANENT_ADR", "CURRENT_ADR" -> "House " + (sampleIndex + 1) + ", Lahore";
            default -> fallbackSampleValue(column, sampleIndex);
        };
    }

    private static String fallbackSampleValue(ExcelImportService.TemplateColumn column, int sampleIndex) {
        if (!column.dropdownOptions().isBlank()) {
            String[] options = column.dropdownOptions().split("\\s*,\\s*");
            return options[Math.min(sampleIndex, options.length - 1)];
        }
        if (column.dateField()) {
            return SAMPLE_JOINING_DATES[sampleIndex];
        }
        if (!"N/A".equalsIgnoreCase(column.sampleValue())) {
            return column.sampleValue();
        }
        return "Sample " + (sampleIndex + 1);
    }

    private static String sampleFrom(String[] values, int sampleIndex) {
        return values[Math.min(sampleIndex, values.length - 1)];
    }

    private static String validValue(ExcelImportService.TemplateColumn column) {
        if (!column.importable()) {
            return "";
        }
        if ("GENDER".equalsIgnoreCase(column.dbColumn())) {
            return column.dropdownOptions().isBlank() ? "Male, Female, Other" : column.dropdownOptions();
        }
        if ("RESIGN_REASON".equalsIgnoreCase(column.dbColumn())) {
            return column.dropdownOptions().isBlank() ? "Layoff, Retirement, Other" : column.dropdownOptions();
        }
        if (!column.dropdownOptions().isBlank()) {
            return column.dropdownOptions();
        }
        if (column.dateField()) {
            return ExcelImportService.DATE_FORMAT_HINT
                    + " e.g. 8/23/2010 00:00:00, 2/19/2011 00:00:00";
        }
        return column.sampleValue();
    }

    private static String ruleComment(ExcelImportService.TemplateColumn column) {
        if (!column.importable()) {
            return "Included so import sample has the same headers/count as export. Import ignores this column.";
        }
        String dbColumn = column.dbColumn();
        if ("NID".equalsIgnoreCase(dbColumn)) {
            return "Use CNIC format " + CnicFormatter.FORMAT_EXAMPLE + ", including both hyphens.";
        }
        if ("EMP_CONTNO".equalsIgnoreCase(dbColumn) || "EMERGENCY_NO".equalsIgnoreCase(dbColumn)) {
            return "Use phone format 0307-5011252, including the hyphen.";
        }
        if ("JOINING_DATE".equalsIgnoreCase(dbColumn)) {
            return "Use " + ExcelImportService.DATE_FORMAT_HINT
                    + ". Date of Joining must be before Date of Resignation.";
        }
        if ("RESIGN_DATE".equalsIgnoreCase(dbColumn)) {
            return "Use " + ExcelImportService.DATE_FORMAT_HINT
                    + ". Date of Resignation must be after Date of Joining.";
        }
        if (column.dateField()) {
            return "Use date format " + ExcelImportService.DATE_FORMAT_HINT + ".";
        }
        if (column.required()) {
            return "Required for standard import.";
        }
        if (!column.dropdownOptions().isBlank()) {
            return "Use one of the listed values unless this field allows a custom value.";
        }
        return "";
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setLocked(false);
        return style;
    }

    private static CellStyle unlockedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setLocked(false);
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return style;
    }

    private static void textCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void unlockColumns(Sheet sheet, CellStyle editableStyle, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.setDefaultColumnStyle(index, editableStyle);
        }
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
}
