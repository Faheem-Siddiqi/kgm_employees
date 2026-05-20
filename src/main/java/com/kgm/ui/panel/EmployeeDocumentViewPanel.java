package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeDocumentViewPanelHelper;
import com.kgm.ui.styling.TablePaginationHelper;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class EmployeeDocumentViewPanel extends JPanel {
    private static final long MAX_SIZE = 400 * 1024;

    private static final String[] DOCUMENTS = {
            "CNIC *", "EOBI Card *", "SS_CARD_COPY*", "Final Settlement",
            "Clearance Certificate", "Job Appointment Letter", "Application Letter",
            "Issuance Form", "Settlement Document", "Trial Card",
            "Interview Form", "Service Letter", "Extension Letter",
            "Retirement Letter", "Covid Certification", "DISCIPLINARY_I", "DISCIPLINARY_II", "DISCIPLINARY_III"
    };

    private static final String[] STORAGE_NAMES = {
            "CNIC_COPY.jpg",
            "EOBI_CARD_COPY.jpg",
            "SS_CARD_COPY.jpg",
            "FINAL_SETTLEMENT.jpg",
            "CLEARANCE_CERT.jpg",
            "JOB_APPOINTMENT.jpg",
            "APPLICATION_DOC.jpg",
            "ISSUANCE_DOC.jpg",
            "SETTLEMENT_DOC.jpg",
            "TRIAL_CARD.jpg",
            "INTERVIEW_DOC.jpg",
            "SERVICE_LETTER.jpg",
            "EXTENSION_LETTER.jpg",
            "RETIREMENT_LETTER.jpg",
            "COVID_CERT.jpg",
            "DISCIPLINARY_I.jpg",
            "DISCIPLINARY_II.jpg",
            "DISCIPLINARY_III.jpg"
    };

    private JTable table;
    private DefaultTableModel model;
    private File[] files;
    private String[] filePaths;
    private boolean[] lockedDocuments;
    private JLabel uploadedCountLabel;

    public EmployeeDocumentViewPanel() {
        this(null);
    }

    public EmployeeDocumentViewPanel(Employee employee) {
        EmployeeDocumentViewPanelHelper.stylePanel(this);

        files = new File[DOCUMENTS.length];
        filePaths = new String[DOCUMENTS.length];
        lockedDocuments = new boolean[DOCUMENTS.length];

        JPanel topPanel = EmployeeDocumentViewPanelHelper.createTopPanel();
        uploadedCountLabel = EmployeeDocumentViewPanelHelper.createUploadedCountLabel("Total fields uploaded: 0");
        JLabel sizeLabel = EmployeeDocumentViewPanelHelper.createSizeLabel();

        topPanel.add(uploadedCountLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(sizeLabel);
        topPanel.add(Box.createVerticalStrut(10));
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Document", "File", "Status", "Actions"};
        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        loadDocumentRows(employee);

        table = TablePaginationHelper.createDocumentTable(model);
        table.getColumnModel().getColumn(3).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new ActionEditor());

        JScrollPane scrollPane = TablePaginationHelper.createScrollPane(table, false);
        showFullTableWithoutScroll(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadDocumentRows(Employee employee) {
        for (int index = 0; index < DOCUMENTS.length; index++) {
            String path = documentPath(employee, index);
            if (hasStoredPath(path)) {
                filePaths[index] = path;
                lockedDocuments[index] = true;
                File resolved = resolveStoredFile(path);
                if (resolved.exists()) {
                    files[index] = resolved;
                    model.addRow(new Object[]{DOCUMENTS[index], resolved.getName(), "Uploaded", "View"});
                } else {
                    model.addRow(new Object[]{DOCUMENTS[index], fileNameFromPath(path), "Saved Path Missing", "Locked"});
                }
            } else {
                model.addRow(new Object[]{DOCUMENTS[index], "-", "Not Uploaded", "Upload"});
            }
        }
        updateCount();
    }

    private void showFullTableWithoutScroll(JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        table.setFillsViewportHeight(false);

        int headerHeight = table.getTableHeader() == null ? 0 : table.getTableHeader().getPreferredSize().height;
        int tableHeight = headerHeight + (table.getRowHeight() * table.getRowCount()) + 2;
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, tableHeight));
        scrollPane.setMinimumSize(new Dimension(320, tableHeight));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        return (bytes / (1024 * 1024)) + " MB";
    }

    private void updateCount() {
        int count = 0;
        for (String path : filePaths) {
            if (hasStoredPath(path)) {
                count++;
            }
        }
        if (uploadedCountLabel != null) {
            uploadedCountLabel.setText("Total fields uploaded: " + count);
        }
    }

    private void chooseFile(int row) {
        if (lockedDocuments[row]) {
            DialogHelper.warning(this, "Document Locked",
                    "This document already has a saved record and cannot be replaced.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG Images (*.jpg, *.jpeg)", "jpg", "jpeg"
        ));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String name = file.getName().toLowerCase();

            if (!(name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
                DialogHelper.warning(this, "Invalid File Type", "Only JPG/JPEG files allowed.");
                return;
            }

            if (file.length() > MAX_SIZE) {
                DialogHelper.warning(this, "File Too Large", "Max size 400 KB.");
                return;
            }

            files[row] = file;
            filePaths[row] = file.getAbsolutePath();
            model.setValueAt(file.getName(), row, 1);
            model.setValueAt("Ready to Save (" + formatSize(file.length()) + ")", row, 2);
            updateCount();
            model.fireTableDataChanged();
        }
    }

    private void viewFile(int row) {
        File file = files[row];
        if (file == null && hasStoredPath(filePaths[row])) {
            file = resolveStoredFile(filePaths[row]);
        }
        if (file == null || !file.exists()) {
            DialogHelper.warning(this, "File Not Found", "The saved document file could not be found.");
            return;
        }

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                DialogHelper.error(this, "Cannot Open File", "Cannot open file.");
                return;
            }

            JLabel label = new JLabel(new ImageIcon(img));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);

            JPanel previewPanel = new JPanel(new GridBagLayout());
            previewPanel.setBackground(Color.WHITE);
            previewPanel.add(label);

            JFrame frame = new JFrame("Document Preview - " + file.getName());
            frame.getContentPane().add(new JScrollPane(previewPanel));
            EmployeeDocumentViewPanelHelper.stylePreviewFrame(frame, this);
            frame.setVisible(true);
        } catch (Exception e) {
            DialogHelper.error(this, "Cannot Open File", "Cannot open file.");
        }
    }

    public boolean hasPendingDocumentUpdates() {
        for (int index = 0; index < files.length; index++) {
            if (!lockedDocuments[index] && files[index] != null) {
                return true;
            }
        }
        return false;
    }

    public Employee getDocumentUpdates(String empCode) throws IOException {
        Employee update = new Employee();
        if (!hasPendingDocumentUpdates()) {
            return update;
        }

        File docDir = new File(System.getProperty("user.dir"), "employees/" + empCode + "/documents");
        if (!docDir.exists() && !docDir.mkdirs()) {
            throw new IOException("Could not create employee document folder: " + docDir.getAbsolutePath());
        }

        for (int index = 0; index < files.length; index++) {
            if (lockedDocuments[index] || files[index] == null) {
                continue;
            }

            File dest = new File(docDir, STORAGE_NAMES[index]);
            Files.copy(files[index].toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String dbPath = "employees/" + empCode + "/documents/" + STORAGE_NAMES[index];
            setDocumentPath(update, index, dbPath);
        }
        return update;
    }

    public String getDocumentPath(int index) {
        return filePaths != null && index < filePaths.length ? filePaths[index] : null;
    }

    public String[] getAllDocumentPaths() {
        return filePaths;
    }

    private boolean rowHasViewableFile(int row) {
        if (files[row] != null && files[row].exists()) {
            return true;
        }
        return hasStoredPath(filePaths[row]) && resolveStoredFile(filePaths[row]).exists();
    }

    private boolean rowHasPendingFile(int row) {
        return !lockedDocuments[row] && files[row] != null;
    }

    private String documentPath(Employee employee, int index) {
        if (employee == null) {
            return null;
        }

        return switch (index) {
            case 0 -> employee.getCNIC_COPY();
            case 1 -> employee.getEOBI_CARD_COPY();
            case 2 -> employee.getSS_CARD_COPY();
            case 3 -> employee.getFINAL_SETTLEMENT();
            case 4 -> employee.getCLEARANCE_CERT();
            case 5 -> employee.getJOB_APPOINTMENT();
            case 6 -> employee.getAPPLICATION_DOC();
            case 7 -> employee.getISSUANCE_DOC();
            case 8 -> employee.getSETTLEMENT_DOC();
            case 9 -> employee.getTRIAL_CARD();
            case 10 -> employee.getINTERVIEW_DOC();
            case 11 -> employee.getSERVICE_LETTER();
            case 12 -> employee.getEXTENSION_LETTER();
            case 13 -> employee.getRETIREMENT_LETTER();
            case 14 -> employee.getCOVID_CERT();
            case 15 -> employee.getDISCIPLINARY_I();
            case 16 -> employee.getDISCIPLINARY_II();
            case 17 -> employee.getDISCIPLINARY_III();
            default -> null;
        };
    }

    private void setDocumentPath(Employee employee, int index, String dbPath) {
        switch (index) {
            case 0 -> employee.setCNIC_COPY(dbPath);
            case 1 -> employee.setEOBI_CARD_COPY(dbPath);
            case 2 -> employee.setSS_CARD_COPY(dbPath);
            case 3 -> employee.setFINAL_SETTLEMENT(dbPath);
            case 4 -> employee.setCLEARANCE_CERT(dbPath);
            case 5 -> employee.setJOB_APPOINTMENT(dbPath);
            case 6 -> employee.setAPPLICATION_DOC(dbPath);
            case 7 -> employee.setISSUANCE_DOC(dbPath);
            case 8 -> employee.setSETTLEMENT_DOC(dbPath);
            case 9 -> employee.setTRIAL_CARD(dbPath);
            case 10 -> employee.setINTERVIEW_DOC(dbPath);
            case 11 -> employee.setSERVICE_LETTER(dbPath);
            case 12 -> employee.setEXTENSION_LETTER(dbPath);
            case 13 -> employee.setRETIREMENT_LETTER(dbPath);
            case 14 -> employee.setCOVID_CERT(dbPath);
            case 15 -> employee.setDISCIPLINARY_I(dbPath);
            case 16 -> employee.setDISCIPLINARY_II(dbPath);
            case 17 -> employee.setDISCIPLINARY_III(dbPath);
            default -> {
            }
        }
    }

    private boolean hasStoredPath(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String trimmed = value.trim();
        return !trimmed.equalsIgnoreCase("N/A")
                && !trimmed.equalsIgnoreCase("NA")
                && !trimmed.equalsIgnoreCase("NULL")
                && !trimmed.equals("-");
    }

    private File resolveStoredFile(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(System.getProperty("user.dir"), path);
    }

    private String fileNameFromPath(String path) {
        if (!hasStoredPath(path)) {
            return "-";
        }
        return new File(path).getName();
    }

    class ActionRenderer extends JPanel implements TableCellRenderer {
        public ActionRenderer() {
            EmployeeDocumentViewPanelHelper.styleRendererPanel(this);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            removeAll();
            EmployeeDocumentViewPanelHelper.styleActionCell(this, isSelected);
            JPanel buttons = EmployeeDocumentViewPanelHelper.createActionButtonsPanel();

            if (!lockedDocuments[row]) {
                buttons.add(createLink(rowHasPendingFile(row) ? "Replace" : "Upload"));
            } else {
                JButton locked = createLink("Locked");
                locked.setEnabled(false);
                buttons.add(locked);
            }

            JButton viewBtn = createLink("View");
            EmployeeDocumentViewPanelHelper.styleViewLink(viewBtn, rowHasViewableFile(row));
            buttons.add(viewBtn);
            add(buttons, EmployeeDocumentViewPanelHelper.actionCellConstraints());

            return this;
        }

        private JButton createLink(String text) {
            return EmployeeDocumentViewPanelHelper.createActionLink(text);
        }
    }

    class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private int row;

        public ActionEditor() {
            panel = EmployeeDocumentViewPanelHelper.createEditorPanel();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {

            this.row = row;
            panel.removeAll();
            EmployeeDocumentViewPanelHelper.styleActionCell(panel, isSelected);
            JPanel buttons = EmployeeDocumentViewPanelHelper.createActionButtonsPanel();

            if (!lockedDocuments[row]) {
                buttons.add(createButton(rowHasPendingFile(row) ? "Replace" : "Upload"));
            } else {
                JButton locked = createButton("Locked");
                locked.setEnabled(false);
                buttons.add(locked);
            }

            JButton viewBtn = createButton("View");
            viewBtn.setEnabled(rowHasViewableFile(row));
            EmployeeDocumentViewPanelHelper.styleViewLink(viewBtn, rowHasViewableFile(row));
            buttons.add(viewBtn);
            panel.add(buttons, EmployeeDocumentViewPanelHelper.actionCellConstraints());

            return panel;
        }

        private JButton createButton(String text) {
            JButton btn = EmployeeDocumentViewPanelHelper.createActionLink(text);
            btn.addActionListener(e -> {
                if ("Upload".equals(text) || "Replace".equals(text)) {
                    chooseFile(row);
                } else if ("View".equals(text)) {
                    viewFile(row);
                }
                stopCellEditing();
            });
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}
