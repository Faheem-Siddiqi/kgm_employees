package com.kgm.ui.component;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileUploadCard extends JPanel {
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color BACKGROUND_HOVER = new Color(241, 247, 253);
    private static final Color BACKGROUND_DISABLED = new Color(244, 247, 250);
    private static final Color BORDER = new Color(198, 212, 226);
    private static final Color ACTION = new Color(0, 112, 210);
    private static final Color ACTION_DISABLED = new Color(71, 85, 105);
    private static final Color TEXT = new Color(35, 43, 54);
    private static final Color MUTED = new Color(99, 115, 129);
    private static final Color DISABLED_TEXT = new Color(91, 103, 116);

    private final JLabel statusLabel = new JLabel("");
    private final JLabel titleLabel = new JLabel();
    private final JLabel hintLabel = new JLabel();
    private final JLabel actionLabel = new JLabel();
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

        titleLabel.setText(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        hintLabel.setText(hint);
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        actionLabel.setText(actionText);
        actionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        applyStateColors();

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
        applyStateColors();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isEnabled() ? hover ? BACKGROUND_HOVER : BACKGROUND : BACKGROUND_DISABLED);
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

    private void applyStateColors() {
        boolean enabled = isEnabled();
        titleLabel.setForeground(enabled ? TEXT : DISABLED_TEXT);
        hintLabel.setForeground(enabled ? MUTED : DISABLED_TEXT);
        statusLabel.setForeground(enabled ? MUTED : DISABLED_TEXT);
        actionLabel.setForeground(enabled ? ACTION : ACTION_DISABLED);
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
        return NativeFileDialog.chooseFile(parent, title, filter);
    }

    public static File[] chooseFiles(Component parent, String title, FileFilterSpec filter) {
        return NativeFileDialog.chooseFiles(parent, title, filter);
    }

    public static File[] chooseDirectories(Component parent, String title) {
        return NativeFileDialog.chooseDirectories(parent, title);
    }

    public static File chooseSaveFile(Component parent, String title, String suggestedFileName, FileFilterSpec filter) {
        return NativeFileDialog.chooseSaveFile(parent, title, suggestedFileName, filter);
    }

    public static File chooseDirectory(Component parent, String title, String suggestedDirectoryName) {
        return NativeFileDialog.chooseDirectory(parent, title, suggestedDirectoryName);
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
