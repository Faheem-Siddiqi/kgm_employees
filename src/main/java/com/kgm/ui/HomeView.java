package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.service.ExcelExportService;
import com.kgm.service.ExcelImportService;
import com.kgm.service.ExcelSampleGenerator;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.panel.EmployeeTablePanel;
import com.kgm.ui.panel.ExcelImportButton;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.HomeStatsPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class HomeView extends JFrame {
    private final ExcelImportService excelImportService = new ExcelImportService();
    private final ExcelExportService excelExportService = new ExcelExportService();
    private EmployeeTablePanel tablePanel;
    private HomeStatsPanel statsPanel;
    private ExcelImportButton excelBtn;
    private JButton refreshBtn;

    public HomeView() {
        HomeViewHelper.applyFrame(this);

        EmployeeRecordDao repo = new EmployeeRecordDao();
        tablePanel = new EmployeeTablePanel(repo);
        statsPanel = new HomeStatsPanel(repo);

        // Wire "Show in Table" from chart cards to the table filtering
        statsPanel.setShowInTableHandler(command -> handleShowInTable(command, repo));

        // Wire table filter callback to toggle Refresh / Clear Filter button text
        tablePanel.setOnFilterChanged(filterLabel -> {
            if (filterLabel == null) {
                refreshBtn.setText("Refresh");
                HomeViewHelper.styleRefreshButton(refreshBtn);
            } else {
                refreshBtn.setText("Clear Filter (" + filterLabel + ")");
                refreshBtn.setBackground(new Color(180, 60, 50));
                refreshBtn.setForeground(Color.WHITE);
                refreshBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
        });

        JPanel top = HomeViewHelper.createTopPanel();
        top.add(new HeaderPanel("Home Dashboard"), BorderLayout.NORTH);

        JPanel searchRow = HomeViewHelper.createSearchRow();
        JTextField searchField = HomeViewHelper.createSearchField("Search Employee Code");

        JButton searchBtn = new JButton("Search");
        HomeViewHelper.styleSearchButton(searchBtn);
        searchField.addActionListener(e -> searchBtn.doClick());

        JButton clearBtn = new JButton("Clear");
        HomeViewHelper.styleClearButton(clearBtn);
        HomeViewHelper.setTextButtonEnabled(clearBtn, false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateClearButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateClearButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateClearButton();
            }

            private void updateClearButton() {
                HomeViewHelper.setTextButtonEnabled(clearBtn, !searchField.getText().trim().isEmpty());
            }
        });

        searchBtn.addActionListener(e -> {
            String empCode = searchField.getText().trim();
            if (empCode.isEmpty()) {
                DialogHelper.warning(this, "Employee Code Required", "Enter Employee Code.");
                return;
            }

            var emp = repo.getEmployeeByCode(empCode);
            if (emp != null) {
                tablePanel.showSingleEmployee(emp);
            } else {
                DialogHelper.info(this, "No Result", "No employee found.");
            }
        });

        clearBtn.addActionListener(e -> {
            searchField.setText("");
            reloadHomeData();
            HomeViewHelper.setTextButtonEnabled(clearBtn, false);
        });

        HomeViewHelper.addSearchControls(searchRow, searchField, searchBtn, clearBtn);

        JPanel northContainer = HomeViewHelper.createNorthContainer();
        northContainer.add(top, BorderLayout.NORTH);
        northContainer.add(searchRow, BorderLayout.CENTER);

        JPanel btnRow = HomeViewHelper.createButtonRow();
        excelBtn = new ExcelImportButton(this::showExcelImportActions);

        JButton addBtn = new JButton("Add Employee");
        HomeViewHelper.styleAddButton(addBtn);
        addBtn.addActionListener(e -> {
            new EmployeeRegistrationView().setVisible(true);
            dispose();
        });

        refreshBtn = new JButton("Refresh");
        HomeViewHelper.styleRefreshButton(refreshBtn);
        refreshBtn.addActionListener(e -> {
            if (tablePanel.getActiveFilterLabel() != null) {
                // Clear the filter and reload
                tablePanel.clearFilter();
            } else {
                reloadHomeData();
            }
        });

        JButton settingsBtn = new JButton("Settings");
        HomeViewHelper.styleAddButton(settingsBtn);
        settingsBtn.addActionListener(e -> {
            new FieldManagementView();
            dispose();
        });

        btnRow.add(excelBtn);
        btnRow.add(addBtn);
        btnRow.add(settingsBtn);
        btnRow.add(refreshBtn);
        northContainer.add(btnRow, BorderLayout.SOUTH);

        JPanel body = HomeViewHelper.createBodyPanel();
        body.add(tablePanel, BorderLayout.NORTH);
        body.add(statsPanel, BorderLayout.CENTER);

        JPanel mainContent = HomeViewHelper.createMainContentPanel();
        mainContent.add(northContainer, BorderLayout.NORTH);
        mainContent.add(body, BorderLayout.CENTER);
        add(HomeViewHelper.createMainScrollPane(mainContent), BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * Handles "Show in Table" clicks from chart cards.
     * Format: columnName::value::displayLabel
     */
    private void handleShowInTable(String command, EmployeeRecordDao repo) {
        if (command == null || command.isBlank()) {
            return;
        }
        String[] parts = command.split("::");
        if (parts.length < 2) {
            return;
        }
        String columnName = parts[0];
        String value = parts[1];
        String displayLabel = parts.length >= 3 ? parts[2] : columnName;

        if ("ALL".equalsIgnoreCase(value)) {
            // Show all employees for this column category (no specific value filter)
            tablePanel.filterByColumn(columnName, null, displayLabel);
        } else {
            tablePanel.filterByColumn(columnName, value, displayLabel);
        }
    }

    private void showExcelImportActions() {
        int selected = DialogHelper.option(
                this,
                "Excel",
                "Choose an Excel action.\n\nThe sample and export are rebuilt from current database fields. Document upload fields are not exported as dynamic columns.",
                "Import Excel",
                "Download Sample",
                "Export Excel"
        );
        if (selected == 0) {
            chooseExcelImportFile();
        } else if (selected == 1) {
            downloadSampleExcel();
        } else if (selected == 2) {
            exportEmployeeExcel();
        }
    }

    private void chooseExcelImportFile() {
        File selectedFile = FileUploadCard.chooseFile(
                this,
                "Import Employee Excel Sheet",
                FileUploadCard.excelWorkbooks()
        );
        if (selectedFile == null) {
            return;
        }

        if (!ExcelImportService.isExcelFile(selectedFile)) {
            showUnsupportedImportFileDialog();
            return;
        }

        ExcelImportService.ImportType importType = chooseExcelImportType();
        if (importType == null) {
            return;
        }
        importEmployeesFromExcel(selectedFile, importType);
    }

    private ExcelImportService.ImportType chooseExcelImportType() {
        JRadioButton standardImport = new JRadioButton("Import New / Standard Employee Data", true);
        JRadioButton legacyImport = new JRadioButton("Import Legacy / Old Employee Data");
        standardImport.setOpaque(false);
        legacyImport.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        group.add(standardImport);
        group.add(legacyImport);

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Choose how this Excel workbook should be imported:"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(standardImport);
        panel.add(new JLabel("Requires every Basic field from the sample workbook."));
        panel.add(Box.createVerticalStrut(8));
        panel.add(legacyImport);
        panel.add(new JLabel("For old records. Requires Employee ID, while CNIC/date rules still apply."));

        int selected = DialogHelper.formOption(
                this,
                "Select Import Type",
                panel,
                "Continue",
                "Cancel"
        );
        if (selected != 0) {
            return null;
        }
        return legacyImport.isSelected()
                ? ExcelImportService.ImportType.LEGACY
                : ExcelImportService.ImportType.STANDARD;
    }

    private void importEmployeesFromExcel(File file, ExcelImportService.ImportType importType) {
        if (!ExcelImportService.isExcelFile(file)) {
            showUnsupportedImportFileDialog();
            return;
        }

        setImportButtonEnabled(false);
        SwingWorker<ExcelImportService.ImportResult, Void> worker = new SwingWorker<>() {
            protected ExcelImportService.ImportResult doInBackground() throws Exception {
                return excelImportService.importEmployees(file, importType);
            }

            protected void done() {
                setImportButtonEnabled(true);
                try {
                    ExcelImportService.ImportResult result = get();
                    showImportResult(result);
                    if (result.importedCount() > 0) {
                        reloadHomeData();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Excel import stopped", "Import was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause == null ? exception.getMessage() : cause.getMessage();
                    if (cause instanceof ExcelImportService.HeaderImportException) {
                        showHeaderImportError(message);
                        return;
                    }
                    DialogHelper.error(
                            HomeView.this,
                            "Excel import needs attention",
                            friendlyImportFailureMessage(message)
                    );
                }
            }
        };
        worker.execute();
    }

    private void reloadHomeData() {
        if (tablePanel != null) {
            tablePanel.reload();
        }
        if (statsPanel != null) {
            statsPanel.reload();
        }
    }

    private void downloadSampleExcel() {
        File selectedFile = FileUploadCard.chooseSaveFile(
                this,
                "Save Employee Import Sample",
                "employee_import_sample.xlsx",
                FileUploadCard.xlsxWorkbook()
        );
        if (selectedFile == null) {
            return;
        }

        File target = xlsxFile(selectedFile);
        try {
            ExcelSampleGenerator.writeSampleWorkbook(target);
            showDownloadedFileSuccess(
                    target,
                    "Sample Excel Ready",
                    "Sample Excel file saved:\n" + target.getAbsolutePath()
            );
        } catch (IOException exception) {
            DialogHelper.error(this, "Sample not saved", friendlySampleSaveFailure(target, exception));
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Sample not saved", exception.getMessage());
        }
    }

    private void exportEmployeeExcel() {
        File selectedFile = FileUploadCard.chooseSaveFile(
                this,
                "Save Employee Excel Export",
                "employee_export.xlsx",
                FileUploadCard.excelWorkbooks()
        );
        if (selectedFile == null) {
            return;
        }

        // Ask user for file format
        String selectedFormat = askForExcelFormat();
        if (selectedFormat == null) {
            return; // User cancelled
        }

        File target = ensureExcelExtension(selectedFile, selectedFormat);
        setExcelButtonBusy("Exporting...");
        SwingWorker<ExcelExportService.ExportResult, Void> worker = new SwingWorker<>() {
            protected ExcelExportService.ExportResult doInBackground() throws Exception {
                return excelExportService.exportEmployees(target);
            }

            protected void done() {
                setExcelButtonReady();
                try {
                    ExcelExportService.ExportResult result = get();
                    showDownloadedFileSuccess(
                            target,
                            "Employee Export Ready",
                            "Employee Excel export saved:\n" + target.getAbsolutePath()
                                    + "\n\nEmployees: " + result.employeeCount()
                                    + "\nColumns: " + result.columnCount()
                                    + "\nDynamic columns highlighted: " + result.dynamicColumnCount()
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Excel export stopped", "Export was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String detail = cause == null ? exception.getMessage() : cause.getMessage();
                    DialogHelper.error(
                            HomeView.this,
                            "Export not saved",
                            friendlyExportSaveFailure(target, detail)
                    );
                }
            }
        };
        worker.execute();
    }

    private void showUnsupportedImportFileDialog() {
        DialogHelper.error(
                this,
                "Unsupported import file",
                "Only Excel workbooks can be imported.\nChoose a .xlsx or .xls file and try again."
        );
    }

    private void showDownloadedFileSuccess(File file, String title, String message) {
        int selected = DialogHelper.successOption(
                this,
                title,
                message,
                "Open File",
                "OK"
        );
        if (selected == 0) {
            openDownloadedFile(file);
        }
    }

    private void openDownloadedFile(File file) {
        if (file == null || !file.isFile()) {
            DialogHelper.warning(this, "File Not Found", "The downloaded file could not be found.");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            DialogHelper.warning(this, "Open File Unavailable", "This computer does not support opening files from the app.");
            return;
        }

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException exception) {
            DialogHelper.error(this, "Open File Failed", "Could not open file:\n" + file.getAbsolutePath());
        }
    }

    private void showHeaderImportError(String details) {
        String message = "Header issue\nThis file does not match the employee import format.\n"
                + "Download the sample file, keep the first row unchanged, and paste employee data below it."
                + (details == null || details.isBlank() ? "" : "\n\nDetails: " + details);
        int selected = DialogHelper.option(
                this,
                "Excel header needs attention",
                message,
                "Download Sample",
                "Close"
        );
        if (selected == 0) {
            downloadSampleExcel();
        }
    }

    private void showImportResult(ExcelImportService.ImportResult result) {
        if (result.skippedRows().isEmpty()) {
            DialogHelper.success(this, importSummaryMessage(result, "Import complete"));
        } else {
            DialogHelper.warningSections(
                    this,
                    "Import completed with rows to review",
                    importSummaryMessage(result, "Import result"),
                    skippedRowsMessage(result)
            );
        }
    }

    private String importSummaryMessage(ExcelImportService.ImportResult result, String heading) {
        return heading
                + "\nImported: " + result.importedCount() + " employee" + plural(result.importedCount())
                + "\nNeeds review: " + result.skippedRows().size() + " row" + plural(result.skippedRows().size())
                + (result.skippedRows().isEmpty()
                ? "\nAll readable rows were imported successfully."
                : "");
    }

    private String skippedRowsMessage(ExcelImportService.ImportResult result) {
        StringBuilder message = new StringBuilder("Rows to review\n");
        for (String skippedRow : result.skippedRows()) {
            message.append(friendlySkippedRowMessage(skippedRow)).append("\n");
        }
        return message.toString().trim();
    }

    private String friendlyImportFailureMessage(String message) {
        String detail = message == null || message.isBlank() ? "The file could not be imported." : message.trim();
        if (detail.toLowerCase().contains("open or locked by another process")) {
            return "Excel file is open\nClose the file in Excel, then try the import again.";
        }
        if (detail.toLowerCase().contains("no employee rows found")) {
            return "No employee rows found\nAdd employee records below the header row, then import again.";
        }
        return "The file could not be imported\nMake sure it is a valid Excel workbook and try again.\n\nDetails: "
                + detail;
    }

    private String friendlySkippedRowMessage(String rowMessage) {
        String rowPrefix = rowPrefix(rowMessage);
        String reason = rowReason(rowMessage);
        if (reason.startsWith("Missing required fields:")) {
            return rowPrefix + "Add required fields: "
                    + reason.substring("Missing required fields:".length()).trim();
        }
        if (reason.startsWith("CNIC must use format")) {
            return rowPrefix + reason;
        }
        if (reason.equals("Date of Resignation must be after Date of Joining.")) {
            return rowPrefix + "Date of Joining must be before Date of Resignation.";
        }
        if (reason.equals("Employee ID already exists.")) {
            return rowPrefix + "Employee ID already exists.";
        }
        return rowPrefix + reason;
    }

    private String rowPrefix(String rowMessage) {
        int separator = rowMessage == null ? -1 : rowMessage.indexOf(':');
        if (separator < 0) {
            return "";
        }
        return rowMessage.substring(0, separator).trim() + ": ";
    }

    private String rowReason(String rowMessage) {
        int separator = rowMessage == null ? -1 : rowMessage.indexOf(':');
        if (separator < 0) {
            return rowMessage == null ? "" : rowMessage.trim();
        }
        return rowMessage.substring(separator + 1).trim();
    }

    private String friendlySampleSaveFailure(File target, IOException exception) {
        String detail = exception.getMessage() == null ? "" : exception.getMessage();
        if (detail.toLowerCase().contains("being used by another process")
                || detail.toLowerCase().contains("process cannot access")
                || detail.toLowerCase().contains("denied")) {
            return "The sample file could not be replaced because it is open or locked.\nClose the existing file, then save the sample again.";
        }
        return "The sample file could not be saved:\n" + target.getAbsolutePath() + "\n\nDetails: " + detail;
    }

    private String friendlyExportSaveFailure(File target, String detail) {
        String cleanDetail = detail == null ? "" : detail.trim();
        if (cleanDetail.toLowerCase().contains("open or locked")
                || cleanDetail.toLowerCase().contains("being used by another process")
                || cleanDetail.toLowerCase().contains("process cannot access")
                || cleanDetail.toLowerCase().contains("denied")) {
            return "The export file could not be replaced because it is open or locked.\nClose the existing file, then export again.";
        }
        return "The export file could not be saved:\n" + target.getAbsolutePath()
                + (cleanDetail.isBlank() ? "" : "\n\nDetails: " + cleanDetail);
    }

    private File xlsxFile(File file) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(".xlsx") ? file : new File(path + ".xlsx");
    }

    private String askForExcelFormat() {
        String[] options = {"XLSX (.xlsx)", "XLS (.xls)"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose the Excel file format:",
                "Excel Format",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        
        if (choice == 0) {
            return "xlsx";
        } else if (choice == 1) {
            return "xls";
        }
        return null; // User cancelled
    }

    private File ensureExcelExtension(File file, String format) {
        String path = file.getAbsolutePath().toLowerCase();
        String extension = "." + format;
        
        // Remove any existing Excel extension if present
        if (path.endsWith(".xlsx")) {
            path = path.substring(0, path.length() - 5);
        } else if (path.endsWith(".xls")) {
            path = path.substring(0, path.length() - 4);
        }
        
        // Add the correct extension
        return new File(path + extension);
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
    }

    private void setImportButtonEnabled(boolean enabled) {
        if (enabled) {
            setExcelButtonReady();
        } else {
            setExcelButtonBusy("Importing...");
        }
    }

    private void setExcelButtonBusy(String text) {
        if (excelBtn == null) {
            return;
        }
        excelBtn.setEnabled(false);
        excelBtn.setText(text);
        excelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void setExcelButtonReady() {
        if (excelBtn == null) {
            return;
        }
        excelBtn.setEnabled(true);
        excelBtn.setText("Import Excel");
        excelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
