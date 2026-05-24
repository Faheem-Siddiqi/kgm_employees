package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeDocumentViewPanelHelper;
import com.kgm.ui.styling.TablePaginationHelper;
import com.kgm.util.EmployeeDocumentUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDocumentViewPanel extends JPanel {
    private static final int ACTION_COLUMN = 3;
    private static final int DOCUMENT_INDEX_COLUMN = 4;

    private JTable table;
    private DefaultTableModel model;
    private File[] files;
    private String[] filePaths;
    private boolean[] lockedDocuments;
    private JLabel uploadedCountLabel;
    private JTextField searchField;
    private JButton clearSearchButton;
    private JScrollPane documentScrollPane;
    private Runnable pendingChangesListener;

    public EmployeeDocumentViewPanel() {
        this(null);
    }

    public EmployeeDocumentViewPanel(Employee employee) {
        EmployeeDocumentViewPanelHelper.stylePanel(this);

        files = new File[EmployeeDocumentUtil.documentCount()];
        filePaths = new String[EmployeeDocumentUtil.documentCount()];
        lockedDocuments = new boolean[EmployeeDocumentUtil.documentCount()];

        JPanel topPanel = EmployeeDocumentViewPanelHelper.createTopPanel();
        uploadedCountLabel = EmployeeDocumentViewPanelHelper.createUploadedCountLabel("Total fields uploaded: 0");
        JLabel sizeLabel = EmployeeDocumentViewPanelHelper.createSizeLabel();
        searchField = new PlaceholderTextField("Search Document Name");
        JButton searchButton = new JButton("Search");
        clearSearchButton = new JButton("Clear");
        JButton uploadAllButton = new JButton("Upload All");

        EmployeeDocumentViewPanelHelper.styleSearchField(searchField);
        EmployeeDocumentViewPanelHelper.styleSearchButton(searchButton);
        EmployeeDocumentViewPanelHelper.styleClearButton(clearSearchButton);
        EmployeeDocumentViewPanelHelper.styleTextCtaButton(uploadAllButton);

        searchButton.addActionListener(e -> refreshDocumentRows());
        uploadAllButton.addActionListener(e -> chooseMultipleFiles());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateSearch();
            }

            public void removeUpdate(DocumentEvent event) {
                updateSearch();
            }

            public void changedUpdate(DocumentEvent event) {
                updateSearch();
            }

            private void updateSearch() {
                EmployeeDocumentViewPanelHelper.updateClearButtonState(
                        clearSearchButton,
                        !searchField.getText().trim().isEmpty()
                );
                refreshDocumentRows();
            }
        });

        topPanel.add(uploadedCountLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(sizeLabel);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(EmployeeDocumentViewPanelHelper.createSearchPanel(searchField, clearSearchButton, searchButton));
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(EmployeeDocumentViewPanelHelper.createBulkActionPanel(uploadAllButton));
        topPanel.add(Box.createVerticalStrut(10));
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Document", "File", "Status", "Actions", "DocumentIndex"};
        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == ACTION_COLUMN;
            }
        };

        loadDocumentState(employee);
        refreshDocumentRows();

        table = TablePaginationHelper.createDocumentTable(model);
        hideDocumentIndexColumn();
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellEditor(new ActionEditor());

        documentScrollPane = TablePaginationHelper.createScrollPane(table, false);
        showFullTableWithoutScroll(documentScrollPane);
        add(documentScrollPane, BorderLayout.CENTER);
    }

    private void loadDocumentState(Employee employee) {
        for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
            String path = EmployeeDocumentUtil.documentPath(employee, index);
            if (EmployeeDocumentUtil.hasStoredPath(path)) {
                filePaths[index] = path;
                lockedDocuments[index] = true;
                File resolved = EmployeeDocumentUtil.resolveStoredFile(path);
                if (resolved.exists()) {
                    files[index] = resolved;
                }
            }
        }
        updateCount();
    }

    private void refreshDocumentRows() {
        if (model == null) {
            return;
        }
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        String query = EmployeeDocumentUtil.normalizedSearch(searchField == null ? "" : searchField.getText());
        List<Integer> orderedIndexes = new ArrayList<>();
        for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
            orderedIndexes.add(index);
        }

        if (!query.isEmpty()) {
            orderedIndexes.removeIf(index -> matchScore(index, query) == 2);
            orderedIndexes.sort((left, right) -> {
                int score = Integer.compare(matchScore(left, query), matchScore(right, query));
                return score != 0 ? score : Integer.compare(left, right);
            });
        }

        model.setRowCount(0);
        for (Integer documentIndex : orderedIndexes) {
            addDocumentRow(documentIndex);
        }

        if (documentScrollPane != null) {
            showFullTableWithoutScroll(documentScrollPane);
            revalidate();
            repaint();
        }
    }

    private void addDocumentRow(int documentIndex) {
        String documentLabel = EmployeeDocumentUtil.documentType(documentIndex).label();
        if (lockedDocuments[documentIndex]) {
            File resolved = files[documentIndex] != null
                    ? files[documentIndex]
                    : EmployeeDocumentUtil.resolveStoredFile(filePaths[documentIndex]);
            if (resolved.exists()) {
                model.addRow(new Object[]{documentLabel, resolved.getName(), "Uploaded", "View", documentIndex});
            } else {
                model.addRow(new Object[]{documentLabel, EmployeeDocumentUtil.fileNameFromPath(filePaths[documentIndex]), "Saved Path Missing", "Locked", documentIndex});
            }
            return;
        }

        File file = files[documentIndex];
        if (file == null) {
            model.addRow(new Object[]{documentLabel, "-", "Not Uploaded", "Upload", documentIndex});
        } else {
            model.addRow(new Object[]{documentLabel, file.getName(), "Ready to Save (" + EmployeeDocumentUtil.formatSize(file.length()) + ")", "View", documentIndex});
        }
    }

    private void hideDocumentIndexColumn() {
        TableColumn hiddenColumn = table.getColumnModel().getColumn(DOCUMENT_INDEX_COLUMN);
        table.getColumnModel().removeColumn(hiddenColumn);
    }

    private int documentIndexForModelRow(int modelRow) {
        Object value = model.getValueAt(modelRow, DOCUMENT_INDEX_COLUMN);
        return value instanceof Integer ? (Integer) value : Integer.parseInt(String.valueOf(value));
    }

    private int findModelRowByDocumentIndex(int documentIndex) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (documentIndexForModelRow(row) == documentIndex) {
                return row;
            }
        }
        return -1;
    }

    private int matchScore(int documentIndex, String query) {
        List<String> searchableValues = new ArrayList<>();
        searchableValues.add(EmployeeDocumentUtil.documentType(documentIndex).label());
        searchableValues.add(EmployeeDocumentUtil.documentType(documentIndex).employeeFieldName());
        searchableValues.add(EmployeeDocumentUtil.documentType(documentIndex).storageName());
        String path = filePaths[documentIndex];
        if (EmployeeDocumentUtil.hasStoredPath(path)) {
            searchableValues.add(EmployeeDocumentUtil.fileNameFromPath(path));
        }

        int bestScore = 2;
        for (String value : searchableValues) {
            String documentName = EmployeeDocumentUtil.normalizedSearch(value);
            if (documentName.startsWith(query)) {
                return 0;
            }
            if (documentName.contains(query)) {
                bestScore = 1;
            }
        }
        return bestScore;
    }

    private void showFullTableWithoutScroll(JScrollPane scrollPane) {
        if (table == null || scrollPane == null) {
            return;
        }
        TablePaginationHelper.autoResizeColumns(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        table.setFillsViewportHeight(false);

        int headerHeight = table.getTableHeader() == null ? 0 : table.getTableHeader().getPreferredSize().height;
        int tableHeight = headerHeight + (table.getRowHeight() * table.getRowCount()) + 2;
        int tableWidth = Math.max(320, table.getPreferredSize().width + 2);
        scrollPane.setPreferredSize(new Dimension(tableWidth, tableHeight));
        scrollPane.setMinimumSize(new Dimension(320, tableHeight));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, tableHeight));
    }

    private void updateCount() {
        int count = 0;
        for (String path : filePaths) {
            if (EmployeeDocumentUtil.hasStoredPath(path)) {
                count++;
            }
        }
        if (uploadedCountLabel != null) {
            uploadedCountLabel.setText("Total fields uploaded: " + count);
        }
    }

    private void chooseFile(int documentIndex) {
        if (lockedDocuments[documentIndex]) {
            DialogHelper.warning(
                    this,
                    "Document Locked",
                    "Since " + EmployeeDocumentUtil.cleanDocumentLabel(documentIndex) + " already exists in DB, it cannot be replaced."
            );
            return;
        }

        File file = FileUploadCard.chooseFile(
                this,
                "Upload " + EmployeeDocumentUtil.cleanDocumentLabel(documentIndex),
                FileUploadCard.jpegImages()
        );
        if (file == null) {
            return;
        }

        String validationMessage = EmployeeDocumentUtil.validateImageFile(file);
        if (validationMessage != null) {
            DialogHelper.warning(this, "Cannot Upload File", validationMessage);
            return;
        }

        files[documentIndex] = file;
        filePaths[documentIndex] = file.getAbsolutePath();
        int modelRow = findModelRowByDocumentIndex(documentIndex);
        if (modelRow >= 0) {
            model.setValueAt(file.getName(), modelRow, 1);
            model.setValueAt("Ready to Save (" + EmployeeDocumentUtil.formatSize(file.length()) + ")", modelRow, 2);
        }
        updateCount();
        model.fireTableDataChanged();
        notifyPendingChanges();
    }

    private void chooseMultipleFiles() {
        File[] selectedFiles = FileUploadCard.chooseFiles(
                this,
                "Upload Employee Documents",
                FileUploadCard.jpegImages()
        );
        if (selectedFiles.length == 0) {
            return;
        }

        EmployeeDocumentUtil.BulkUploadResult summary =
                EmployeeDocumentUtil.matchBulkFiles(selectedFiles, lockedDocuments);
        for (EmployeeDocumentUtil.BulkUploadItem item : summary.uploadedDocuments()) {
            files[item.documentIndex()] = item.file();
            filePaths[item.documentIndex()] = item.file().getAbsolutePath();
        }

        refreshDocumentRows();
        updateCount();
        model.fireTableDataChanged();
        notifyPendingChanges();
        showBulkUploadSummary(summary);
    }

    private void showBulkUploadSummary(EmployeeDocumentUtil.BulkUploadResult summary) {
        String uploadedText = summary.uploadedCount() == 1
                ? "1 document is ready to save."
                : summary.uploadedCount() + " documents are ready to save.";
        String discardedText = summary.discardedCount() == 1
                ? "1 file was not used."
                : summary.discardedCount() + " files were not used.";

        if (summary.discardedCount() == 0) {
            DialogHelper.success(this, "Bulk upload complete.\n" + uploadedText);
            return;
        }

        DialogHelper.warningSections(
                this,
                "Bulk Upload Complete",
                "Uploaded documents\n" + uploadedText,
                "Files to review\n" + discardedText + "\n" + summary.discardedDetails()
        );
    }

    private void viewFile(int documentIndex) {
        File file = files[documentIndex];
        if (file == null && EmployeeDocumentUtil.hasStoredPath(filePaths[documentIndex])) {
            file = EmployeeDocumentUtil.resolveStoredFile(filePaths[documentIndex]);
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

            JFrame frame = new JFrame("Document Preview - " + file.getName());
            JScrollPane previewScroll = new JScrollPane(new DocumentImagePreviewPanel(img));
            previewScroll.setBorder(null);
            previewScroll.getViewport().setBackground(Color.WHITE);
            frame.getContentPane().add(previewScroll);
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

    public void setPendingChangesListener(Runnable pendingChangesListener) {
        this.pendingChangesListener = pendingChangesListener;
    }

    private void notifyPendingChanges() {
        if (pendingChangesListener != null) {
            pendingChangesListener.run();
        }
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

            String storageName = EmployeeDocumentUtil.documentType(index).storageName();
            File dest = new File(docDir, storageName);
            Files.copy(files[index].toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String dbPath = "employees/" + empCode + "/documents/" + storageName;
            EmployeeDocumentUtil.setDocumentPath(update, index, dbPath);
        }
        return update;
    }

    public String getDocumentPath(int index) {
        return filePaths != null && index < filePaths.length ? filePaths[index] : null;
    }

    public String[] getAllDocumentPaths() {
        return filePaths;
    }

    private boolean hasDocumentReference(int documentIndex) {
        return files[documentIndex] != null
                || EmployeeDocumentUtil.hasStoredPath(filePaths[documentIndex]);
    }

    private boolean rowHasPendingFile(int row) {
        return !lockedDocuments[row] && files[row] != null;
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
            int modelRow = table.convertRowIndexToModel(row);
            int documentIndex = documentIndexForModelRow(modelRow);
            JPanel buttons = EmployeeDocumentViewPanelHelper.createActionButtonsPanel();

            if (!lockedDocuments[documentIndex]) {
                buttons.add(createLink(rowHasPendingFile(documentIndex) ? "Replace" : "Upload"));
            } else {
                JButton locked = createLink("Locked");
                locked.setEnabled(false);
                EmployeeDocumentViewPanelHelper.styleViewLink(locked, false);
                buttons.add(locked);
            }

            JButton viewBtn = createLink("View");
            EmployeeDocumentViewPanelHelper.styleViewLink(viewBtn, hasDocumentReference(documentIndex));
            buttons.add(viewBtn);
            add(buttons, EmployeeDocumentViewPanelHelper.actionCellConstraints());

            return this;
        }

        private JButton createLink(String text) {
            return EmployeeDocumentViewPanelHelper.createActionLink(text);
        }
    }

    class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int documentIndex;

        public ActionEditor() {
            panel = EmployeeDocumentViewPanelHelper.createEditorPanel();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {

            int modelRow = table.convertRowIndexToModel(row);
            this.documentIndex = documentIndexForModelRow(modelRow);
            panel.removeAll();
            EmployeeDocumentViewPanelHelper.styleActionCell(panel, isSelected);
            JPanel buttons = EmployeeDocumentViewPanelHelper.createActionButtonsPanel();

            if (!lockedDocuments[documentIndex]) {
                buttons.add(createButton(rowHasPendingFile(documentIndex) ? "Replace" : "Upload"));
            } else {
                JButton locked = createButton("Locked");
                locked.setEnabled(false);
                EmployeeDocumentViewPanelHelper.styleViewLink(locked, false);
                buttons.add(locked);
            }

            JButton viewBtn = createButton("View");
            viewBtn.setEnabled(hasDocumentReference(documentIndex));
            EmployeeDocumentViewPanelHelper.styleViewLink(viewBtn, hasDocumentReference(documentIndex));
            buttons.add(viewBtn);
            panel.add(buttons, EmployeeDocumentViewPanelHelper.actionCellConstraints());

            return panel;
        }

        private JButton createButton(String text) {
            JButton btn = EmployeeDocumentViewPanelHelper.createActionLink(text);
            btn.addActionListener(e -> {
                if ("Upload".equals(text) || "Replace".equals(text)) {
                    chooseFile(documentIndex);
                } else if ("View".equals(text)) {
                    viewFile(documentIndex);
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

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(130, 140, 150));
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, 0, y);
            g2.dispose();
        }
    }
}
