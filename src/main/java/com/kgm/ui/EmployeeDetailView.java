package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.Employee;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.panel.EmployeeBasicDetailsPanel;
import com.kgm.ui.panel.EmployeeDocumentViewPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.EmployeeAdditionalDetailsPanel;
import com.kgm.service.EmployeeReportService;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeDetailViewHelper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton updateBtn;

    public EmployeeDetailView(String empCode) {
        this.empCode = (empCode != null) ? empCode.trim() : null;
        Employee emp = null;

        try {
            if (this.empCode != null && !this.empCode.isEmpty()) {
                emp = new EmployeeRecordDao().getFullEmployeeByCode(this.empCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogHelper.error(
                    null,
                    "Error",
                    "An unexpected error occurred.\nPlease contact the administrator.");
        }

        initializeUI(emp, true);
    }

    public EmployeeDetailView() {
        initializeUI(null, false);
    }

    private void initializeUI(Employee emp, boolean isWithData) {
        EmployeeDetailViewHelper.applyFrame(this);

        JPanel topContainer = EmployeeDetailViewHelper.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);
        String nameValue = (emp != null) ? emp.getEMP_NAME() : "";
        String codeValue = (emp != null) ? emp.getEMPLOYEE_CODE() : "";
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeDetailViewHelper.createCenterWrapper();
        Runnable onDownloadReport = isWithData && emp != null ? this::downloadEmployeeReport : null;
        centerWrapper.add(EmployeeDetailViewHelper.screenHeader(
                nameValue,
                codeValue,
                () -> {
                    this.dispose();
                    new HomeView();
                },
                onDownloadReport
        ), EmployeeDetailViewHelper.pageConstraints(0));

        JTabbedPane tabs = new HugHeightTabbedPane();

        EmployeeDocumentViewPanel documentPanel = isWithData && emp != null
                ? new EmployeeDocumentViewPanel(emp)
                : new EmployeeDocumentViewPanel();

        if (isWithData) {
            tabs.addTab("Basic", new EmployeeBasicDetailsPanel(emp));
            tabs.addTab("Others", new EmployeeAdditionalDetailsPanel(emp));
        } else {
            tabs.addTab("Core", new EmployeeBasicDetailsPanel());
            tabs.addTab("Details", new EmployeeAdditionalDetailsPanel());
        }

        tabs.addTab("Documents", documentPanel);

        // Apply custom tab styling
        EmployeeDetailViewHelper.styleTabs(tabs);
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
        footerActions.add(updateBtn);

        JPanel tabContent = EmployeeDetailViewHelper.createTabContent(tabs, footerActions);
        centerWrapper.add(tabContent, EmployeeDetailViewHelper.pageConstraints(1));

        JScrollPane pageScroll = EmployeeDetailViewHelper.createPageScrollPane(centerWrapper);
        tabs.addChangeListener(event -> SwingUtilities.invokeLater(() -> {
            centerWrapper.revalidate();
            centerWrapper.repaint();
            pageScroll.getVerticalScrollBar().setValue(0);
        }));
        EmployeeDetailViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);
        add(pageScroll, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        Runnable refreshButtonState = () -> {
            boolean canUpdate = false;

            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component comp = tabs.getComponentAt(i);

                if (comp instanceof EmployeeBasicDetailsPanel bp && panelHasEditableFields(bp)) {
                    canUpdate = true;
                    break;
                }

                if (comp instanceof EmployeeAdditionalDetailsPanel op && panelHasEditableFields(op)) {
                    canUpdate = true;
                    break;
                }
            }

            updateBtn.setEnabled(canUpdate);
        };

        tabs.addChangeListener(e -> refreshButtonState.run());
        refreshButtonState.run();

        updateBtn.addActionListener(e -> {
            try {
                EmployeeBasicDetailsPanel basicPanel = null;
                EmployeeAdditionalDetailsPanel otherPanel = null;

                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component comp = tabs.getComponentAt(i);

                    if (comp instanceof EmployeeBasicDetailsPanel bp) {
                        basicPanel = bp;
                    }

                    if (comp instanceof EmployeeAdditionalDetailsPanel op) {
                        otherPanel = op;
                    }
                }

                EmployeeRecordDao dao = new EmployeeRecordDao();
                boolean updatedAny = false;

                if (basicPanel != null && panelHasEditableFields(basicPanel)) {
                    Employee updatedBasic = basicPanel.getEmployeeFromForm();
                    File selectedImage = basicPanel.getSelectedImage();
                    if (selectedImage != null) {
                        updatedBasic.setEMP_IMG(copyProfileImage(selectedImage, empCode));
                    }
                    updatedBasic.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(updatedBasic);
                    updatedAny = true;
                }

                if (otherPanel != null && panelHasEditableFields(otherPanel)) {
                    Employee updatedOther = otherPanel.getUpdatedOtherDetails();
                    updatedOther.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(updatedOther);
                    updatedAny = true;
                }

                if (documentPanel.hasPendingDocumentUpdates()) {
                    Employee documentUpdates = documentPanel.getDocumentUpdates(empCode);
                    documentUpdates.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(documentUpdates);
                    updatedAny = true;
                }

                if (!updatedAny) {
                    DialogHelper.warning(this, "No Editable Fields", "No editable fields found.");
                    return;
                }

                DialogHelper.success(this, "Updated successfully.");

            } catch (Exception ex) {
                ex.printStackTrace();
                DialogHelper.error(this, "Update Failed", "Update failed.");
            }
        });
        setVisible(true);
    }

    private String copyProfileImage(File source, String employeeCode) throws IOException {
        File employeeDir = new File(System.getProperty("user.dir"), "employees/" + employeeCode);
        if (!employeeDir.exists() && !employeeDir.mkdirs()) {
            throw new IOException("Could not create employee folder: " + employeeDir.getAbsolutePath());
        }

        File destination = new File(employeeDir, "EMP_IMG.jpg");
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return "employees/" + employeeCode + "/EMP_IMG.jpg";
    }

    private void downloadEmployeeReport() {
        if (empCode == null || empCode.isBlank()) {
            DialogHelper.warning(this, "Missing Employee Code", "Employee code is required to generate a report.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Folder for Employee Report Package");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setApproveButtonText("Save Package Here");

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            EmployeeReportService.PackageResult result = new EmployeeReportService()
                    .generateEmployeePackage(empCode, chooser.getSelectedFile());
            DialogHelper.success(this, "Report package saved successfully.\nFolder: "
                    + result.folder().getAbsolutePath()
                    + "\nDocuments copied: " + result.copiedDocumentCount()
                    + " / " + result.totalDocumentCount());
        } catch (Exception exception) {
            exception.printStackTrace();
            String message = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "Employee report package could not be generated."
                    : exception.getMessage();
            DialogHelper.error(this, "Download Report Failed", message);
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

