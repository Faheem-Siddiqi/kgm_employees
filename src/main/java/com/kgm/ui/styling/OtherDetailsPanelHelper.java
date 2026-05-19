package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class OtherDetailsPanelHelper {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color SECTION_BORDER = new Color(230, 230, 230);
    private static final Color FIELD_BORDER = new Color(220, 220, 220);
    private static final Color HEADER_TEXT = new Color(60, 60, 60);

    private OtherDetailsPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    public static JPanel createRootPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(PAGE_BACKGROUND);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        return root;
    }

    public static JPanel createSectionPanel() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PAGE_BACKGROUND);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SECTION_BORDER),
                new EmptyBorder(12, 12, 12, 12)));
        return section;
    }

    public static JLabel createSectionHeader(String title) {
        JLabel header = new JLabel(title);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(HEADER_TEXT);
        return header;
    }

    public static JPanel createGridPanel() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(PAGE_BACKGROUND);
        return grid;
    }

    public static JPanel createFieldPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 3));
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    public static JTextField createField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(PAGE_BACKGROUND);
        field.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        field.setPreferredSize(new Dimension(200, 30));
        return field;
    }

    public static JPanel createGridFiller() {
        JPanel panel = new JPanel();
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }
}
