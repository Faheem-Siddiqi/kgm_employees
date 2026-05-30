package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.panel.EmployeeBasicDetailsPanel;
import com.kgm.ui.panel.EmployeeDocumentViewPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.EmployeeAdditionalDetailsPanel;
import com.kgm.service.EmployeeReportService;
import com.kgm.ui.styling.AppTabsHelper;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeDetailViewHelper;
import com.kgm.util.EmployeeBasicFieldUtil;
import com.kgm.util.EmployeeFieldDefinitionCache;
import com.kgm.util.EmployeeStorageUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDetailView extends JFrame {
    private static final int BASIC_TAB_INDEX = 0;
    private static final int OTHER_TAB_INDEX = 1;
    private static final int DOCUMENT_TAB_INDEX = 2;

    private String empCode;
    private JButton updateBtn;
    private Employee employee;
    private List<EmployeeFieldDefinition> basicDefinitions = List.of();
    private List<EmployeeFieldDefinition> detailDefinitions = List.of();
    private JTabbedPane tabs;
    private JScrollPane pageScroll;
    private JPanel centerWrapper;
    private EmployeeBasicDetailsPanel basicPanel;
    private EmployeeAdditionalDetailsPanel otherPanel;
    private EmployeeDocumentViewPanel documentPanel;
    private boolean basicTabLoaded;
    private boolean basicTabLoading;
    private boolean otherTabLoaded;
    private boolean otherTabLoading;
    private boolean documentTabLoaded;
    private boolean documentTabLoading;

    public EmployeeDetailView(String empCode) {
        this.empCode = (empCode != null) ? empCode.trim() : null;
        showLoadingShell();
        setVisible(true);
        loadEmployeeAsync();
    }

    public EmployeeDetailView() {
        initializeUI((Employee) null, false);
    }

    public void refreshDynamicFields() {
        if (empCode == null || empCode.isBlank()) {
            initializeUI((Employee) null, false);
            return;
        }
        showLoadingShell();
        loadEmployeeAsync();
    }

    private void initializeUI(Employee emp, boolean isWithData) {
        initializeUI(new DetailLoadResult(
                emp,
                EmployeeBasicFieldUtil.loadBasicDefinitions(),
                loadDetailDefinitions()
        ), isWithData);
    }

    private void initializeUI(DetailLoadResult detailData, boolean isWithData) {
        Employee emp = detailData == null ? null : detailData.employee();
        this.employee = emp;
        this.basicDefinitions = detailData == null || detailData.basicDefinitions() == null
                ? EmployeeBasicFieldUtil.loadBasicDefinitions()
                : detailData.basicDefinitions();
        this.detailDefinitions = detailData == null || detailData.detailDefinitions() == null
                ? List.of()
                : detailData.detailDefinitions();
        resetLazyTabs();

        getContentPane().removeAll();
        EmployeeDetailViewHelper.applyFrame(this);

        JPanel topContainer = EmployeeDetailViewHelper.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);
        String nameValue = (emp != null) ? emp.getEMP_NAME() : "";
        String codeValue = (emp != null) ? emp.getEMPLOYEE_CODE() : "";
        Runnable onDownloadReport = isWithData && emp != null ? this::downloadEmployeeReport : null;
        topContainer.add(EmployeeDetailViewHelper.screenHeader(
                nameValue,
                codeValue,
                () -> {
                    new HomeView();
                    this.dispose();
                },
                onDownloadReport
        ), BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        centerWrapper = EmployeeDetailViewHelper.createCenterWrapper();
        tabs = new HugHeightTabbedPane();

        if (isWithData) {
            tabs.addTab("Basic", createTabLoadingPanel("Basic details", "Fetching only the Basic fields for this employee."));
            tabs.addTab("Others", createTabLoadingPanel("Other details", "This tab loads when you open it."));
            tabs.addTab("Documents", createTabLoadingPanel("Documents", "Document paths load only when needed."));
        } else {
            basicPanel = new EmployeeBasicDetailsPanel(null, basicDefinitions);
            otherPanel = new EmployeeAdditionalDetailsPanel(null, detailDefinitions);
            documentPanel = new EmployeeDocumentViewPanel();
            documentPanel.setProfileImageUploadListener(basicPanel::setSelectedImageFromDocumentUpload);
            basicPanel.setSelectedImageListener(documentPanel::setProfileImageFromMainTab);
            tabs.addTab("Core", basicPanel);
            tabs.addTab("Details", otherPanel);
            tabs.addTab("Documents", documentPanel);
        }

        AppTabsHelper.styleTabs(tabs);
        tabs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent event) {
                int tabIndex = tabs.indexAtLocation(event.getX(), event.getY());
                if (tabIndex >= 0 && tabs.isEnabledAt(tabIndex)) {
                    tabs.setSelectedIndex(tabIndex);
                    tabs.revalidate();
                    tabs.repaint();
                }
            }
        });

        JPanel footerActions = EmployeeDetailViewHelper.createActionRow();
        updateBtn = new JButton("Update");
        EmployeeDetailViewHelper.styleUpdateButton(updateBtn);
        updateBtn.setEnabled(false);
        footerActions.add(updateBtn);

        JPanel tabContent = EmployeeDetailViewHelper.createTabContent(tabs, null);
        centerWrapper.add(tabContent, EmployeeDetailViewHelper.pageConstraints(0));

        pageScroll = EmployeeDetailViewHelper.createPageScrollPane(centerWrapper);
        tabs.addChangeListener(event -> SwingUtilities.invokeLater(() -> {
            centerWrapper.revalidate();
            centerWrapper.repaint();
            pageScroll.getVerticalScrollBar().setValue(0);
            if (isWithData) {
                loadSelectedTab();
            }
            refreshUpdateButtonState();
        }));
        EmployeeDetailViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);
        add(pageScroll, BorderLayout.CENTER);

        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.setBackground(Color.WHITE);
        southContainer.add(footerActions, BorderLayout.NORTH);
        southContainer.add(new FooterPanel(), BorderLayout.SOUTH);
        add(southContainer, BorderLayout.SOUTH);

        if (isWithData) {
            updateBtn.addActionListener(event -> handleUpdate());
        }
        refreshUpdateButtonState();
        revalidate();
        repaint();
        setVisible(true);

        if (isWithData) {
            SwingUtilities.invokeLater(this::loadSelectedTab);
        }
    }

    private void resetLazyTabs() {
        basicPanel = null;
        otherPanel = null;
        documentPanel = null;
        basicTabLoaded = false;
        basicTabLoading = false;
        otherTabLoaded = false;
        otherTabLoading = false;
        documentTabLoaded = false;
        documentTabLoading = false;
    }

    private JPanel createTabLoadingPanel(String title, String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(42, 24, 52, 24));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        titleLabel.setForeground(new Color(17, 24, 39));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(100, 116, 139));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(titleLabel);
        box.add(Box.createVerticalStrut(5));
        box.add(messageLabel);
        panel.add(box);
        return panel;
    }

    private JPanel createTabErrorPanel(String title, String message, Runnable retryAction) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(46, 24, 56, 24));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        titleLabel.setForeground(new Color(185, 28, 28));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(100, 116, 139));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton retry = createDialogButton("Retry", true);
        retry.setAlignmentX(Component.CENTER_ALIGNMENT);
        retry.addActionListener(event -> retryAction.run());

        box.add(titleLabel);
        box.add(Box.createVerticalStrut(6));
        box.add(messageLabel);
        box.add(Box.createVerticalStrut(18));
        box.add(retry);
        panel.add(box);
        return panel;
    }

    private void loadSelectedTab() {
        if (tabs == null || empCode == null || empCode.isBlank()) {
            return;
        }

        int selectedIndex = tabs.getSelectedIndex();
        if (selectedIndex == BASIC_TAB_INDEX) {
            loadBasicTab();
        } else if (selectedIndex == OTHER_TAB_INDEX) {
            loadOtherTab();
        } else if (selectedIndex == DOCUMENT_TAB_INDEX) {
            loadDocumentTab();
        }
    }

    private void loadBasicTab() {
        if (basicTabLoaded || basicTabLoading || tabs == null) {
            return;
        }
        basicTabLoading = true;
        tabs.setComponentAt(BASIC_TAB_INDEX, createTabLoadingPanel("Basic details", "Fetching Basic fields from the database."));
        LoadingOverlay.Handle loader = LoadingOverlay.show(this, "Loading Basic Details", "Fetching only Basic tab fields...");

        SwingWorker<Employee, Void> worker = new SwingWorker<>() {
            @Override
            protected Employee doInBackground() {
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    return dao.getEmployeeSectionByCode(
                            empCode,
                            columnsForDefinitions(basicDefinitions, true)
                    );
                }
            }

            @Override
            protected void done() {
                loader.close();
                basicTabLoading = false;
                if (!isDisplayable()) {
                    return;
                }
                try {
                    Employee loaded = get();
                    if (loaded == null) {
                        throw new IllegalStateException("Employee record was not found.");
                    }
                    employee = loaded;
                    basicPanel = new EmployeeBasicDetailsPanel(loaded, basicDefinitions);
                    basicPanel.setPendingChangesListener(EmployeeDetailView.this::refreshUpdateButtonState);
                    if (documentPanel != null) {
                        documentPanel.setProfileImageUploadListener(basicPanel::setSelectedImageFromDocumentUpload);
                        basicPanel.setSelectedImageListener(documentPanel::setProfileImageFromMainTab);
                        File pendingProfileImage = documentPanel.pendingProfileImageFile();
                        if (pendingProfileImage != null) {
                            basicPanel.setSelectedImageFromDocumentUpload(pendingProfileImage);
                        }
                    }
                    tabs.setComponentAt(BASIC_TAB_INDEX, basicPanel);
                    basicTabLoaded = true;
                    refreshAfterTabChange();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    tabs.setComponentAt(BASIC_TAB_INDEX, createTabErrorPanel(
                            "Basic details could not load",
                            "Check the database connection and try again.",
                            EmployeeDetailView.this::loadBasicTab
                    ));
                    refreshAfterTabChange();
                }
            }
        };
        worker.execute();
    }

    private void loadOtherTab() {
        if (otherTabLoaded || otherTabLoading || tabs == null) {
            return;
        }
        otherTabLoading = true;
        tabs.setComponentAt(OTHER_TAB_INDEX, createTabLoadingPanel("Other details", "Fetching only additional fields."));
        LoadingOverlay.Handle loader = LoadingOverlay.show(this, "Loading Other Details", "Fetching selected tab fields...");

        SwingWorker<Employee, Void> worker = new SwingWorker<>() {
            @Override
            protected Employee doInBackground() {
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    return dao.getEmployeeSectionByCode(
                            empCode,
                            columnsForDefinitions(detailDefinitions, false)
                    );
                }
            }

            @Override
            protected void done() {
                loader.close();
                otherTabLoading = false;
                if (!isDisplayable()) {
                    return;
                }
                try {
                    Employee loaded = get();
                    if (loaded == null) {
                        throw new IllegalStateException("Employee record was not found.");
                    }
                    otherPanel = new EmployeeAdditionalDetailsPanel(loaded, detailDefinitions);
                    otherPanel.setPendingChangesListener(EmployeeDetailView.this::refreshUpdateButtonState);
                    tabs.setComponentAt(OTHER_TAB_INDEX, otherPanel);
                    otherTabLoaded = true;
                    refreshAfterTabChange();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    tabs.setComponentAt(OTHER_TAB_INDEX, createTabErrorPanel(
                            "Other details could not load",
                            "Check the database connection and try again.",
                            EmployeeDetailView.this::loadOtherTab
                    ));
                    refreshAfterTabChange();
                }
            }
        };
        worker.execute();
    }

    private void loadDocumentTab() {
        if (documentTabLoaded || documentTabLoading || tabs == null) {
            return;
        }
        documentTabLoading = true;
        tabs.setComponentAt(DOCUMENT_TAB_INDEX, createTabLoadingPanel("Documents", "Fetching saved document paths."));
        LoadingOverlay.Handle loader = LoadingOverlay.show(this, "Loading Documents", "Fetching document references...");

        SwingWorker<Employee, Void> worker = new SwingWorker<>() {
            @Override
            protected Employee doInBackground() {
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    return dao.getEmployeeDocumentsByCode(empCode);
                }
            }

            @Override
            protected void done() {
                loader.close();
                documentTabLoading = false;
                if (!isDisplayable()) {
                    return;
                }
                try {
                    Employee loaded = get();
                    if (loaded == null) {
                        throw new IllegalStateException("Employee record was not found.");
                    }
                    documentPanel = new EmployeeDocumentViewPanel(loaded);
                    documentPanel.setPendingChangesListener(EmployeeDetailView.this::refreshUpdateButtonState);
                    if (basicPanel != null) {
                        documentPanel.setProfileImageUploadListener(basicPanel::setSelectedImageFromDocumentUpload);
                        basicPanel.setSelectedImageListener(documentPanel::setProfileImageFromMainTab);
                    }
                    tabs.setComponentAt(DOCUMENT_TAB_INDEX, documentPanel);
                    documentTabLoaded = true;
                    refreshAfterTabChange();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    tabs.setComponentAt(DOCUMENT_TAB_INDEX, createTabErrorPanel(
                            "Documents could not load",
                            "Check the database connection and try again.",
                            EmployeeDetailView.this::loadDocumentTab
                    ));
                    refreshAfterTabChange();
                }
            }
        };
        worker.execute();
    }

    private List<String> columnsForDefinitions(List<EmployeeFieldDefinition> definitions, boolean includeProfileImage) {
        List<String> columns = new ArrayList<>();
        columns.add("ID");
        columns.add("EMPLOYEE_CODE");
        columns.add("EMP_NAME");
        if (includeProfileImage) {
            columns.add("EMP_IMG");
        }
        if (definitions != null) {
            for (EmployeeFieldDefinition definition : definitions) {
                if (definition != null && definition.columnName() != null && !definition.columnName().isBlank()) {
                    columns.add(definition.columnName());
                }
            }
        }
        return columns;
    }

    private void refreshAfterTabChange() {
        if (centerWrapper != null) {
            centerWrapper.revalidate();
            centerWrapper.repaint();
        }
        if (tabs != null) {
            tabs.revalidate();
            tabs.repaint();
        }
        refreshUpdateButtonState();
    }

    private void refreshUpdateButtonState() {
        if (updateBtn == null) {
            return;
        }
        boolean hasEmployee = empCode != null && !empCode.isBlank();
        boolean canUpdate = false;

        if (basicPanel != null && basicPanel.hasPendingChanges()) {
            canUpdate = true;
        }
        if (!canUpdate && otherPanel != null && otherPanel.hasPendingChanges()) {
            canUpdate = true;
        }
        if (!canUpdate && documentPanel != null && documentPanel.hasPendingDocumentUpdates()) {
            canUpdate = true;
        }

        updateBtn.setEnabled(hasEmployee && canUpdate);
    }

    private void handleUpdate() {
        if (empCode == null || empCode.isBlank()) {
            DialogHelper.warning(this, "Missing Employee Code", "Employee code is required to update details.");
            return;
        }

        Employee basicUpdate = null;
        File selectedImage = null;
        if (basicPanel != null && basicPanel.hasPendingChanges()) {
            String validationMessage = basicPanel.validationMessage();
            if (validationMessage != null) {
                DialogHelper.warning(this, "Check Employee Details", validationMessage);
                return;
            }
            basicUpdate = basicPanel.getEmployeeFromForm();
            basicUpdate.setEMPLOYEE_CODE(empCode);
            selectedImage = basicPanel.getSelectedImage();
        }

        Employee otherUpdate = null;
        if (otherPanel != null && otherPanel.hasPendingChanges()) {
            otherUpdate = otherPanel.getUpdatedOtherDetails();
            otherUpdate.setEMPLOYEE_CODE(empCode);
        }

        boolean hasDocumentUpdates = documentPanel != null && documentPanel.hasPendingDocumentUpdates();
        if (basicUpdate == null && otherUpdate == null && !hasDocumentUpdates) {
            DialogHelper.warning(this, "No Editable Fields", "Open a tab with editable fields or upload a document first.");
            return;
        }

        final Employee finalBasicUpdate = basicUpdate;
        final File finalSelectedImage = selectedImage;
        final Employee finalOtherUpdate = otherUpdate;
        final EmployeeDocumentViewPanel finalDocumentPanel = documentPanel;
        LoadingOverlay.Handle loader = LoadingOverlay.show(this, "Saving Employee", "Preparing changes...");

        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                boolean documentsSaved = false;
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    if (finalBasicUpdate != null) {
                        publish("Saving Basic details...");
                        if (finalSelectedImage != null) {
                            finalBasicUpdate.setEMP_IMG(copyProfileImage(finalSelectedImage, empCode));
                        }
                        dao.updateEmployeeDynamic(finalBasicUpdate);
                    }

                    if (finalOtherUpdate != null) {
                        publish("Saving Other details...");
                        dao.updateEmployeeDynamic(finalOtherUpdate);
                    }

                    if (hasDocumentUpdates && finalDocumentPanel != null) {
                        publish("Copying document files...");
                        Employee documentUpdates = finalDocumentPanel.getDocumentUpdates(empCode);
                        documentUpdates.setEMPLOYEE_CODE(empCode);
                        dao.updateEmployeeDynamic(documentUpdates);
                        documentsSaved = true;
                    }
                }

                return documentsSaved;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    loader.setMessage(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                loader.close();
                if (!isDisplayable()) {
                    return;
                }
                try {
                    boolean documentsSaved = get();
                    DialogHelper.success(EmployeeDetailView.this, "Updated successfully.");
                    if (documentsSaved) {
                        invalidateDocumentTab();
                    }
                    refreshUpdateButtonState();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    DialogHelper.error(EmployeeDetailView.this, "Update Failed", "Update failed.");
                }
            }
        };
        worker.execute();
    }

    private void invalidateDocumentTab() {
        documentPanel = null;
        documentTabLoaded = false;
        documentTabLoading = false;
        if (tabs == null || tabs.getTabCount() <= DOCUMENT_TAB_INDEX) {
            return;
        }
        tabs.setComponentAt(DOCUMENT_TAB_INDEX, createTabLoadingPanel("Documents", "Refreshing saved document paths."));
        refreshAfterTabChange();
        if (tabs.getSelectedIndex() == DOCUMENT_TAB_INDEX) {
            SwingUtilities.invokeLater(this::loadDocumentTab);
        }
    }

    private void showLoadingShell() {
        getContentPane().removeAll();
        EmployeeDetailViewHelper.applyFrame(this);

        JPanel topContainer = EmployeeDetailViewHelper.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);
        topContainer.add(EmployeeDetailViewHelper.screenHeader(
                "Loading employee details",
                empCode == null ? "" : empCode,
                () -> {
                    new HomeView();
                    dispose();
                },
                null
        ), BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeDetailViewHelper.createCenterWrapper();

        JPanel blankPanel = new JPanel(new BorderLayout());
        blankPanel.setBackground(Color.WHITE);
        blankPanel.setBorder(BorderFactory.createEmptyBorder(40, 28, 56, 28));

        centerWrapper.add(blankPanel, EmployeeDetailViewHelper.pageConstraints(0));
        add(EmployeeDetailViewHelper.createPageScrollPane(centerWrapper), BorderLayout.CENTER);

        JPanel footerActions = EmployeeDetailViewHelper.createActionRow();
        updateBtn = new JButton("Update");
        EmployeeDetailViewHelper.styleUpdateButton(updateBtn);
        updateBtn.setEnabled(false);
        footerActions.add(updateBtn);

        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.setBackground(Color.WHITE);
        southContainer.add(footerActions, BorderLayout.NORTH);
        southContainer.add(new FooterPanel(), BorderLayout.SOUTH);
        add(southContainer, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private void loadEmployeeAsync() {
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Loading Employee",
                "Preparing employee detail screen..."
        );
        SwingWorker<DetailLoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DetailLoadResult doInBackground() {
                if (empCode == null || empCode.isBlank()) {
                    return new DetailLoadResult(null, List.of(), List.of());
                }
                Employee loadedEmployee;
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    loadedEmployee = dao.getEmployeeHeaderByCode(empCode);
                }
                if (loadedEmployee == null) {
                    return new DetailLoadResult(null, List.of(), List.of());
                }
                return new DetailLoadResult(
                        loadedEmployee,
                        EmployeeBasicFieldUtil.loadBasicDefinitions(),
                        loadDetailDefinitions()
                );
            }

            @Override
            protected void done() {
                loader.close();
                if (!isDisplayable()) {
                    return;
                }
                try {
                    DetailLoadResult detailData = get();
                    if (detailData.employee() == null) {
                        DialogHelper.warning(
                                EmployeeDetailView.this,
                                "Employee Not Found",
                                "This employee record could not be found.");
                        new HomeView();
                        dispose();
                        return;
                    }
                    initializeUI(detailData, true);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    DialogHelper.error(
                            EmployeeDetailView.this,
                            "Load Failed",
                            "Employee details could not be loaded.");
                    new HomeView();
                    dispose();
                }
            }
        };
        worker.execute();
    }

    private List<EmployeeFieldDefinition> loadDetailDefinitions() {
        try {
            return EmployeeFieldDefinitionCache.detailFields();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return List.of();
        }
    }

    private String copyProfileImage(File source, String employeeCode) throws IOException {
        Path employeeDir = EmployeeStorageUtil.ensureEmployeeDirectory(employeeCode);
        Path destination = employeeDir.resolve("EMP_IMG.jpg");
        Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return EmployeeStorageUtil.profileImagePath(employeeCode);
    }

    private void downloadEmployeeReport() {
        if (empCode == null || empCode.isBlank()) {
            DialogHelper.warning(this, "Missing Employee Code", "Employee code is required to generate a report.");
            return;
        }

        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Preparing Download",
                "Loading employee profile and saved documents..."
        );

        SwingWorker<ReportPreparation, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportPreparation doInBackground() {
                EmployeeReportService reportService = new EmployeeReportService();
                Employee currentEmployee;
                try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
                    currentEmployee = dao.getFullEmployeeByCode(empCode);
                }
                if (currentEmployee == null) {
                    return null;
                }
                return new ReportPreparation(
                        currentEmployee,
                        reportService.availableDocuments(currentEmployee),
                        reportService.suggestedPackageFolderName(currentEmployee)
                );
            }

            @Override
            protected void done() {
                loader.close();
                if (!isDisplayable()) {
                    return;
                }
                try {
                    ReportPreparation preparation = get();
                    if (preparation == null || preparation.employee() == null) {
                        DialogHelper.warning(EmployeeDetailView.this, "Employee Not Found", "This employee record could not be found.");
                        return;
                    }
                    continueDownloadEmployeeReport(preparation);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    DialogHelper.error(EmployeeDetailView.this, "Download Profile Failed", "Employee details could not be loaded.");
                }
            }
        };
        worker.execute();
    }

    private void continueDownloadEmployeeReport(ReportPreparation preparation) {
        Employee currentEmployee = preparation.employee();
        this.employee = currentEmployee;

        List<EmployeeReportService.AvailableDocument> availableDocuments = preparation.availableDocuments();
        EmployeeReportService.PackageOptions options = showDownloadOptionsDialog(availableDocuments);
        if (options == null) {
            return;
        }

        File selectedFolder = FileUploadCard.chooseDirectory(
                this,
                "Save Employee Report Package",
                preparation.suggestedFolderName()
        );
        if (selectedFolder == null) {
            return;
        }

        generateEmployeeReportPackage(selectedFolder, options);
    }

    private void generateEmployeeReportPackage(
            File selectedFolder,
            EmployeeReportService.PackageOptions options
    ) {
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Building Package",
                "Creating profile PDF and copying selected files..."
        );

        SwingWorker<EmployeeReportService.PackageResult, String> worker = new SwingWorker<>() {
            @Override
            protected EmployeeReportService.PackageResult doInBackground() throws Exception {
                publish("Loading latest saved values...");
                EmployeeReportService reportService = new EmployeeReportService();
                publish("Writing package files...");
                return reportService.generateEmployeePackageAt(empCode, selectedFolder, options);
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    loader.setMessage(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                loader.close();
                if (!isDisplayable()) {
                    return;
                }
                try {
                    EmployeeReportService.PackageResult result = get();
                    String pdfStatus = result.pdfFile() == null ? "Not included" : "Saved";
                    String mergedPdfStatus = result.mergedDocumentsPdfFile() == null
                            ? "Not included"
                            : "Saved (" + result.mergedDocumentCount() + " documents)";
                    String message = "Folder: " + result.folder().getAbsolutePath()
                            + "\nPDF profile: " + pdfStatus
                            + "\nAll documents PDF: " + mergedPdfStatus
                            + "\nDocuments copied: " + result.copiedDocumentCount()
                            + " / " + result.totalDocumentCount();
                    int choice = DialogHelper.successOption(
                            EmployeeDetailView.this,
                            "Download Folder Ready",
                            message,
                            "Open Folder",
                            "OK"
                    );
                    if (choice == 0) {
                        openReportFolder(result.folder());
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    String message = exception.getMessage() == null || exception.getMessage().isBlank()
                            ? "Employee report package could not be generated."
                            : exception.getMessage();
                    DialogHelper.error(EmployeeDetailView.this, "Download Report Failed", message);
                }
            }
        };
        worker.execute();
    }

    private void openReportFolder(File folder) {
        if (folder == null || !folder.isDirectory()) {
            DialogHelper.warning(this, "Folder Not Found", "The report folder could not be found.");
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

    private EmployeeReportService.PackageOptions showDownloadOptionsDialog(
            List<EmployeeReportService.AvailableDocument> availableDocuments
    ) {
        JDialog dialog = new JDialog(this, "Download Employee Report", true);
        EmployeeReportService.PackageOptions[] selectedOptions = new EmployeeReportService.PackageOptions[1];

        final Color primary = new Color(20, 101, 192);
        final Color primarySoft = new Color(232, 241, 255);
        final Color pageBg = new Color(245, 247, 250);
        final Color cardBg = Color.WHITE;
        final Color textDark = new Color(17, 24, 39);
        final Color textMuted = new Color(100, 116, 139);
        final Color border = new Color(226, 232, 240);
        final Color success = new Color(22, 101, 52);
        final Color successSoft = new Color(220, 252, 231);
        final Color warning = new Color(180, 83, 9);
        final Color warningSoft = new Color(254, 243, 199);
        final Color error = new Color(185, 28, 28);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(pageBg);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(cardBg);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, border),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));

        JLabel icon = new JLabel("↓", SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(primarySoft);
        icon.setForeground(primary);
        icon.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 22));
        icon.setPreferredSize(new Dimension(48, 48));
        icon.setBorder(new RoundedBorder(primarySoft, 24, 0));
        header.add(icon, BorderLayout.WEST);

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Download Employee Report");
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 20));
        title.setForeground(textDark);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(htmlWrap("Build a clean employee package with profile PDF, merged documents PDF, and separate saved files."));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(textMuted);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitle);
        header.add(headerText, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(pageBg);
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.getViewport().setBackground(pageBg);
        bodyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(18);
        bodyScroll.getVerticalScrollBar().setBlockIncrement(120);
        styleModernScrollPane(bodyScroll);

        JCheckBox pdfProfile = createDownloadCheckbox("Employee profile PDF", true);
        JCheckBox allDocumentsPdf = createDownloadCheckbox("All Documents as one PDF", false);
        JCheckBox allDocuments = createDownloadCheckbox("Copy all saved documents", true);

        JPanel optionsCard = createModernCard();
        optionsCard.add(createSectionHeader(
                "Package contents",
                "Select what should be generated or copied into the employee folder."
        ));
        optionsCard.add(Box.createVerticalStrut(12));
        optionsCard.add(createOptionCard(
                pdfProfile,
                "Generate a readable employee profile report.",
                "Recommended",
                successSoft,
                success
        ));
        optionsCard.add(Box.createVerticalStrut(10));
        optionsCard.add(createOptionCard(
                allDocumentsPdf,
                "Combine supported document images into one PDF file.",
                "Optional",
                primarySoft,
                primary
        ));
        optionsCard.add(Box.createVerticalStrut(10));
        optionsCard.add(createOptionCard(
                allDocuments,
                "Copy every available saved document separately. Turn this off to pick specific files.",
                "Files",
                warningSoft,
                warning
        ));

        JPanel documentSection = createModernCard();
        documentSection.add(createSectionHeader(
                "Choose separate documents",
                "Shown only when Copy all saved documents is off. Missing files stay visible but disabled."
        ));
        documentSection.add(Box.createVerticalStrut(12));

        JPanel documentList = new JPanel();
        documentList.setLayout(new BoxLayout(documentList, BoxLayout.Y_AXIS));
        documentList.setBackground(cardBg);
        documentList.setAlignmentX(Component.LEFT_ALIGNMENT);

        Map<String, JCheckBox> documentChecks = new LinkedHashMap<>();
        for (EmployeeReportService.AvailableDocument availableDocument : availableDocuments) {
            JCheckBox checkbox = createDownloadCheckbox(availableDocument.label(), false);
            checkbox.setEnabled(availableDocument.fileReady() && !allDocuments.isSelected());
            checkbox.setToolTipText(availableDocument.fileReady()
                    ? "Include this document as a separate copied file."
                    : "This document path exists, but the file is missing.");
            documentChecks.put(availableDocument.label(), checkbox);
            documentList.add(createDocumentCard(checkbox, availableDocument));
            documentList.add(Box.createVerticalStrut(8));
        }

        if (availableDocuments.isEmpty()) {
            JPanel empty = createInfoBox(
                    "No saved documents found",
                    "This employee has no ready document files to copy. You can still generate the profile PDF.",
                    warningSoft,
                    warning
            );
            documentList.add(empty);
        }

        JScrollPane documentListScroll = new JScrollPane(documentList);
        documentListScroll.setBorder(new RoundedBorder(border, 14, 1));
        documentListScroll.getViewport().setBackground(cardBg);
        documentListScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        documentListScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        documentListScroll.getVerticalScrollBar().setUnitIncrement(16);
        styleModernScrollPane(documentListScroll);
        documentListScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        documentSection.add(documentListScroll);

        JPanel selectionSummary = createInfoBox("Ready", "All ready saved documents will be copied separately.", successSoft, success);
        JLabel selectionTitle = (JLabel) selectionSummary.getClientProperty("titleLabel");
        JLabel selectionDescription = (JLabel) selectionSummary.getClientProperty("descriptionLabel");

        JLabel validation = new JLabel(" ");
        validation.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        validation.setForeground(error);
        validation.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
        validation.setAlignmentX(Component.LEFT_ALIGNMENT);

        Runnable refreshState = () -> {
            boolean chooseSpecificDocs = !allDocuments.isSelected();

            documentSection.setVisible(chooseSpecificDocs);

            for (JCheckBox checkbox : documentChecks.values()) {
                EmployeeReportService.AvailableDocument doc = findDocumentByLabel(availableDocuments, checkbox.getText());
                boolean fileReady = doc != null && doc.fileReady();
                checkbox.setEnabled(chooseSpecificDocs && fileReady);
                if (!chooseSpecificDocs) {
                    checkbox.setSelected(false);
                }
            }

            long readyCount = availableDocuments.stream().filter(EmployeeReportService.AvailableDocument::fileReady).count();
            long selectedCount = documentChecks.values().stream().filter(JCheckBox::isSelected).count();

            if (allDocuments.isSelected()) {
                selectionTitle.setText("Copy mode: All documents");
                selectionDescription.setText(readyCount == 0
                        ? "No saved document files are ready to copy."
                        : "All ready saved documents will be copied separately: " + readyCount);
            } else {
                selectionTitle.setText("Copy mode: Specific documents");
                selectionDescription.setText(selectedCount == 0
                        ? "Choose one or more ready documents, or turn on Copy all saved documents."
                        : "Selected separate documents: " + selectedCount);
            }

            validation.setText(" ");
            documentList.revalidate();
            documentList.repaint();
            body.revalidate();
            body.repaint();
            bodyScroll.revalidate();
            bodyScroll.repaint();
            dialog.pack();
            resizeReportDialog(dialog);
            dialog.setLocationRelativeTo(this);
        };

        allDocuments.addItemListener(event -> refreshState.run());
        for (JCheckBox checkbox : documentChecks.values()) {
            checkbox.addItemListener(event -> refreshState.run());
        }

        body.add(optionsCard);
        body.add(Box.createVerticalStrut(14));
        body.add(documentSection);
        body.add(Box.createVerticalStrut(14));
        body.add(selectionSummary);
        body.add(Box.createVerticalStrut(8));
        body.add(validation);

        JPanel footer = new JPanel(new BorderLayout(12, 8));
        footer.setBackground(cardBg);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, border),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel footerHint = new JLabel(htmlWrap("Next step: choose a folder where the package should be saved."));
        footerHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerHint.setForeground(textMuted);
        footer.add(footerHint, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        JButton cancel = createDialogButton("Cancel", false);
        JButton chooseFolder = createDialogButton("Choose Folder", true);
        buttonRow.add(cancel);
        buttonRow.add(chooseFolder);
        footer.add(buttonRow, BorderLayout.EAST);

        cancel.addActionListener(event -> dialog.dispose());
        chooseFolder.addActionListener(event -> {
            List<String> selectedDocumentLabels = new ArrayList<>();
            if (!allDocuments.isSelected()) {
                for (Map.Entry<String, JCheckBox> entry : documentChecks.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selectedDocumentLabels.add(entry.getKey());
                    }
                }
            }

            boolean hasDocumentSelection = allDocuments.isSelected()
                    ? hasReadyDocument(availableDocuments)
                    : hasReadySelectedDocument(availableDocuments, selectedDocumentLabels);

            boolean hasMergedDocumentPdfSelection = allDocumentsPdf.isSelected()
                    && hasReadyMergeableDocument(availableDocuments);

            if (allDocumentsPdf.isSelected() && !hasMergedDocumentPdfSelection) {
                validation.setText("No saved document images are available for the All Documents PDF.");
                return;
            }

            if (!pdfProfile.isSelected() && !hasDocumentSelection && !hasMergedDocumentPdfSelection) {
                validation.setText("Select profile PDF, All Documents PDF, all documents, or at least one separate document.");
                return;
            }

            selectedOptions[0] = new EmployeeReportService.PackageOptions(
                    pdfProfile.isSelected(),
                    allDocuments.isSelected(),
                    allDocumentsPdf.isSelected(),
                    selectedDocumentLabels
            );
            dialog.dispose();
        });

        root.add(header, BorderLayout.NORTH);
        root.add(bodyScroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(chooseFolder);
        refreshState.run();
        resizeReportDialog(dialog);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return selectedOptions[0];
    }

    private JPanel createModernCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(226, 232, 240), 18, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel createSectionHeader(String titleText, String helperText) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        title.setForeground(new Color(17, 24, 39));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel helper = new JLabel(htmlWrap(helperText));
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helper.setForeground(new Color(100, 116, 139));
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(helper);
        return header;
    }

    private JPanel createOptionCard(
            JCheckBox checkbox,
            String helperText,
            String badgeText,
            Color badgeBg,
            Color badgeFg
    ) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setBackground(new Color(248, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(226, 232, 240), 14, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(checkbox);

        JLabel helper = new JLabel(htmlWrap(helperText));
        helper.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helper.setForeground(new Color(100, 116, 139));
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(helper);

        row.add(left, BorderLayout.CENTER);
        row.add(createBadge(badgeText, badgeBg, badgeFg), BorderLayout.EAST);
        return row;
    }

    private JPanel createDocumentCard(
            JCheckBox checkbox,
            EmployeeReportService.AvailableDocument availableDocument
    ) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setBackground(new Color(248, 250, 252));
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(226, 232, 240), 12, 1),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)
        ));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(checkbox);

        String statusText;
        Color badgeBg;
        Color badgeFg;
        String badge;
        if (!availableDocument.fileReady()) {
            statusText = "File missing — cannot be included.";
            badgeBg = new Color(254, 226, 226);
            badgeFg = new Color(185, 28, 28);
            badge = "Missing";
        } else if (availableDocument.mergeableForDocumentsPdf()) {
            statusText = "Ready — can be copied and included in All Documents PDF.";
            badgeBg = new Color(220, 252, 231);
            badgeFg = new Color(22, 101, 52);
            badge = "PDF Ready";
        } else {
            statusText = "Ready — can be copied separately.";
            badgeBg = new Color(232, 241, 255);
            badgeFg = new Color(20, 101, 192);
            badge = "Ready";
        }

        JLabel status = new JLabel(htmlWrap(statusText));
        status.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));
        status.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        status.setForeground(new Color(100, 116, 139));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(status);

        row.add(left, BorderLayout.CENTER);
        row.add(createBadge(badge, badgeBg, badgeFg), BorderLayout.EAST);
        return row;
    }

    private JPanel createInfoBox(String titleText, String detailText, Color bg, Color fg) {
        JPanel box = new JPanel(new BorderLayout(10, 0));
        box.setBackground(bg);
        box.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(bg, 14, 0),
                BorderFactory.createEmptyBorder(11, 13, 11, 13)
        ));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dot = new JLabel("●", SwingConstants.CENTER);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dot.setForeground(fg);
        box.add(dot, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        title.setForeground(fg);
        JLabel desc = new JLabel(htmlWrap(detailText));
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(fg);

        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(desc);
        box.add(text, BorderLayout.CENTER);

        box.putClientProperty("titleLabel", title);
        box.putClientProperty("descriptionLabel", desc);
        return box;
    }

    private JLabel createBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(fg);
        badge.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(bg, 20, 0),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return badge;
    }

    private EmployeeReportService.AvailableDocument findDocumentByLabel(
            List<EmployeeReportService.AvailableDocument> availableDocuments,
            String label
    ) {
        for (EmployeeReportService.AvailableDocument document : availableDocuments) {
            if (document.label().equals(label)) {
                return document;
            }
        }
        return null;
    }

    private void resizeReportDialog(JDialog dialog) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int availableWidth = Math.max(360, screen.width - 90);
        int availableHeight = Math.max(320, screen.height - 110);
        int width = Math.min(820, Math.max(500, (int) (screen.width * 0.58)));
        int height = Math.min(680, Math.max(420, (int) (screen.height * 0.78)));
        width = Math.min(width, availableWidth);
        height = Math.min(height, availableHeight);
        dialog.setMinimumSize(new Dimension(Math.min(460, width), Math.min(360, height)));
        dialog.setPreferredSize(new Dimension(width, height));
        dialog.setSize(new Dimension(width, height));
    }

    private void styleModernScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 9));
        scrollPane.getViewport().setOpaque(true);
    }

    private String htmlWrap(String text) {
        return "<html><body style='width:360px'>" + text + "</body></html>";
    }

    private boolean hasReadyDocument(List<EmployeeReportService.AvailableDocument> availableDocuments) {
        for (EmployeeReportService.AvailableDocument document : availableDocuments) {
            if (document.fileReady()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReadySelectedDocument(
            List<EmployeeReportService.AvailableDocument> availableDocuments,
            List<String> selectedDocumentLabels
    ) {
        for (EmployeeReportService.AvailableDocument document : availableDocuments) {
            if (document.fileReady() && selectedDocumentLabels.contains(document.label())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReadyMergeableDocument(List<EmployeeReportService.AvailableDocument> availableDocuments) {
        for (EmployeeReportService.AvailableDocument document : availableDocuments) {
            if (document.fileReady() && document.mergeableForDocumentsPdf()) {
                return true;
            }
        }
        return false;
    }

    private JCheckBox createDownloadCheckbox(String text, boolean selected) {
        JCheckBox checkbox = new JCheckBox(text, selected);
        checkbox.setOpaque(false);
        checkbox.setForeground(new Color(17, 24, 39));
        checkbox.setFocusPainted(false);
        checkbox.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        checkbox.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return checkbox;
    }

    private JButton createDialogButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(primary ? 142 : 96, 38));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(primary ? new Color(20, 101, 192) : new Color(226, 232, 240), 18, primary ? 0 : 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        if (primary) {
            button.setBackground(new Color(22, 163, 74));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(new Color(71, 85, 105));
            button.setBorderPainted(true);
        }
        com.kgm.ui.styling.ButtonStateHelper.install(button);
        return button;
    }

    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (thickness <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            for (int i = 0; i < thickness; i++) {
                g2.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, radius, radius);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = thickness;
            insets.top = thickness;
            insets.right = thickness;
            insets.bottom = thickness;
            return insets;
        }
    }

    private boolean panelHasEditableFields(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextField tf && tf.isEditable()) {
                return true;
            }

            if (comp instanceof JTextArea ta && ta.isEditable()) {
                return true;
            }

            if (comp instanceof JComboBox<?> cb && cb.isEnabled()) {
                return true;
            }

            if (comp instanceof JSpinner sp && sp.isEnabled()) {
                return true;
            }

            if (comp instanceof UniversalDatePicker udp && udp.isEnabled()) {
                return true;
            }

            if (comp instanceof Container child && panelHasEditableFields(child)) {
                return true;
            }
        }

        return false;
    }

    private record DetailLoadResult(
            Employee employee,
            List<EmployeeFieldDefinition> basicDefinitions,
            List<EmployeeFieldDefinition> detailDefinitions
    ) {
    }

    private record ReportPreparation(
            Employee employee,
            List<EmployeeReportService.AvailableDocument> availableDocuments,
            String suggestedFolderName
    ) {
    }

    private static class HugHeightTabbedPane extends JTabbedPane {
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            Component selected = getSelectedComponent();
            if (selected == null) {
                return preferred;
            }

            int tallestTabContentHeight = 0;
            for (int index = 0; index < getTabCount(); index++) {
                Component component = getComponentAt(index);
                if (component != null) {
                    tallestTabContentHeight = Math.max(
                            tallestTabContentHeight,
                            component.getPreferredSize().height
                    );
                }
            }

            int tabChromeHeight = Math.max(0, preferred.height - tallestTabContentHeight);
            Dimension selectedSize = selected.getPreferredSize();
            return new Dimension(preferred.width, selectedSize.height + tabChromeHeight);
        }
    }
}
