package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.model.Employee;
import com.kgm.service.BulkFolderDocumentImportService;
import com.kgm.service.ExcelExportService;
import com.kgm.service.ExcelImportService;
import com.kgm.service.ExcelSampleGenerator;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.panel.ChartsPanel;
import com.kgm.ui.panel.EmployeeTablePanel;
import com.kgm.ui.panel.ExcelImportButton;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.KPIRowsPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class HomeView extends JFrame {
    private final BulkFolderDocumentImportService bulkFolderDocumentImportService = new BulkFolderDocumentImportService();
    private final ExcelImportService excelImportService = new ExcelImportService();
    private final ExcelExportService excelExportService = new ExcelExportService();
    private EmployeeTablePanel tablePanel;
    private KPIRowsPanel kpiPanel;
    private ChartsPanel chartsPanel;
    private JScrollPane mainScrollPane;
    private ExcelImportButton excelBtn;
    private JButton bulkDocumentBtn;
    private JButton refreshBtn;
    private boolean bulkDocumentActionRunning;
    private boolean homeDataLoading;
    private boolean dashboardStatsLoading;
    private int homeLoadToken;
    private int dashboardStatsLoadToken;
    private SwingWorker<HomeTableData, String> homeDataWorker;
    private SwingWorker<EmployeeRecordDao.DashboardStats, Void> dashboardStatsWorker;

    public HomeView() {
        HomeViewHelper.applyFrame(this);

        tablePanel = new EmployeeTablePanel(null);
        kpiPanel = new KPIRowsPanel(null);
        chartsPanel = new ChartsPanel(null);

        // Wire "Show in Table" from chart cards to the table filtering
        chartsPanel.setShowInTableHandler(this::handleShowInTable);

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

            searchEmployeeAsync(empCode);
        });

        clearBtn.addActionListener(e -> {
            searchField.setText("");
            if (tablePanel != null) {
                tablePanel.clearFilter();
            }
            HomeViewHelper.setTextButtonEnabled(clearBtn, false);
        });

        HomeViewHelper.addSearchControls(searchRow, searchField, searchBtn, clearBtn);

        JPanel btnRow = HomeViewHelper.createButtonRow();
        excelBtn = new ExcelImportButton(this::showExcelImportActions);

        bulkDocumentBtn = new JButton("Bulk Documents");
        HomeViewHelper.styleBulkDocumentButton(bulkDocumentBtn);
        bulkDocumentBtn.addActionListener(e -> chooseBulkDocumentFolders());

        JButton addBtn = new JButton("Add Employee");
        HomeViewHelper.styleAddButton(addBtn);
        addBtn.addActionListener(e -> {
            new EmployeeRegistrationView();
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
        btnRow.add(bulkDocumentBtn);
        btnRow.add(addBtn);
        btnRow.add(settingsBtn);
        btnRow.add(refreshBtn);

        // Build layout: Header > KPIs > Search > Buttons > Table > Charts
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        
        mainContent.add(top);
        
        // Wrap KPI panel with left/right margins (28px) to match filter
        JPanel kpiWrapper = new JPanel(new BorderLayout());
        kpiWrapper.setOpaque(false);
        kpiWrapper.setBorder(BorderFactory.createEmptyBorder(24, 28, 0, 28));
        kpiWrapper.add(kpiPanel, BorderLayout.CENTER);
        mainContent.add(kpiWrapper);
        
        mainContent.add(searchRow);
        mainContent.add(btnRow);
        
        // Wrap table panel with left/right margins (28px) to match filter
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(20, 28, 0, 28));
        tableWrapper.add(tablePanel, BorderLayout.CENTER);
        mainContent.add(tableWrapper);
        
        // Wrap charts panel with left/right margins (28px) to match filter
        JPanel chartsWrapper = new JPanel(new BorderLayout());
        chartsWrapper.setOpaque(false);
        chartsWrapper.setBorder(BorderFactory.createEmptyBorder(20, 28, 0, 28));
        chartsWrapper.add(chartsPanel, BorderLayout.CENTER);
        mainContent.add(chartsWrapper);

        mainScrollPane = HomeViewHelper.createMainScrollPane(mainContent);
        add(mainScrollPane, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        setVisible(true);
        SwingUtilities.invokeLater(() -> loadHomeDataAsync(
                "Loading Dashboard",
                "Preparing employee table and analytics..."
        ));
    }

    /**
     * Handles "Show in Table" clicks from chart cards.
     * Format: columnName::value::displayLabel
     */
    private void handleShowInTable(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String[] parts = command.split("::");
        if (parts.length < 2) {
            return;
        }
        if ("MULTI".equalsIgnoreCase(parts[0])) {
            handleMultiColumnShowInTable(parts);
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
        scrollToEmployeeTable();
    }

    private void handleMultiColumnShowInTable(String[] parts) {
        if (parts.length < 5) {
            return;
        }
        String displayLabel = parts[1];
        Map<String, String> criteria = new LinkedHashMap<>();
        for (int index = 2; index + 1 < parts.length; index += 2) {
            String column = parts[index];
            String value = parts[index + 1];
            if (column == null || column.isBlank() || value == null || value.isBlank()) {
                continue;
            }
            criteria.put(column, value);
        }
        if (!criteria.isEmpty()) {
            tablePanel.filterByColumns(criteria, displayLabel);
            scrollToEmployeeTable();
        }
    }

    private void scrollToEmployeeTable() {
        SwingUtilities.invokeLater(() -> {
            if (mainScrollPane == null || tablePanel == null) {
                return;
            }
            Component view = mainScrollPane.getViewport().getView();
            if (!(view instanceof JComponent viewComponent)) {
                tablePanel.scrollRectToVisible(new Rectangle(0, 0, tablePanel.getWidth(), tablePanel.getHeight()));
                return;
            }

            Rectangle tableBounds = SwingUtilities.convertRectangle(
                    tablePanel.getParent(),
                    tablePanel.getBounds(),
                    viewComponent
            );
            tableBounds.y = Math.max(0, tableBounds.y - 12);
            tableBounds.height = Math.min(Math.max(1, tablePanel.getHeight()), mainScrollPane.getViewport().getHeight());
            viewComponent.scrollRectToVisible(tableBounds);
        });
    }

    private void searchEmployeeAsync(String employeeCode) {
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Searching Employee",
                "Checking employee code..."
        );
        SwingWorker<Employee, Void> worker = new SwingWorker<>() {
            @Override
            protected Employee doInBackground() {
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    return dao.getEmployeeByCode(employeeCode);
                }
            }

            @Override
            protected void done() {
                loader.close();
                try {
                    Employee emp = get();
                    if (emp != null) {
                        tablePanel.showSingleEmployee(emp);
                    } else {
                        DialogHelper.info(HomeView.this, "No Result", "No employee found.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Search stopped", "Search was interrupted.");
                } catch (ExecutionException exception) {
                    DialogHelper.error(HomeView.this, "Search failed", "Employee search could not be completed.");
                }
            }
        };
        worker.execute();
    }

    private void showExcelImportActions() {
        try {
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
        } catch (RuntimeException exception) {
            showExcelServiceError(
                    "Excel Services",
                    "Excel actions could not be opened.",
                    exception
            );
        }
    }

    private void chooseBulkDocumentFolders() {
        if (bulkDocumentActionRunning) {
            return;
        }
        bulkDocumentActionRunning = true;
        setBulkDocumentButtonOpening();
        SwingWorker<File[], Void> pickerWorker = new SwingWorker<>() {
            @Override
            protected File[] doInBackground() {
                return FileUploadCard.chooseDirectories(HomeView.this, "Select employee folders or files inside them");
            }

            @Override
            protected void done() {
                try {
                    File[] selectedFolders = get();
                    if (selectedFolders.length == 0) {
                        bulkDocumentActionRunning = false;
                        setBulkDocumentButtonReady();
                        return;
                    }
                    if (selectedFolders.length > BulkFolderDocumentImportService.MAX_FOLDERS) {
                        bulkDocumentActionRunning = false;
                        setBulkDocumentButtonReady();
                        DialogHelper.warning(
                                HomeView.this,
                                "Too Many Folders",
                                "Select up to " + BulkFolderDocumentImportService.MAX_FOLDERS + " employee folders at once."
                        );
                        return;
                    }
                    importBulkDocumentFolders(selectedFolders);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    bulkDocumentActionRunning = false;
                    setBulkDocumentButtonReady();
                    DialogHelper.error(HomeView.this, "Bulk upload stopped", "Folder selection was interrupted.");
                } catch (ExecutionException exception) {
                    bulkDocumentActionRunning = false;
                    setBulkDocumentButtonReady();
                    Throwable cause = exception.getCause();
                    showExcelServiceError(
                            "Bulk document upload failed",
                            "The bulk document upload could not be started.",
                            cause == null ? exception : cause
                    );
                }
            }
        };
        pickerWorker.execute();
    }

    private void importBulkDocumentFolders(File[] folders) {
        setBulkDocumentButtonBusy();
        setExcelButtonBusy("Uploading...");
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Uploading Documents",
                "Scanning employee folders..."
        );
        SwingWorker<BulkFolderDocumentImportService.ImportResult, Void> worker = new SwingWorker<>() {
            protected BulkFolderDocumentImportService.ImportResult doInBackground() throws Exception {
                return bulkFolderDocumentImportService.importFolders(folders, (message, completedFolders, totalFolders, percent) ->
                        updateExcelLoader(loader, message, percent));
            }

            protected void done() {
                loader.close();
                bulkDocumentActionRunning = false;
                setBulkDocumentButtonReady();
                setExcelButtonReady();
                try {
                    BulkFolderDocumentImportService.ImportResult result = get();
                    showBulkDocumentImportResult(result);
                    if (result.uploadedCount() > 0) {
                        reloadHomeData();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Bulk upload stopped", "Document folder upload was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    DialogHelper.error(
                            HomeView.this,
                            "Bulk upload failed",
                            "Document folder upload could not be completed.\n\n" + rootMessage(cause == null ? exception : cause)
                    );
                }
            }
        };
        worker.execute();
    }

    private void showBulkDocumentImportResult(BulkFolderDocumentImportService.ImportResult result) {
        JDialog dialog = new JDialog(this, "Bulk Document Upload Complete", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(createBulkDocumentResultContent(dialog, result));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(560, 240));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createBulkDocumentResultContent(JDialog dialog, BulkFolderDocumentImportService.ImportResult result) {
        Color accent = result.skippedCount() == 0 ? new Color(28, 137, 85) : new Color(176, 76, 19);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JLabel title = new JLabel("Bulk Document Upload Complete");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        body.add(summaryCard(result));
        if (!result.uploadedEmployees().isEmpty()) {
            body.add(Box.createVerticalStrut(10));
            body.add(uploadedEmployeesCard(result.uploadedEmployees()));
        }
        if (!result.folderErrors().isEmpty()) {
            body.add(Box.createVerticalStrut(10));
            body.add(folderErrorsCard(result.folderErrors()));
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        int preferredHeight = Math.min(560, Math.max(180, body.getPreferredSize().height + 12));
        scroll.setPreferredSize(new Dimension(640, preferredHeight));
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(new Color(247, 249, 251));
        footer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JButton close = new JButton("OK");
        close.setPreferredSize(new Dimension(92, 34));
        close.setBackground(accent);
        close.setForeground(Color.WHITE);
        close.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(event -> dialog.dispose());
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        return root;
    }

    private JPanel summaryCard(BulkFolderDocumentImportService.ImportResult result) {
        JPanel card = resultCard(new Color(248, 250, 252), new Color(220, 226, 232));
        JLabel heading = sectionHeading("Summary");
        JLabel text = new JLabel(result.uploadedCount() + " document" + plural(result.uploadedCount())
                + " uploaded, " + result.skippedCount() + " issue" + plural(result.skippedCount()) + " found.");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(new Color(35, 43, 54));
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(text);
        return card;
    }

    private JPanel uploadedEmployeesCard(java.util.List<BulkFolderDocumentImportService.UploadedEmployee> employees) {
        JPanel card = resultCard(new Color(248, 255, 250), new Color(187, 247, 208));
        card.add(sectionHeading("Updated"));
        for (BulkFolderDocumentImportService.UploadedEmployee employee : employees) {
            card.add(Box.createVerticalStrut(10));
            JLabel code = new JLabel("Employee Code - " + employee.employeeCode());
            code.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
            code.setForeground(new Color(35, 43, 54));
            code.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(code);
            card.add(wrappedText(String.join(", ", employee.labels()), new Color(248, 255, 250)));
        }
        return card;
    }

    private JPanel folderErrorsCard(java.util.List<BulkFolderDocumentImportService.FolderError> errors) {
        JPanel card = resultCard(new Color(255, 250, 245), new Color(254, 215, 170));
        card.add(sectionHeading("Error Folder"));
        for (BulkFolderDocumentImportService.FolderError error : errors) {
            card.add(Box.createVerticalStrut(10));
            card.add(folderLink("Employee Code - " + error.folderName(), error.folder()));
            card.add(wrappedText(String.join("\n", error.messages()), new Color(255, 250, 245)));
        }
        return card;
    }

    private JPanel resultCard(Color background, Color border) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(background);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(35, 43, 54));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea wrappedText(String text, Color background) {
        JTextArea area = new JTextArea(text == null || text.isBlank() ? "-" : text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setForeground(new Color(35, 43, 54));
        area.setBackground(background);
        area.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return area;
    }

    private JLabel folderLink(String folderName, File folder) {
        JLabel label = new JLabel(folderName == null || folderName.isBlank() ? "Open folder" : folderName);
        Color normal = new Color(35, 43, 54);
        Color active = new Color(0, 112, 210);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setForeground(normal);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent event) {
                label.setForeground(active);
            }

            public void mouseExited(java.awt.event.MouseEvent event) {
                label.setForeground(normal);
            }

            public void mousePressed(java.awt.event.MouseEvent event) {
                label.setForeground(active.darker());
            }

            public void mouseReleased(java.awt.event.MouseEvent event) {
                label.setForeground(active);
            }

            public void mouseClicked(java.awt.event.MouseEvent event) {
                openFolder(folder);
            }
        });
        return label;
    }

    private void openFolder(File folder) {
        if (folder == null || !folder.isDirectory()) {
            DialogHelper.warning(this, "Folder Not Found", "The selected folder could not be found.");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            DialogHelper.warning(this, "Open Folder Unavailable", "This computer does not support opening folders from the app.");
            return;
        }

        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException exception) {
            DialogHelper.error(this, "Open Folder Failed", "Could not open folder:\n" + folder.getAbsolutePath());
        }
    }

    private void chooseExcelImportFile() {
        try {
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
        } catch (RuntimeException exception) {
            showExcelServiceError(
                    "Excel import failed",
                    "The Excel import could not be started.",
                    exception
            );
        }
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
        panel.add(new JLabel("Requires CNIC and every field marked Required in Field Management."));
        panel.add(Box.createVerticalStrut(8));
        panel.add(legacyImport);
        panel.add(new JLabel("For old records. Requires Employee ID; CNIC is optional and dates use mm/dd/yyyy hh:mm:ss."));

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
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Importing Excel",
                "Reading workbook and saving employees..."
        );
        SwingWorker<ExcelImportService.ImportResult, Void> worker = new SwingWorker<>() {
            protected ExcelImportService.ImportResult doInBackground() throws Exception {
                return excelImportService.importEmployees(file, importType, (message, completedRows, totalRows, percent) ->
                        updateExcelLoader(loader, message, percent));
            }

            protected void done() {
                loader.close();
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
                } catch (RuntimeException exception) {
                    showExcelServiceError(
                            "Excel import failed",
                            "The Excel import could not be completed.",
                            exception
                    );
                }
            }
        };
        worker.execute();
    }

    private void reloadHomeData() {
        loadHomeDataAsync("Refreshing Dashboard", "Loading latest employee table and analytics...");
    }

    private void loadHomeDataAsync(String title, String message) {
        if (homeDataWorker != null && !homeDataWorker.isDone()) {
            homeDataWorker.cancel(true);
        }
        if (dashboardStatsWorker != null && !dashboardStatsWorker.isDone()) {
            dashboardStatsWorker.cancel(true);
        }
        int token = ++homeLoadToken;
        homeDataLoading = true;
        tablePanel.showLoading(message);
        chartsPanel.setStats(null);
        if (refreshBtn != null) {
            refreshBtn.setText("Loading...");
            refreshBtn.setEnabled(true);
        }

        SwingWorker<HomeTableData, String> worker = new SwingWorker<>() {
            @Override
            protected HomeTableData doInBackground() {
                publish("Preparing database and field settings...");
                DatabaseInitializer.init();
                publish("Loading employee table...");
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    java.util.List<Employee> employees = dao.getEmployeeSummaries();
                    publish("Preparing employee rows...");
                    java.util.List<Object[]> rows = EmployeeTablePanel.toRows(employees);
                    return new HomeTableData(employees, rows);
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (token == homeLoadToken && !chunks.isEmpty()) {
                    tablePanel.showLoading(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                boolean latest = token == homeLoadToken;
                try {
                    if (!latest || isCancelled()) {
                        return;
                    }
                    HomeTableData data = get();
                    if (!isDisplayable()) {
                        return;
                    }
                    tablePanel.setRepository(null);
                    tablePanel.setEmployees(data.employees(), data.rows());
                    kpiPanel.setRepository(null);
                    kpiPanel.setStats(null);
                    chartsPanel.setRepository(null);
                    chartsPanel.setStats(null);
                    loadDashboardStatsAsync();
                } catch (CancellationException exception) {
                    if (latest) {
                        tablePanel.showLoadFailed("Dashboard loading was cancelled.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    if (latest) {
                        tablePanel.showLoadFailed("Dashboard loading was interrupted.");
                    }
                    DialogHelper.error(HomeView.this, "Dashboard stopped", "Dashboard loading was interrupted.");
                } catch (ExecutionException exception) {
                    exception.printStackTrace();
                    if (latest) {
                        tablePanel.showLoadFailed("Dashboard data could not be loaded.");
                    }
                    DialogHelper.error(
                            HomeView.this,
                            "Dashboard Load Failed",
                            "Dashboard data could not be loaded.\n\n" + rootMessage(exception)
                    );
                } catch (RuntimeException exception) {
                    exception.printStackTrace();
                    if (latest) {
                        tablePanel.showLoadFailed("Dashboard display could not be prepared.");
                    }
                    DialogHelper.error(
                            HomeView.this,
                            "Dashboard Display Failed",
                            "Employee data was fetched, but the dashboard could not be prepared.\n\n"
                                    + rootMessage(exception)
                    );
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    if (latest) {
                        tablePanel.showLoadFailed("Dashboard display could not be prepared.");
                    }
                    DialogHelper.error(
                            HomeView.this,
                            "Dashboard Display Failed",
                            "Employee data was fetched, but the dashboard could not be prepared.\n\n"
                                    + rootMessage(throwable)
                    );
                } finally {
                    if (latest) {
                        homeDataLoading = false;
                        homeDataWorker = null;
                    }
                    if (latest && refreshBtn != null && isDisplayable()) {
                        refreshBtn.setEnabled(true);
                        refreshBtn.setText("Refresh");
                        HomeViewHelper.styleRefreshButton(refreshBtn);
                    }
                }
            }
        };
        homeDataWorker = worker;
        worker.execute();
    }

    private void loadDashboardStatsAsync() {
        if (dashboardStatsWorker != null && !dashboardStatsWorker.isDone()) {
            dashboardStatsWorker.cancel(true);
        }
        int token = ++dashboardStatsLoadToken;
        dashboardStatsLoading = true;

        SwingWorker<EmployeeRecordDao.DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected EmployeeRecordDao.DashboardStats doInBackground() {
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    return dao.dashboardStats();
                }
            }

            @Override
            protected void done() {
                boolean latest = token == dashboardStatsLoadToken;
                if (latest) {
                    dashboardStatsLoading = false;
                    dashboardStatsWorker = null;
                }
                if (!latest || isCancelled()) {
                    return;
                }
                if (!isDisplayable()) {
                    return;
                }
                try {
                    kpiPanel.setStats(get());
                    chartsPanel.setStats(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Dashboard analytics stopped", "Dashboard analytics loading was interrupted.");
                } catch (ExecutionException exception) {
                    // Log but don't fail completely - table is already showing
                    exception.printStackTrace();
                    DialogHelper.error(HomeView.this, "Dashboard Analytics Failed", 
                            "Dashboard charts could not be loaded, but employee data is displayed.\n\n" + 
                            (exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage()));
                } catch (RuntimeException exception) {
                    exception.printStackTrace();
                    DialogHelper.error(
                            HomeView.this,
                            "Dashboard Analytics Failed",
                            "Dashboard charts could not be displayed, but employee data is displayed.\n\n"
                                    + rootMessage(exception)
                    );
                }
            }
        };
        dashboardStatsWorker = worker;
        worker.execute();
    }

    private void downloadSampleExcel() {
        try {
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
            LoadingOverlay.Handle loader = LoadingOverlay.show(
                    this,
                    "Saving Sample",
                    "Building the latest import sample..."
            );
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ExcelSampleGenerator.writeSampleWorkbook(target);
                    return null;
                }

                @Override
                protected void done() {
                    loader.close();
                    try {
                        get();
                        showDownloadedFileSuccess(
                                target,
                                "Sample Excel Ready",
                                "Sample Excel file saved:\n" + target.getAbsolutePath()
                        );
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        DialogHelper.error(HomeView.this, "Sample not saved", "Sample save was interrupted.");
                    } catch (ExecutionException exception) {
                        Throwable cause = exception.getCause();
                        if (cause instanceof IOException ioException) {
                            DialogHelper.error(HomeView.this, "Sample not saved", friendlySampleSaveFailure(target, ioException));
                        } else {
                            showExcelServiceError(
                                    "Sample not saved",
                                    "The sample Excel file could not be saved.",
                                    cause == null ? exception : cause
                            );
                        }
                    } catch (RuntimeException exception) {
                        showExcelServiceError(
                                "Sample not saved",
                                "The sample Excel file could not be saved.",
                                exception
                        );
                    }
                }
            };
            worker.execute();
        } catch (RuntimeException exception) {
            showExcelServiceError(
                    "Sample not saved",
                    "The sample Excel file could not be started.",
                    exception
            );
        }
    }

    private void exportEmployeeExcel() {
        try {
            File selectedFile = FileUploadCard.chooseSaveFile(
                    this,
                    "Save Employee Excel Export",
                    "employee_export.xlsx",
                    FileUploadCard.excelWorkbooks()
            );
            if (selectedFile == null) {
                return;
            }

            String selectedFormat = askForExcelFormat();
            if (selectedFormat == null) {
                return;
            }

            File target = ensureExcelExtension(selectedFile, selectedFormat);
            setExcelButtonBusy("Exporting...");
            LoadingOverlay.Handle loader = LoadingOverlay.show(
                    this,
                    "Exporting Excel",
                    "Saving employee workbook..."
            );
            SwingWorker<ExcelExportService.ExportResult, Void> worker = new SwingWorker<>() {
                protected ExcelExportService.ExportResult doInBackground() throws Exception {
                    return excelExportService.exportEmployees(target, (message, completedRows, totalRows, percent) ->
                            updateExcelLoader(loader, message, percent));
                }

                protected void done() {
                    loader.close();
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
                        // Reload home data after successful export to refresh the display
                        reloadHomeData();
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
                    } catch (RuntimeException exception) {
                        showExcelServiceError(
                                "Export not saved",
                                "The employee Excel export could not be completed.",
                                exception
                        );
                    }
                }
            };
            worker.execute();
        } catch (RuntimeException exception) {
            setExcelButtonReady();
            showExcelServiceError(
                    "Export not saved",
                    "The employee Excel export could not be started.",
                    exception
            );
        }
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
        int choice = DialogHelper.option(
                this,
                "Excel Format",
                "Choose the Excel file format:",
                "XLSX (.xlsx)",
                "XLS (.xls)"
        );

        if (choice == 0) {
            return "xlsx";
        } else if (choice == 1) {
            return "xls";
        }
        return null; // User cancelled
    }

    private void showExcelServiceError(String title, String action, Throwable throwable) {
        DialogHelper.error(
                this,
                title,
                action + "\n\nDetails: " + rootMessage(throwable)
        );
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

    private void updateExcelLoader(LoadingOverlay.Handle loader, String message, int percent) {
        if (loader == null) {
            return;
        }
        loader.setMessage(message);
        loader.setProgress(percent);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank()
                ? "No additional details were provided."
                : message.trim();
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

    private void setBulkDocumentButtonOpening() {
        if (bulkDocumentBtn == null) {
            return;
        }
        bulkDocumentBtn.setText("Opening...");
        HomeViewHelper.styleBulkDocumentButton(bulkDocumentBtn);
    }

    private void setBulkDocumentButtonBusy() {
        if (bulkDocumentBtn == null) {
            return;
        }
        bulkDocumentBtn.setText("Uploading...");
        HomeViewHelper.styleBulkDocumentButton(bulkDocumentBtn);
    }

    private void setBulkDocumentButtonReady() {
        if (bulkDocumentBtn == null) {
            return;
        }
        bulkDocumentBtn.setText("Bulk Documents");
        HomeViewHelper.styleBulkDocumentButton(bulkDocumentBtn);
    }

    private record HomeTableData(
            java.util.List<Employee> employees,
            java.util.List<Object[]> rows
    ) {
    }
}
