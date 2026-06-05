package com.kgm.ui;

import com.kgm.config.AppConfig;
import com.kgm.dao.EmployeeRecordDao;
import com.kgm.database.DatabaseInitializer;
import com.kgm.model.Employee;
import com.kgm.service.BulkFolderDocumentImportService;
import com.kgm.service.ExcelExportService;
import com.kgm.service.ExcelImportService;
import com.kgm.service.ExcelSampleGenerator;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.dialog.UniversalDialog;
import com.kgm.ui.panel.ChartsPanel;
import com.kgm.ui.panel.EmployeeTablePanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.KPIRowsPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.ui.styling.UniversalDialogHelper;

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
    private JMenuItem excelBtn;
    private JMenuItem bulkDocumentBtn;
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
                HomeViewHelper.styleActiveFilterButton(refreshBtn);
            }
        });

        JPanel top = HomeViewHelper.createTopPanel();
        top.add(new HeaderPanel("Dashboard"), BorderLayout.NORTH);

        JPanel commandBar = HomeViewHelper.createCommandBar();
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

        HomeViewHelper.addSearchControls(commandBar, searchField, searchBtn, clearBtn);

        excelBtn = HomeViewHelper.createServicesMenuItem("Excel Services");
        excelBtn.addActionListener(e -> showExcelImportActions());

        bulkDocumentBtn = HomeViewHelper.createServicesMenuItem("Bulk Documents");
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

        JMenuItem settingsBtn = HomeViewHelper.createServicesMenuItem("Settings");
        settingsBtn.addActionListener(e -> {
            new FieldManagementView();
            dispose();
        });

        JButton servicesBtn = HomeViewHelper.createServicesMenuButton();
        JPopupMenu servicesMenu = HomeViewHelper.createServicesMenu(excelBtn, bulkDocumentBtn, settingsBtn);
        servicesBtn.addActionListener(e -> {
            Dimension menuSize = servicesMenu.getPreferredSize();
            int menuX = Math.max(0, servicesBtn.getWidth() - menuSize.width);
            servicesMenu.show(servicesBtn, menuX, servicesBtn.getHeight() + 4);
        });
        HomeViewHelper.addCommandActions(commandBar, addBtn, refreshBtn, servicesBtn);

        // Build layout: Header > KPIs > Command Bar > Table > Charts
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
        
        mainContent.add(commandBar);
        
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
                return FileUploadCard.chooseDirectories(
                        HomeView.this,
                        "Select up to " + BulkFolderDocumentImportService.MAX_FOLDERS + " employee folders"
                );
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
        final ServiceTimeoutGuard[] timeoutGuard = new ServiceTimeoutGuard[1];
        SwingWorker<BulkFolderDocumentImportService.ImportResult, Void> worker = new SwingWorker<>() {
            protected BulkFolderDocumentImportService.ImportResult doInBackground() throws Exception {
                return bulkFolderDocumentImportService.importFolders(folders, (message, completedFolders, totalFolders, percent) ->
                        updateExcelLoader(loader, message, percent));
            }

            protected void done() {
                finishTimeoutGuard(timeoutGuard);
                loader.close();
                bulkDocumentActionRunning = false;
                setBulkDocumentButtonReady();
                setExcelButtonReady();
                if (timedOut(timeoutGuard)) {
                    return;
                }
                try {
                    BulkFolderDocumentImportService.ImportResult result = get();
                    showBulkDocumentImportResult(result);
                    if (result.uploadedCount() > 0) {
                        reloadHomeData();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Bulk upload stopped", "Document folder upload was interrupted.");
                } catch (CancellationException exception) {
                    DialogHelper.error(HomeView.this, "Bulk upload stopped", "Document folder upload was cancelled.");
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
        timeoutGuard[0] = startServiceTimeout(
                worker,
                loader,
                "Bulk upload timeout",
                "Document folder upload ran for more than " + longServiceTimeoutLabel() + " and was stopped.\nTry fewer folders, check file sizes, and start again.",
                () -> {
                    bulkDocumentActionRunning = false;
                    setBulkDocumentButtonReady();
                    setExcelButtonReady();
                }
        );
        worker.execute();
    }

    private void showBulkDocumentImportResult(BulkFolderDocumentImportService.ImportResult result) {
        JDialog dialog = new JDialog(this, "Bulk Document Upload Finished", Dialog.ModalityType.APPLICATION_MODAL);
        UniversalDialogHelper.styleDialogWindow(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.setContentPane(createBulkDocumentResultContent(dialog, result));
        UniversalDialogHelper.resizeLargeDialog(dialog, this);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createBulkDocumentResultContent(JDialog dialog, BulkFolderDocumentImportService.ImportResult result) {
        UniversalDialog.Type resultType = bulkResultType(result);
        Color accent = UniversalDialogHelper.accentFor(resultType);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UniversalDialogHelper.BACKGROUND);
        UniversalDialogHelper.styleRoot(root);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(UniversalDialogHelper.BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(24, 24, 8, 20));

        JPanel headerText = UniversalDialogHelper.createDialogTextStack();
        JLabel title = new JLabel(bulkResultTitle(result));
        title.setFont(UniversalDialogHelper.mediumFont(18));
        title.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(UniversalDialogHelper.htmlWrap(bulkResultSubtitle(result)));
        subtitle.setFont(UniversalDialogHelper.regularFont(13));
        subtitle.setForeground(UniversalDialogHelper.MUTED_TEXT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitle);
        header.add(headerText, BorderLayout.CENTER);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(UniversalDialogHelper.createPill(
                result.skippedCount() == 0 ? "Complete" : "Needs review",
                UniversalDialogHelper.surface(resultType),
                accent
        ));
        JButton topClose = UniversalDialogHelper.closeButton();
        topClose.addActionListener(event -> dialog.dispose());
        headerActions.add(topClose);
        header.add(headerActions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UniversalDialogHelper.BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(14, 24, 18, 24));

        body.add(summaryCard(result));
        if (result.uploadedCount() > 0) {
            body.add(Box.createVerticalStrut(10));
            body.add(successfulUploadsCard(result));
        }
        java.util.List<BulkFolderDocumentImportService.EmployeeUploadSummary> reviewEmployees =
                employeesWithReviewItems(result.employeeSummaries());
        if (!reviewEmployees.isEmpty()) {
            body.add(Box.createVerticalStrut(10));
            body.add(employeeReviewCard(reviewEmployees));
        }
        if (!result.folderErrors().isEmpty()) {
            body.add(Box.createVerticalStrut(10));
            body.add(folderErrorsCard(result.folderErrors()));
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setBlockIncrement(120);
        UniversalDialogHelper.styleDialogScrollPane(scroll);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 8));
        footer.setBackground(UniversalDialogHelper.BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        JLabel footerHint = new JLabel(UniversalDialogHelper.htmlWrap(
                result.skippedCount() == 0
                        ? "All matched documents are now saved in their employee records."
                        : "Open any folder link below to review files that still need attention."
        ));
        footerHint.setFont(UniversalDialogHelper.regularFont(12));
        footerHint.setForeground(UniversalDialogHelper.MUTED_TEXT);
        footer.add(footerHint, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        JButton close = UniversalDialogHelper.primaryButton("Done", UniversalDialogHelper.PRIMARY);
        close.addActionListener(event -> dialog.dispose());
        buttonRow.add(close);
        footer.add(buttonRow, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        return root;
    }

    private UniversalDialog.Type bulkResultType(BulkFolderDocumentImportService.ImportResult result) {
        if (result.skippedCount() > 0 || result.uploadedCount() == 0) {
            return UniversalDialog.Type.WARNING;
        }
        return UniversalDialog.Type.SUCCESS;
    }

    private String bulkResultTitle(BulkFolderDocumentImportService.ImportResult result) {
        if (result.skippedCount() > 0) {
            return "Review bulk document upload";
        }
        if (result.uploadedCount() == 0) {
            return "No documents were uploaded";
        }
        return "Bulk documents uploaded";
    }

    private String bulkResultSubtitle(BulkFolderDocumentImportService.ImportResult result) {
        if (result.skippedCount() > 0) {
            return "Some folders or files need attention. The upload results are grouped below.";
        }
        if (result.uploadedCount() == 0) {
            return "No matching documents were found in the selected folders.";
        }
        return "Matched documents were saved to the correct employee records.";
    }

    private JPanel summaryCard(BulkFolderDocumentImportService.ImportResult result) {
        UniversalDialog.Type type = bulkResultType(result);
        JPanel card = resultCard(UniversalDialogHelper.surface(type), UniversalDialogHelper.border(type));
        JLabel heading = sectionHeading("Summary");
        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(wrappedText(bulkSummaryMessage(result), UniversalDialogHelper.surface(type)));
        return card;
    }

    private String bulkSummaryMessage(BulkFolderDocumentImportService.ImportResult result) {
        String status;
        if (result.uploadedCount() == 0) {
            status = "No documents were uploaded.";
        } else if (result.skippedCount() == 0) {
            status = "All matched documents were uploaded successfully.";
        } else {
            status = "Upload finished with items to review.";
        }
        return status
                + "\nUploaded: " + result.uploadedCount() + " document" + plural(result.uploadedCount())
                + "\nNeeds review: " + result.skippedCount() + " item" + plural(result.skippedCount())
                + "\nOnly files directly inside each selected employee folder were checked.";
    }

    private JPanel successfulUploadsCard(BulkFolderDocumentImportService.ImportResult result) {
        JPanel card = resultCard(
                UniversalDialogHelper.surface(UniversalDialog.Type.SUCCESS),
                UniversalDialogHelper.border(UniversalDialog.Type.SUCCESS)
        );
        int employeeCount = result.uploadedEmployees().size();
        card.add(sectionHeading("Uploaded documents"));
        card.add(Box.createVerticalStrut(4));
        card.add(wrappedText(
                result.uploadedCount() + " document" + plural(result.uploadedCount())
                        + " uploaded for " + employeeCount + " employee" + plural(employeeCount) + ".",
                UniversalDialogHelper.surface(UniversalDialog.Type.SUCCESS)
        ));
        return card;
    }

    private java.util.List<BulkFolderDocumentImportService.EmployeeUploadSummary> employeesWithReviewItems(
            java.util.List<BulkFolderDocumentImportService.EmployeeUploadSummary> employees
    ) {
        java.util.List<BulkFolderDocumentImportService.EmployeeUploadSummary> reviewEmployees = new java.util.ArrayList<>();
        for (BulkFolderDocumentImportService.EmployeeUploadSummary employee : employees) {
            if (employee.skippedCount() > 0) {
                reviewEmployees.add(employee);
            }
        }
        return reviewEmployees;
    }

    private JPanel employeeReviewCard(java.util.List<BulkFolderDocumentImportService.EmployeeUploadSummary> employees) {
        Color background = UniversalDialogHelper.surface(UniversalDialog.Type.WARNING);
        JPanel card = resultCard(background, UniversalDialogHelper.border(UniversalDialog.Type.WARNING));
        card.add(sectionHeading("Needs attention"));
        for (BulkFolderDocumentImportService.EmployeeUploadSummary employee : employees) {
            card.add(Box.createVerticalStrut(10));
            card.add(folderLink("Employee folder: " + employee.displayName(), employee.folder()));
            addUploadedCountParagraph(card, employee.uploadedLabels().size(), background);
            addSummaryParagraph(
                    card,
                    "Already saved, left unchanged",
                    employee.alreadyExistingLabels(),
                    background
            );
            addSummaryParagraph(
                    card,
                    "No matching document label",
                    employee.noMatchFiles(),
                    background
            );
            for (Map.Entry<String, java.util.List<String>> failure : employee.failedByReason().entrySet()) {
                addSummaryParagraph(card, failure.getKey(), failure.getValue(), background);
            }
        }
        return card;
    }

    private void addUploadedCountParagraph(JPanel card, int uploadedCount, Color background) {
        if (uploadedCount <= 0) {
            return;
        }
        card.add(wrappedText(
                "Uploaded successfully: " + uploadedCount + " document" + plural(uploadedCount),
                background
        ));
    }

    private void addSummaryParagraph(JPanel card, String label, java.util.List<String> values, Color background) {
        if (values == null || values.isEmpty()) {
            return;
        }
        card.add(wrappedText(label + ": " + String.join(", ", values), background));
    }

    private JPanel folderErrorsCard(java.util.List<BulkFolderDocumentImportService.FolderError> errors) {
        Color background = UniversalDialogHelper.surface(UniversalDialog.Type.ERROR);
        JPanel card = resultCard(background, UniversalDialogHelper.border(UniversalDialog.Type.ERROR));
        card.add(sectionHeading("Folder issues"));
        for (BulkFolderDocumentImportService.FolderError error : errors) {
            card.add(Box.createVerticalStrut(10));
            card.add(folderLink("Folder: " + error.folderName(), error.folder()));
            card.add(wrappedText(String.join("\n", error.messages()), background));
        }
        return card;
    }

    private JPanel resultCard(Color background, Color border) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(background);
        card.setBorder(BorderFactory.createCompoundBorder(
                UniversalDialogHelper.roundedBorder(border, 8, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UniversalDialogHelper.mediumFont(14));
        label.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea wrappedText(String text, Color background) {
        JTextArea area = new JTextArea(text == null || text.isBlank() ? "-" : text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UniversalDialogHelper.regularFont(13));
        area.setForeground(UniversalDialogHelper.TEXT_SECONDARY);
        area.setBackground(background);
        area.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return area;
    }

    private JLabel folderLink(String folderName, File folder) {
        JLabel label = new JLabel(folderName == null || folderName.isBlank() ? "Open folder" : folderName);
        Color normal = UniversalDialogHelper.TEXT_PRIMARY;
        Color active = UniversalDialogHelper.PRIMARY;
        label.setFont(UniversalDialogHelper.mediumFont(13));
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
        final ServiceTimeoutGuard[] timeoutGuard = new ServiceTimeoutGuard[1];
        SwingWorker<ExcelImportService.ImportResult, Void> worker = new SwingWorker<>() {
            protected ExcelImportService.ImportResult doInBackground() throws Exception {
                return excelImportService.importEmployees(file, importType, (message, completedRows, totalRows, percent) ->
                        updateExcelLoader(loader, message, percent));
            }

            protected void done() {
                finishTimeoutGuard(timeoutGuard);
                loader.close();
                setImportButtonEnabled(true);
                if (timedOut(timeoutGuard)) {
                    return;
                }
                try {
                    ExcelImportService.ImportResult result = get();
                    showImportResult(result);
                    if (result.importedCount() > 0) {
                        reloadHomeData();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.error(HomeView.this, "Excel import stopped", "Import was interrupted.");
                } catch (CancellationException exception) {
                    DialogHelper.error(HomeView.this, "Excel import stopped", "Import was cancelled.");
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
        timeoutGuard[0] = startServiceTimeout(
                worker,
                loader,
                "Excel import timeout",
                "Excel import ran for more than " + longServiceTimeoutLabel() + " and was stopped.\nCheck the workbook size, close any locked files, and try again.",
                () -> setImportButtonEnabled(true)
        );
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
            HomeViewHelper.styleRefreshButton(refreshBtn);
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
            final ServiceTimeoutGuard[] timeoutGuard = new ServiceTimeoutGuard[1];
            SwingWorker<ExcelExportService.ExportResult, Void> worker = new SwingWorker<>() {
                protected ExcelExportService.ExportResult doInBackground() throws Exception {
                    return excelExportService.exportEmployees(target, (message, completedRows, totalRows, percent) ->
                            updateExcelLoader(loader, message, percent));
                }

                protected void done() {
                    finishTimeoutGuard(timeoutGuard);
                    loader.close();
                    setExcelButtonReady();
                    if (timedOut(timeoutGuard)) {
                        return;
                    }
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
                    } catch (CancellationException exception) {
                        DialogHelper.error(HomeView.this, "Excel export stopped", "Export was cancelled.");
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
            timeoutGuard[0] = startServiceTimeout(
                    worker,
                    loader,
                    "Excel export timeout",
                    "Excel export ran for more than " + longServiceTimeoutLabel() + " and was stopped.\nClose any open export file, then try again.",
                    this::setExcelButtonReady
            );
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

    private ServiceTimeoutGuard startServiceTimeout(
            SwingWorker<?, ?> worker,
            LoadingOverlay.Handle loader,
            String title,
            String message,
            Runnable resetUi
    ) {
        ServiceTimeoutGuard guard = new ServiceTimeoutGuard();
        Timer timer = new Timer(longServiceTimeoutMs(), event -> {
            if (worker.isDone()) {
                return;
            }
            guard.markTimedOut();
            worker.cancel(true);
            loader.close();
            if (resetUi != null) {
                resetUi.run();
            }
            DialogHelper.error(HomeView.this, title, message);
        });
        timer.setRepeats(false);
        guard.setTimer(timer);
        timer.start();
        return guard;
    }

    private int longServiceTimeoutMs() {
        long milliseconds = AppConfig.longServiceTimeoutMinutes() * 60_000L;
        return milliseconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) milliseconds;
    }

    private String longServiceTimeoutLabel() {
        int minutes = AppConfig.longServiceTimeoutMinutes();
        return minutes + " minute" + plural(minutes);
    }

    private void finishTimeoutGuard(ServiceTimeoutGuard[] timeoutGuard) {
        if (timeoutGuard != null && timeoutGuard.length > 0 && timeoutGuard[0] != null) {
            timeoutGuard[0].stop();
        }
    }

    private boolean timedOut(ServiceTimeoutGuard[] timeoutGuard) {
        return timeoutGuard != null
                && timeoutGuard.length > 0
                && timeoutGuard[0] != null
                && timeoutGuard[0].timedOut();
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
        HomeViewHelper.styleServicesMenuItem(excelBtn);
    }

    private void setExcelButtonReady() {
        if (excelBtn == null) {
            return;
        }
        excelBtn.setEnabled(true);
        excelBtn.setText("Excel Services");
        excelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        HomeViewHelper.styleServicesMenuItem(excelBtn);
    }

    private void setBulkDocumentButtonOpening() {
        if (bulkDocumentBtn == null) {
            return;
        }
        bulkDocumentBtn.setText("Opening...");
        HomeViewHelper.styleServicesMenuItem(bulkDocumentBtn);
    }

    private void setBulkDocumentButtonBusy() {
        if (bulkDocumentBtn == null) {
            return;
        }
        bulkDocumentBtn.setText("Uploading...");
        HomeViewHelper.styleServicesMenuItem(bulkDocumentBtn);
    }

    private void setBulkDocumentButtonReady() {
        if (bulkDocumentBtn == null) {
            return;
        }
        bulkDocumentBtn.setText("Bulk Documents");
        HomeViewHelper.styleServicesMenuItem(bulkDocumentBtn);
    }

    private record HomeTableData(
            java.util.List<Employee> employees,
            java.util.List<Object[]> rows
    ) {
    }

    private static final class ServiceTimeoutGuard {
        private Timer timer;
        private boolean timedOut;

        private void setTimer(Timer timer) {
            this.timer = timer;
        }

        private void markTimedOut() {
            timedOut = true;
        }

        private boolean timedOut() {
            return timedOut;
        }

        private void stop() {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}
