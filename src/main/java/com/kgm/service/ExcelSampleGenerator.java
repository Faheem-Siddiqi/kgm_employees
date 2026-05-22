package com.kgm.service;

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

public final class ExcelSampleGenerator {
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
        Row sample = sheet.createRow(1);
        for (int index = 0; index < columns.size(); index++) {
            ExcelImportService.TemplateColumn column = columns.get(index);
            textCell(header, index, column.header(), headerStyle);
            textCell(sample, index, column.sampleValue(), textStyle);
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
        String[] headers = {"Field", "DB Column", "Required", "Type", "Valid / Sample Value"};
        unlockColumns(sheet, textStyle, headers.length);

        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            textCell(header, index, headers[index], headerStyle);
        }

        int rowIndex = 1;
        for (ExcelImportService.TemplateColumn column : columns) {
            Row row = sheet.createRow(rowIndex++);
            textCell(row, 0, column.header(), textStyle);
            textCell(row, 1, column.dbColumn(), textStyle);
            textCell(row, 2, column.required() ? "Yes" : "No", textStyle);
            textCell(row, 3, column.dateField() ? "Date" : column.dropdownOptions().isBlank() ? "Text" : "Dropdown", textStyle);
            textCell(row, 4, validValue(column), textStyle);
        }

        rowIndex++;
        textCell(sheet.createRow(rowIndex++), 0, "Rules", headerStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Only .xlsx or .xls files can be imported.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "CNIC must contain exactly 13 digits. Hyphens are allowed but ignored.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Date of Joining must be before Date of Resignation.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Date format: " + ExcelImportService.DATE_FORMAT_HINT + " (example: 2024-01-31).", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Document fields are not part of Excel import.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "The sample is rebuilt from current Field Management settings each time it is downloaded.", textStyle);
        textCell(sheet.createRow(rowIndex++), 0, "Fields in the Fundamentals category are required; other non-document fields are optional.", textStyle);
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

    private static String validValue(ExcelImportService.TemplateColumn column) {
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
            return ExcelImportService.DATE_FORMAT_HINT + " e.g. " + column.sampleValue();
        }
        return column.sampleValue();
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
