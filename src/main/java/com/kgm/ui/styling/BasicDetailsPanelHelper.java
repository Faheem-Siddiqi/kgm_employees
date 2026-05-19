package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class BasicDetailsPanelHelper {
    public static final int PHOTO_SIZE = 200;
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PHOTO_BORDER = new Color(210, 210, 210);
    private static final Color FORM_BORDER = new Color(235, 235, 235);
    private static final Color LABEL_TEXT = new Color(70, 70, 70);
    private static final Color LINK_BLUE = new Color(0, 102, 204);

    private BasicDetailsPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
    }

    public static JPanel createFormRoot() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(PAGE_BACKGROUND);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        return root;
    }

    public static JPanel createPhotoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(240, 300));
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JLabel createPhotoPreview(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(220, 220));
        label.setBorder(BorderFactory.createLineBorder(PHOTO_BORDER));
        return label;
    }

    public static void styleUploadLabel(JLabel label) {
        label.setForeground(LINK_BLUE);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static JPanel createPhotoInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PAGE_BACKGROUND);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    public static JLabel createPhotoInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return label;
    }

    public static JPanel createRightFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PAGE_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FORM_BORDER),
                new EmptyBorder(20, 20, 20, 20)));
        return panel;
    }

    public static void styleTextArea(JTextArea textArea) {
        textArea.setFont(INPUT_FONT);
        textArea.setBorder(BorderFactory.createLineBorder(PHOTO_BORDER));
    }

    public static void styleFormField(JPanel panel) {
        panel.setLayout(new BorderLayout(6, 4));
        panel.setBackground(PAGE_BACKGROUND);
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(LABEL_TEXT);
        return label;
    }

    public static void styleInput(JComponent input) {
        input.setFont(INPUT_FONT);
        if (!(input instanceof JTextArea)) {
            input.setPreferredSize(new Dimension(240, 34));
        }
    }
}
