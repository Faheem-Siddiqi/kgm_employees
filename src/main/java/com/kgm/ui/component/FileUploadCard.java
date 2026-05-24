package com.kgm.ui.component;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileUploadCard extends JPanel {
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color BACKGROUND_HOVER = new Color(241, 247, 253);
    private static final Color BORDER = new Color(198, 212, 226);
    private static final Color ACTION = new Color(0, 112, 210);
    private static final Color TEXT = new Color(35, 43, 54);
    private static final Color MUTED = new Color(99, 115, 129);

    private final JLabel statusLabel = new JLabel("");
    private final List<ActionListener> listeners = new ArrayList<>();
    private boolean hover;

    public FileUploadCard(String title, String hint, String actionText) {
        setLayout(new BorderLayout(8, 4));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(TEXT);

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLabel.setForeground(MUTED);

        JLabel actionLabel = new JLabel(actionText);
        actionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        actionLabel.setForeground(ACTION);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(hintLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(statusLabel);

        add(text, BorderLayout.CENTER);
        add(actionLabel, BorderLayout.EAST);

        java.awt.event.MouseAdapter clickHandler = new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent event) {
                hover = true;
                repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent event) {
                hover = false;
                repaint();
            }

            public void mouseClicked(java.awt.event.MouseEvent event) {
                fireAction();
            }
        };
        installClickHandler(this, clickHandler);
    }

    public void setStatus(String status) {
        statusLabel.setText(status == null || status.isBlank() ? "" : status.trim());
    }

    public void addActionListener(ActionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        setChildrenEnabled(this, enabled);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isEnabled() && hover ? BACKGROUND_HOVER : BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();
        super.paintComponent(graphics);
    }

    private void fireAction() {
        if (!isEnabled()) {
            return;
        }
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "choose-file");
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }

    private void installClickHandler(Component component, java.awt.event.MouseAdapter clickHandler) {
        component.addMouseListener(clickHandler);
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installClickHandler(child, clickHandler);
            }
        }
    }

    private void setChildrenEnabled(Container container, boolean enabled) {
        for (Component child : container.getComponents()) {
            child.setEnabled(enabled);
            child.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            if (child instanceof Container childContainer) {
                setChildrenEnabled(childContainer, enabled);
            }
        }
    }

    public static FileFilterSpec jpegImages() {
        return new FileFilterSpec("JPEG Images (*.jpg, *.jpeg)", "jpg", "jpeg");
    }

    public static FileFilterSpec excelWorkbooks() {
        return new FileFilterSpec("Excel files (*.xlsx, *.xls)", "xlsx", "xls");
    }

    public static FileFilterSpec xlsxWorkbook() {
        return new FileFilterSpec("Excel workbook (*.xlsx)", "xlsx");
    }

    public static File chooseFile(Component parent, String title, FileFilterSpec filter) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.LOAD);
        applyFilter(dialog, filter);
        dialog.setVisible(true);
        return selectedFile(dialog);
    }

    public static File[] chooseFiles(Component parent, String title, FileFilterSpec filter) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.LOAD);
        dialog.setMultipleMode(true);
        applyFilter(dialog, filter);
        dialog.setVisible(true);
        File[] files = dialog.getFiles();
        return files == null ? new File[0] : files;
    }

    public static File chooseSaveFile(Component parent, String title, String suggestedFileName, FileFilterSpec filter) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.SAVE);
        if (suggestedFileName != null && !suggestedFileName.isBlank()) {
            dialog.setFile(suggestedFileName);
        }
        applyFilter(dialog, filter);
        dialog.setVisible(true);
        return selectedFile(dialog);
    }

    public static File chooseDirectory(Component parent, String title, String approveButtonText) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setApproveButtonText(approveButtonText);
        return chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    private static FileDialog nativeFileDialog(Component parent, String title, int mode) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner instanceof Frame frame) {
            return new FileDialog(frame, title, mode);
        }
        if (owner instanceof Dialog dialog) {
            return new FileDialog(dialog, title, mode);
        }
        return new FileDialog((Frame) null, title, mode);
    }

    private static void applyFilter(FileDialog dialog, FileFilterSpec filter) {
        if (filter == null || filter.extensions().length == 0) {
            return;
        }
        FilenameFilter filenameFilter = (directory, name) -> hasAllowedExtension(name, filter.extensions());
        dialog.setFilenameFilter(filenameFilter);
        if (dialog.getMode() == FileDialog.LOAD) {
            dialog.setFile(glob(filter.extensions()));
        }
    }

    private static File selectedFile(FileDialog dialog) {
        String fileName = dialog.getFile();
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String directory = dialog.getDirectory();
        return directory == null ? new File(fileName) : new File(directory, fileName);
    }

    private static boolean hasAllowedExtension(String fileName, String[] extensions) {
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (normalized.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    private static String glob(String[] extensions) {
        StringBuilder pattern = new StringBuilder();
        for (String extension : extensions) {
            if (pattern.length() > 0) {
                pattern.append(';');
            }
            pattern.append("*.").append(extension);
        }
        return pattern.toString();
    }

    private static String[] cleanExtensions(String[] rawExtensions) {
        if (rawExtensions == null) {
            return new String[0];
        }
        return Arrays.stream(rawExtensions)
                .filter(extension -> extension != null && !extension.isBlank())
                .map(extension -> extension.replace(".", "").toLowerCase(Locale.ROOT).trim())
                .distinct()
                .toArray(String[]::new);
    }

    public record FileFilterSpec(String description, String... extensions) {
        public FileFilterSpec {
            extensions = cleanExtensions(extensions);
        }
    }
}
