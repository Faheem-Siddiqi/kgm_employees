package com.kgm.ui.panel;

import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeDocumentUploadPanelHelper;
import com.kgm.ui.styling.TablePaginationHelper;
import com.kgm.ui.component.FileUploadCard;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmployeeDocumentUploadPanel extends JPanel {
    private static final int ACTION_COLUMN = 3;
    private static final int DOCUMENT_INDEX_COLUMN = 4;

    private JTable table;
    private DefaultTableModel model;
    private File[] files;
    private String[] filePaths;
    private boolean[] requiredDocuments;
    private JTextField searchField;
    private JButton clearSearchButton;
    private JScrollPane documentScrollPane;
    private JLabel uploadedCountLabel;

    public EmployeeDocumentUploadPanel() {
        EmployeeDocumentUploadPanelHelper.stylePanel(this);

        files = new File[EmployeeDocumentUtil.documentCount()];
        filePaths = new String[EmployeeDocumentUtil.documentCount()];
        requiredDocuments = EmployeeDocumentUtil.requiredDocumentFlags();

        JPanel topPanel = EmployeeDocumentUploadPanelHelper.createTopPanel();

        uploadedCountLabel = EmployeeDocumentUploadPanelHelper.createUploadedCountLabel("Total uploads: 0");
        JLabel sizeLabel = EmployeeDocumentUploadPanelHelper.createSizeLabel();
        searchField = new PlaceholderTextField("Search Document Name");
        JButton searchButton = new JButton("Search");
        clearSearchButton = new JButton("Clear");
        JButton uploadAllButton = new JButton("Upload All");

        EmployeeDocumentUploadPanelHelper.styleSearchField(searchField);
        EmployeeDocumentUploadPanelHelper.styleSearchButton(searchButton);
        EmployeeDocumentUploadPanelHelper.styleClearButton(clearSearchButton);
        EmployeeDocumentUploadPanelHelper.styleTextCtaButton(uploadAllButton);

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
                EmployeeDocumentUploadPanelHelper.updateClearButtonState(
                        clearSearchButton,
                        !searchField.getText().trim().isEmpty()
                );
                refreshDocumentRows();
            }
        });

        topPanel.add(uploadedCountLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(sizeLabel);
        topPanel.add(Box.createVerticalStrut(12));
        topPanel.add(EmployeeDocumentUploadPanelHelper.createSearchPanel(searchField, clearSearchButton, searchButton));
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(EmployeeDocumentUploadPanelHelper.createBulkActionPanel(uploadAllButton));
        topPanel.add(Box.createVerticalStrut(12));
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Document", "File", "Status", "Actions", "DocumentIndex"};
        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == ACTION_COLUMN;
            }
        };

        refreshDocumentRows();

        table = TablePaginationHelper.createDocumentTable(model);
        hideDocumentIndexColumn();
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellEditor(new ActionEditor());

        documentScrollPane = TablePaginationHelper.createScrollPane(table, false);
        showFullTableWithoutScroll(documentScrollPane);
        add(documentScrollPane, BorderLayout.CENTER);
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
        File file = files[documentIndex];
        boolean required = isRequiredDocument(documentIndex);
        String fileText = file == null ? "-" : file.getName();
        String statusText = file == null
                ? required ? "Missing required" : "Not Uploaded"
                : "Uploaded (" + EmployeeDocumentUtil.formatSize(file.length()) + ")";
        model.addRow(new Object[]{
                documentLabel(documentIndex),
                fileText,
                statusText,
                "",
                documentIndex
        });
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
        File file = files[documentIndex];
        if (file != null) {
            searchableValues.add(file.getName());
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

    private String documentLabel(int documentIndex) {
        String label = EmployeeDocumentUtil.documentType(documentIndex).label();
        return isRequiredDocument(documentIndex) ? label + " *" : label;
    }

    private boolean isRequiredDocument(int documentIndex) {
        return requiredDocuments != null
                && documentIndex >= 0
                && documentIndex < requiredDocuments.length
                && requiredDocuments[documentIndex];
    }

    private void updateCount() {
        int count = 0;
        for (File file : files) {
            if (file != null) {
                count++;
            }
        }
        uploadedCountLabel.setText("Total uploads: " + count);
    }

    private void chooseFile(int documentIndex) {
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
            model.setValueAt("Uploaded (" + EmployeeDocumentUtil.formatSize(file.length()) + ")", modelRow, 2);
        }

        updateCount();
        model.fireTableDataChanged();
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

        EmployeeDocumentUtil.BulkUploadResult summary = EmployeeDocumentUtil.matchBulkFiles(selectedFiles, null);
        for (EmployeeDocumentUtil.BulkUploadItem item : summary.uploadedDocuments()) {
            files[item.documentIndex()] = item.file();
            filePaths[item.documentIndex()] = item.file().getAbsolutePath();
        }

        refreshDocumentRows();
        updateCount();
        model.fireTableDataChanged();
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
        if (files[documentIndex] == null) {
            return;
        }

        try {
            BufferedImage img = ImageIO.read(files[documentIndex]);
            if (img == null) {
                DialogHelper.error(this, "Cannot Open File", "Cannot open file.");
                return;
            }

            JFrame frame = new JFrame("Document Preview - " + files[documentIndex].getName());
            JScrollPane previewScroll = new JScrollPane(new DocumentImagePreviewPanel(img));
            previewScroll.setBorder(null);
            previewScroll.getViewport().setBackground(Color.WHITE);
            frame.getContentPane().add(previewScroll);
            EmployeeDocumentUploadPanelHelper.stylePreviewFrame(frame, this);
            frame.setVisible(true);

        } catch (Exception e) {
            DialogHelper.error(this, "Cannot Open File", "Cannot open file.");
        }
    }

    class ActionRenderer extends JPanel implements TableCellRenderer {
        public ActionRenderer() {
            EmployeeDocumentUploadPanelHelper.styleRendererPanel(this);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            removeAll();
            EmployeeDocumentUploadPanelHelper.styleActionCell(this, isSelected);

            int modelRow = table.convertRowIndexToModel(row);
            String status = (String) table.getModel().getValueAt(modelRow, 2);
            boolean uploaded = status.startsWith("Uploaded");

            JPanel buttons = EmployeeDocumentUploadPanelHelper.createActionButtonsPanel();
            buttons.add(createLink(uploaded ? "Replace" : "Upload"));

            JButton viewBtn = createLink("View");
            EmployeeDocumentUploadPanelHelper.styleViewLink(viewBtn, uploaded);
            buttons.add(viewBtn);
            add(buttons, EmployeeDocumentUploadPanelHelper.actionCellConstraints());

            return this;
        }

        private JButton createLink(String text) {
            return EmployeeDocumentUploadPanelHelper.createActionLink(text);
        }
    }

    class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int documentIndex;

        public ActionEditor() {
            panel = EmployeeDocumentUploadPanelHelper.createEditorPanel();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {

            int modelRow = table.convertRowIndexToModel(row);
            this.documentIndex = documentIndexForModelRow(modelRow);
            panel.removeAll();
            EmployeeDocumentUploadPanelHelper.styleActionCell(panel, isSelected);

            String status = (String) table.getModel().getValueAt(modelRow, 2);
            boolean uploaded = status.startsWith("Uploaded");

            JPanel buttons = EmployeeDocumentUploadPanelHelper.createActionButtonsPanel();
            buttons.add(createButton(uploaded ? "Replace" : "Upload"));

            JButton viewBtn = createButton("View");
            viewBtn.setEnabled(uploaded);
            EmployeeDocumentUploadPanelHelper.styleViewLink(viewBtn, uploaded);
            buttons.add(viewBtn);
            panel.add(buttons, EmployeeDocumentUploadPanelHelper.actionCellConstraints());

            return panel;
        }

        private JButton createButton(String text) {
            JButton btn = EmployeeDocumentUploadPanelHelper.createActionLink(text);

            btn.addActionListener(e -> {
                if ("Upload".equals(text) || "Replace".equals(text)) {
                    chooseFile(documentIndex);
                } else {
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

    public String getDocumentPath(int index) {
        return (filePaths != null && index < filePaths.length)
                ? filePaths[index]
                : null;
    }

    public String[] getAllDocumentPaths() {
        return filePaths;
    }

    public List<String> missingRequiredDocumentLabels() {
        return EmployeeDocumentUtil.missingRequiredDocumentLabels(filePaths);
    }

    public void reloadDocumentRequirements() {
        requiredDocuments = EmployeeDocumentUtil.requiredDocumentFlags();
        refreshDocumentRows();
    }

    public void clearDocuments() {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        Arrays.fill(files, null);
        Arrays.fill(filePaths, null);
        searchField.setText("");
        EmployeeDocumentUploadPanelHelper.updateClearButtonState(clearSearchButton, false);
        refreshDocumentRows();
        updateCount();

        if (table != null) {
            table.clearSelection();
        }

        revalidate();
        repaint();
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
