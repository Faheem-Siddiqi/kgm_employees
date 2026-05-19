package com.kgm.ui.panel;

import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.DocumentPanelHelper;
import com.kgm.ui.styling.UniversalTablePagination;

import java.io.File;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocumentPanel extends JPanel {
    private static final int ACTION_COLUMN = 3;
    private static final int DOCUMENT_INDEX_COLUMN = 4;

    private JTable table;
    private DefaultTableModel model;
    private File[] files;
    private JTextField searchField;
    private JButton clearSearchButton;

    // ✅ NEW: store actual file paths for DB
    private String[] filePaths;

    private static final long MAX_SIZE = 400 * 1024; // 400 KB

    private JLabel uploadedCountLabel;

    private final String[] documents = {
            "CNIC *", "EOBI Card *", "SS_CARD_COPY*", "Final Settlement",
            "Clearance Certificate", "Job Appointment Letter", "Application Letter",
            "Insurance Form", "Settlement Document", "Trial Card",
            "Interview Form", "Service Letter", "Extension Letter",
            "Retirement Letter", "Covid Certification", "DISCIPLINARY_I", "DISCIPLINARY_II", "DISCIPLINARY_III"
    };

    public DocumentPanel() {
        DocumentPanelHelper.stylePanel(this);

        files = new File[documents.length];

        // ✅ NEW INIT
        filePaths = new String[documents.length];

        // ================= TOP TEXT =================
        JPanel topPanel = DocumentPanelHelper.createTopPanel();

        uploadedCountLabel = DocumentPanelHelper.createUploadedCountLabel("Total Uploades: 0");

        JLabel sizeLabel = DocumentPanelHelper.createSizeLabel();
        searchField = new PlaceholderTextField("Search Document Name");
        JButton searchButton = new JButton("Search");
        clearSearchButton = new JButton("Clear");
        DocumentPanelHelper.styleSearchField(searchField);
        DocumentPanelHelper.styleSearchButton(searchButton);
        DocumentPanelHelper.styleClearButton(clearSearchButton);

        searchButton.addActionListener(e -> refreshDocumentRows());
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
                DocumentPanelHelper.updateClearButtonState(
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
        topPanel.add(DocumentPanelHelper.createSearchPanel(searchField, clearSearchButton, searchButton));
        topPanel.add(Box.createVerticalStrut(12));

        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Document", "File", "Status", "Actions", "DocumentIndex"};

        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == ACTION_COLUMN;
            }
        };

        refreshDocumentRows();

        table = UniversalTablePagination.createDocumentTable(model);
        hideDocumentIndexColumn();

        table.getColumnModel().getColumn(ACTION_COLUMN).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellEditor(new ActionEditor());

        JScrollPane scrollPane = UniversalTablePagination.createScrollPane(table, false);
        showFullTableWithoutScroll(scrollPane);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void showFullTableWithoutScroll(JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        table.setFillsViewportHeight(false);

        int headerHeight = table.getTableHeader() == null ? 0 : table.getTableHeader().getPreferredSize().height;
        int tableHeight = headerHeight + (table.getRowHeight() * table.getRowCount()) + 2;
        Dimension preferredSize = new Dimension(scrollPane.getPreferredSize().width, tableHeight);
        scrollPane.setPreferredSize(preferredSize);
        scrollPane.setMinimumSize(new Dimension(320, tableHeight));
    }

    private void refreshDocumentRows() {
        if (model == null) {
            return;
        }
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        String query = normalizedSearch(searchField == null ? "" : searchField.getText());
        List<Integer> orderedIndexes = new ArrayList<>();
        for (int index = 0; index < documents.length; index++) {
            orderedIndexes.add(index);
        }

        if (!query.isEmpty()) {
            orderedIndexes.sort((left, right) -> {
                int score = Integer.compare(matchScore(left, query), matchScore(right, query));
                return score != 0 ? score : Integer.compare(left, right);
            });
        }

        model.setRowCount(0);
        for (Integer documentIndex : orderedIndexes) {
            addDocumentRow(documentIndex);
        }
    }

    private void addDocumentRow(int documentIndex) {
        File file = files[documentIndex];
        String fileText = file == null ? "-" : file.getName();
        String statusText = file == null ? "Not Uploaded" : "Uploaded (" + formatSize(file.length()) + ")";
        model.addRow(new Object[]{
                documents[documentIndex],
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
        String documentName = normalizedSearch(documents[documentIndex]);
        if (documentName.startsWith(query)) {
            return 0;
        }
        if (documentName.contains(query)) {
            return 1;
        }
        return 2;
    }

    private String normalizedSearch(String value) {
        return value == null
                ? ""
                : value.replace("*", "").trim().toLowerCase();
    }

    // ================= FILE HELPERS =================
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    private void updateCount() {
        int count = 0;
        for (File f : files) if (f != null) count++;
        uploadedCountLabel.setText("Total Uploades: " + count);
    }

    // ================= FILE UPLOAD =================
    private void chooseFile(int documentIndex) {
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

            files[documentIndex] = file;

            // ✅ STORE PATH FOR DB
            filePaths[documentIndex] = file.getAbsolutePath();

            int modelRow = findModelRowByDocumentIndex(documentIndex);
            if (modelRow >= 0) {
                model.setValueAt(file.getName(), modelRow, 1);
                model.setValueAt("Uploaded (" + formatSize(file.length()) + ")", modelRow, 2);
            }

            updateCount();
            model.fireTableDataChanged();
        }
    }

    // ================= VIEW FILE =================
    private void viewFile(int documentIndex) {
        if (files[documentIndex] == null) return;

        try {
            BufferedImage img = ImageIO.read(files[documentIndex]);
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

            JFrame frame = new JFrame("Document Preview - " + files[documentIndex].getName());
            frame.getContentPane().add(new JScrollPane(previewPanel));
            DocumentPanelHelper.stylePreviewFrame(frame, this);
            frame.setVisible(true);

        } catch (Exception e) {
            DialogHelper.error(this, "Cannot Open File", "Cannot open file.");
        }
    }

    // ================= ACTION RENDERER =================
    class ActionRenderer extends JPanel implements TableCellRenderer {

        public ActionRenderer() {
            DocumentPanelHelper.styleRendererPanel(this);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            removeAll();
            DocumentPanelHelper.styleActionCell(this, isSelected);

            int modelRow = table.convertRowIndexToModel(row);
            String status = (String) table.getModel().getValueAt(modelRow, 2);
            boolean uploaded = status.startsWith("Uploaded");

            JPanel buttons = DocumentPanelHelper.createActionButtonsPanel();
            JButton uploadBtn = createLink(uploaded ? "Replace" : "Upload");
            buttons.add(uploadBtn);

            JButton viewBtn = createLink("View");
            DocumentPanelHelper.styleViewLink(viewBtn, uploaded);
            buttons.add(viewBtn);
            add(buttons, DocumentPanelHelper.actionCellConstraints());

            return this;
        }

        private JButton createLink(String text) {
            return DocumentPanelHelper.createActionLink(text);
        }
    }

    // ================= ACTION EDITOR =================
    class ActionEditor extends AbstractCellEditor implements TableCellEditor {

        private JPanel panel;
        private int documentIndex;

        public ActionEditor() {
            panel = DocumentPanelHelper.createEditorPanel();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {

            int modelRow = table.convertRowIndexToModel(row);
            this.documentIndex = documentIndexForModelRow(modelRow);
            panel.removeAll();
            DocumentPanelHelper.styleActionCell(panel, isSelected);

            String status = (String) table.getModel().getValueAt(modelRow, 2);
            boolean uploaded = status.startsWith("Uploaded");

            JPanel buttons = DocumentPanelHelper.createActionButtonsPanel();
            buttons.add(createButton(uploaded ? "Replace" : "Upload"));

            JButton viewBtn = createButton("View");
            viewBtn.setEnabled(uploaded);
            DocumentPanelHelper.styleViewLink(viewBtn, uploaded);
            buttons.add(viewBtn);
            panel.add(buttons, DocumentPanelHelper.actionCellConstraints());

            return panel;
        }

        private JButton createButton(String text) {
            JButton btn = DocumentPanelHelper.createActionLink(text);

            btn.addActionListener(e -> {
                if (text.equals("Upload") || text.equals("Replace")) {
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

    // ================= NEW: GET PATHS FOR PARENT =================
    // CUSTOMIZE FILE PATH TO STORE DOCUMENTS
    public String getDocumentPath(int index) {
        return (filePaths != null && index < filePaths.length)
                ? filePaths[index]
                : null;
    }

    public String[] getAllDocumentPaths() {
        return filePaths;
    }

    public void clearDocuments() {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        Arrays.fill(files, null);
        Arrays.fill(filePaths, null);
        searchField.setText("");
        DocumentPanelHelper.updateClearButtonState(clearSearchButton, false);
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
