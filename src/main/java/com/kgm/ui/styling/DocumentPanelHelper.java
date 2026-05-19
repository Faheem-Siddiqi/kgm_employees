package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;

public final class DocumentPanelHelper {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color ACTION_BLUE = new Color(30, 144, 255);
    private static final Color DISABLED_TEXT = Color.GRAY;

    private DocumentPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
    }

    public static JPanel createTopPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PAGE_BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    public static JLabel createUploadedCountLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    public static JLabel createSizeLabel() {
        JLabel label = new JLabel("<html>Maximum file size allowed is: <b>400KB</b></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    public static JPanel createRendererPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    public static void styleRendererPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());
        panel.setOpaque(false);
    }

    public static JPanel createEditorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static JButton createActionLink(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(ACTION_BLUE);
        return button;
    }

    public static void styleViewLink(JButton button, boolean uploaded) {
        button.setForeground(uploaded ? Color.BLACK : DISABLED_TEXT);
    }

    public static void stylePreviewFrame(JFrame frame, Component relativeTo) {
        frame.setSize(850, 650);
        frame.setLocationRelativeTo(relativeTo);
    }
}
