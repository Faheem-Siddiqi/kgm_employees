package com.kgm.ui.panel;

import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.UniversalTablePagination;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;

public class DocumentPanelView extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    private String[] documents = {
            "CNIC *", "EOBI Card *", "SS_CARD_COPY*", "Final Settlement",
            "Clearance Certificate", "Job Appointment Letter", "Application Letter",
            "Issuance Form", "Settlement Document", "Trial Card",
            "Interview Form", "Service Letter", "Extension Letter",
            "Retirement Letter", "Covid Certification",
            "DISCIPLINARY_I", "DISCIPLINARY_II", "DISCIPLINARY_III"
    };

    // injected from parent (DB / service layer)
    private String[] filePaths;

    public DocumentPanelView() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        buildUI();
    }

    // ================= UI =================
    private void buildUI() {

        String[] columns = {"Document", "File", "Status", "Actions"};

        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }
        };

        for (String doc : documents) {
            model.addRow(new Object[]{doc, "-", "Not Loaded", "Action"});
        }

        table = UniversalTablePagination.createDocumentViewTable(model);

        table.getColumnModel().getColumn(3).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new ActionEditor());

        add(UniversalTablePagination.createScrollPane(table, false), BorderLayout.CENTER);
    }

    // ================= DATA INJECTION FROM PARENT =================
    public void setFilePaths(String[] paths) {
        this.filePaths = paths;

        if (paths == null) return;

        for (int i = 0; i < paths.length && i < model.getRowCount(); i++) {
            if (paths[i] != null) {

                File f = new File(paths[i]);

                model.setValueAt(f.getName(), i, 1);
                model.setValueAt("Available", i, 2);
            }
        }
    }

    // ================= ACTION RENDERER =================
    class ActionRenderer extends JPanel implements TableCellRenderer {

        public ActionRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            removeAll();

            String status = table.getValueAt(row, 2).toString();
            boolean available = status.equals("Available");

            JButton viewBtn = createBtn("View", available);
            JButton downloadBtn = createBtn("Download", available);

            add(viewBtn);
            add(downloadBtn);

            return this;
        }

        private JButton createBtn(String text, boolean enabled) {
            JButton btn = new JButton(text);
            btn.setEnabled(enabled);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setForeground(enabled ? new Color(0, 120, 215) : Color.GRAY);
            return btn;
        }
    }

    // ================= ACTION EDITOR =================
    class ActionEditor extends AbstractCellEditor implements TableCellEditor {

        private JPanel panel;
        private int row;

        public ActionEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            panel.setOpaque(false);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int col) {

            this.row = row;
            panel.removeAll();

            String path = (filePaths != null && row < filePaths.length)
                    ? filePaths[row]
                    : null;

            boolean available = path != null;

            JButton viewBtn = createBtn("View", available);
            JButton downloadBtn = createBtn("Download", available);

            panel.add(viewBtn);
            panel.add(downloadBtn);

            return panel;
        }

        private JButton createBtn(String text, boolean enabled) {
            JButton btn = new JButton(text);
            btn.setEnabled(enabled);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {

                String path = filePaths[row];

                if (path == null) return;

                if (text.equals("View")) {
                    DialogHelper.info(DocumentPanelView.this, "Document Preview", "Open Preview:\n" + path);
                }

                if (text.equals("Download")) {
                    DialogHelper.info(DocumentPanelView.this, "Download File", "Download File:\n" + path);
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
