package com.kgm.ui.panel;

import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.UniversalTablePagination;

import java.io.File;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class DocumentPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private File[] files;

    // ✅ NEW: store actual file paths for DB
    private String[] filePaths;

    private static final long MAX_SIZE = 400 * 1024; // 400 KB

    private JLabel uploadedCountLabel;

    private final String[] documents = {
            "CNIC *", "EOBI Card *", "SS_CARD_COPY*", "Final Settlement",
            "Clearance Certificate", "Job Appointment Letter", "Application Letter",
            "Insurance Form", "Settlement Document", "Trial Card",
            "Interview Form", "Service Letter", "Extension Letter",
            "Retirement Letter", "Covid Certification", "DISCIPLINARY_I","DISCIPLINARY_II","DISCIPLINARY_III"
    };

    public DocumentPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        files = new File[documents.length];

        // ✅ NEW INIT
        filePaths = new String[documents.length];

        // ================= TOP TEXT =================
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        uploadedCountLabel = new JLabel("Total fields uploaded: 0");
        uploadedCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel sizeLabel = new JLabel(
                "<html>Maximum file size allowed is: <b>400KB</b></html>"
        );
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

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

        for (String doc : documents) {
            model.addRow(new Object[]{doc, "-", "Not Uploaded", "Upload"});
        }

        table = UniversalTablePagination.createDocumentTable(model);

        table.getColumnModel().getColumn(3).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new ActionEditor());

        JScrollPane scrollPane = UniversalTablePagination.createScrollPane(table, false);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ================= FILE HELPERS =================
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    private String trimFileName(String name) {
        if (name == null) return "-";
        if (name.length() <= 16) return name;

        int dot = name.lastIndexOf(".");
        String ext = (dot != -1) ? name.substring(dot) : "";
        String base = (dot != -1) ? name.substring(0, dot) : "";

        if (base.length() > 10) {
            base = base.substring(0, 10) + "..";
        }

        return base + ext;
    }

    private void updateCount() {
        int count = 0;
        for (File f : files) if (f != null) count++;
        uploadedCountLabel.setText("Total fields uploaded: " + count);
    }

    // ================= FILE UPLOAD =================
    private void chooseFile(int row) {
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

            // ✅ STORE PATH FOR DB
            filePaths[row] = file.getAbsolutePath();

            model.setValueAt(trimFileName(file.getName()), row, 1);
            model.setValueAt("Uploaded (" + formatSize(file.length()) + ")", row, 2);

            updateCount();
            model.fireTableDataChanged();
        }
    }

    // ================= VIEW FILE =================
    private void viewFile(int row) {
        if (files[row] == null) return;

        try {
            BufferedImage img = ImageIO.read(files[row]);

            Image scaled = img.getScaledInstance(800, 600, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(scaled));

            JFrame frame = new JFrame("Document Preview");
            frame.getContentPane().add(new JScrollPane(label));
            frame.setSize(850, 650);
            frame.setLocationRelativeTo(this);
            frame.setVisible(true);

        } catch (Exception e) {
            DialogHelper.error(this, "Cannot Open File", "Cannot open file.");
        }
    }

    // ================= ACTION RENDERER =================
    class ActionRenderer extends JPanel implements TableCellRenderer {

public ActionRenderer() {
    setLayout(new GridBagLayout());
    setOpaque(false);
}
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            removeAll();

            String status = (String) table.getModel().getValueAt(row, 2);
            boolean uploaded = status.startsWith("Uploaded");

            JButton uploadBtn = createLink(uploaded ? "Replace" : "Upload");
            add(uploadBtn);

            JButton viewBtn = createLink("View");
            viewBtn.setForeground(uploaded ? Color.BLACK : Color.GRAY);
            add(viewBtn);

            return this;
        }

        private JButton createLink(String text) {
            JButton btn = new JButton(text);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setForeground(new Color(30, 144, 255));
            return btn;
        }
    }

    // ================= ACTION EDITOR =================
    class ActionEditor extends AbstractCellEditor implements TableCellEditor {

        private JPanel panel;
        private int row;

        public ActionEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setOpaque(false);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {

            this.row = row;
            panel.removeAll();

            String status = (String) table.getModel().getValueAt(row, 2);
            boolean uploaded = status.startsWith("Uploaded");

            panel.add(createButton(uploaded ? "Replace" : "Upload"));

            JButton viewBtn = createButton("View");
            viewBtn.setEnabled(uploaded);
            panel.add(viewBtn);

            return panel;
        }

        private JButton createButton(String text) {
            JButton btn = new JButton(text);

            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {
                if (text.equals("Upload") || text.equals("Replace")) {
                    chooseFile(row);
                } else {
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

    // ================= NEW: GET PATHS FOR PARENT =================
    public String getDocumentPath(int index) {
        return (filePaths != null && index < filePaths.length)
                ? filePaths[index]
                : null;
    }

    public String[] getAllDocumentPaths() {
        return filePaths;
    }
}
