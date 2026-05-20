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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton updateBtn;
    private Employee employee;

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
        this.employee = emp;
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

        EmployeeReportService reportService = new EmployeeReportService();
        Employee currentEmployee;
        try {
            currentEmployee = new EmployeeRecordDao().getFullEmployeeByCode(empCode);
        } catch (Exception exception) {
            exception.printStackTrace();
            DialogHelper.error(this, "Download Profile Failed", "Employee details could not be loaded.");
            return;
        }
        if (currentEmployee == null) {
            DialogHelper.warning(this, "Employee Not Found", "This employee record could not be found.");
            return;
        }
        this.employee = currentEmployee;

        List<EmployeeReportService.AvailableDocument> availableDocuments = reportService.availableDocuments(currentEmployee);
        EmployeeReportService.PackageOptions options = showDownloadOptionsDialog(availableDocuments);
        if (options == null) {
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
            EmployeeReportService.PackageResult result = reportService
                    .generateEmployeePackage(empCode, chooser.getSelectedFile(), options);
            String pdfStatus = result.pdfFile() == null ? "Not included" : "Saved";
            String mergedPdfStatus = result.mergedDocumentsPdfFile() == null
                    ? "Not included"
                    : "Saved (" + result.mergedDocumentCount() + " documents)";
            DialogHelper.success(this, "Download folder is ready.\nFolder: "
                    + result.folder().getAbsolutePath()
                    + "\nPDF profile: " + pdfStatus
                    + "\nAll documents PDF: " + mergedPdfStatus
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

    private EmployeeReportService.PackageOptions showDownloadOptionsDialog(
            List<EmployeeReportService.AvailableDocument> availableDocuments
    ) {
        JDialog dialog = new JDialog(this, "Choose Download Contents", true);
        EmployeeReportService.PackageOptions[] selectedOptions = new EmployeeReportService.PackageOptions[1];

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 112, 210));
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JLabel title = new JLabel("Choose Download Contents");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

        JLabel helperText = new JLabel("Select what should be saved in the employee folder.");
        helperText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        helperText.setForeground(new Color(35, 43, 54));
        helperText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox pdfProfile = createDownloadCheckbox("PDF profile", true);
        JCheckBox allDocuments = createDownloadCheckbox("All saved documents", true);
        JCheckBox allDocumentsPdf = createDownloadCheckbox("All Documents (PDF)", false);
        JPanel optionRow = new JPanel(new GridLayout(0, 2, 12, 0));
        optionRow.setBackground(Color.WHITE);
        optionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionRow.add(pdfProfile);
        optionRow.add(allDocuments);
        optionRow.add(allDocumentsPdf);

        JLabel documentHint = new JLabel("The all-documents option is off. Choose the specific files to include.");
        documentHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        documentHint.setForeground(new Color(99, 115, 129));
        documentHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel documentGrid = new JPanel(new GridLayout(0, 2, 8, 6));
        documentGrid.setBackground(Color.WHITE);
        documentGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        Map<String, JCheckBox> documentChecks = new LinkedHashMap<>();
        for (EmployeeReportService.AvailableDocument availableDocument : availableDocuments) {
            String label = availableDocument.fileReady()
                    ? availableDocument.label()
                    : availableDocument.label() + " (file missing)";
            JCheckBox checkbox = createDownloadCheckbox(label, false);
            documentChecks.put(availableDocument.label(), checkbox);
            documentGrid.add(checkbox);
        }
        if (availableDocuments.isEmpty()) {
            JLabel empty = new JLabel(" No saved document files are available for this employee.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            empty.setForeground(new Color(99, 115, 129));
            documentGrid.add(empty);
        }

        JPanel individualWrapper = new JPanel();
        individualWrapper.setLayout(new BoxLayout(individualWrapper, BoxLayout.Y_AXIS));
        individualWrapper.setBackground(Color.WHITE);
        individualWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        individualWrapper.add(documentHint);
        individualWrapper.add(Box.createVerticalStrut(8));
        JScrollPane documentListScroll = new JScrollPane(documentGrid);
        documentListScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 226, 232)));
        documentListScroll.getViewport().setBackground(Color.WHITE);
        documentListScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        documentListScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        documentListScroll.setPreferredSize(new Dimension(470, Math.min(190, Math.max(74, ((availableDocuments.size() + 1) / 2) * 40))));
        documentListScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        individualWrapper.add(documentListScroll);
        individualWrapper.setVisible(false);

        JLabel validation = new JLabel(" ");
        validation.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        validation.setForeground(new Color(217, 45, 32));
        validation.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean[] individualSelectionStarted = {false};
        allDocuments.addItemListener(event -> {
            boolean showIndividualDocuments = !allDocuments.isSelected();
            if (showIndividualDocuments && !individualSelectionStarted[0]) {
                for (JCheckBox checkbox : documentChecks.values()) {
                    checkbox.setSelected(false);
                }
                individualSelectionStarted[0] = true;
            }
            individualWrapper.setVisible(showIndividualDocuments);
            validation.setText(" ");
            individualWrapper.revalidate();
            individualWrapper.repaint();
            dialog.pack();
            dialog.setLocationRelativeTo(this);
        });

        body.add(helperText);
        body.add(Box.createVerticalStrut(16));
        body.add(optionRow);
        body.add(Box.createVerticalStrut(14));
        body.add(individualWrapper);
        body.add(Box.createVerticalStrut(8));
        body.add(validation);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(new Color(247, 249, 251));
        footer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JButton cancel = createDialogButton("Cancel", false);
        JButton chooseFolder = createDialogButton("Choose Folder", true);
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
                validation.setText("Select the PDF profile, All Documents PDF, or at least one document file.");
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
        footer.add(cancel);
        footer.add(chooseFolder);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(chooseFolder);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(520, 260));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return selectedOptions[0];
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
        checkbox.setBackground(Color.WHITE);
        checkbox.setForeground(new Color(35, 43, 54));
        checkbox.setFocusPainted(false);
        checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        checkbox.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        return checkbox;
    }

    private JButton createDialogButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(primary ? 126 : 92, 34));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            button.setBackground(new Color(0, 112, 210));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(new Color(99, 115, 129));
            button.setBorder(BorderFactory.createLineBorder(new Color(220, 226, 232)));
        }
        return button;
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
