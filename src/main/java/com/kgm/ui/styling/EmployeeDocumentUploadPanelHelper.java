package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public final class EmployeeDocumentUploadPanelHelper {
    private static final int SEARCH_BOX_WIDTH = 340;
    private static final int SEARCH_ROW_WIDTH = 455;
    private static final int SEARCH_CONTROL_HEIGHT = 36;

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color ACTION_BLUE = new Color(30, 144, 255);
    private static final Color DISABLED_TEXT = Color.GRAY;
    private static final Color PRIMARY = new Color(0, 112, 210);
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color CELL_DIVIDER = new Color(232, 236, 240);
    private static final Color ROW_SELECTION = new Color(229, 242, 255);

    private EmployeeDocumentUploadPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        panel.setOpaque(true);
        panel.setBorder(new EmptyBorder(14, 28, 15, 28));
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

    public static JPanel createSearchPanel(JTextField searchField, JButton clearButton, JButton searchButton) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(PAGE_BACKGROUND);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(SEARCH_ROW_WIDTH, SEARCH_CONTROL_HEIGHT));
        row.setMaximumSize(new Dimension(SEARCH_ROW_WIDTH, SEARCH_CONTROL_HEIGHT));

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(PAGE_BACKGROUND);
        Dimension searchBoxSize = new Dimension(SEARCH_BOX_WIDTH, SEARCH_CONTROL_HEIGHT);
        searchBox.setPreferredSize(searchBoxSize);
        searchBox.setMinimumSize(searchBoxSize);
        searchBox.setMaximumSize(searchBoxSize);
        searchBox.setBorder(new CompoundBorder(
                new LineBorder(FIELD_BORDER),
                new EmptyBorder(0, 10, 0, 4)
        ));
        searchBox.add(searchField, BorderLayout.CENTER);
        searchBox.add(clearButton, BorderLayout.EAST);

        row.add(searchBox);
        row.add(searchButton);
        return row;
    }

    public static JPanel createBulkActionPanel(JButton uploadAllButton) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBackground(PAGE_BACKGROUND);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.add(uploadAllButton);
        return row;
    }

    public static void styleSearchField(JTextField field) {
        field.setBorder(null);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(PAGE_BACKGROUND);
        field.setPreferredSize(new Dimension(240, 34));
    }

    public static void styleSearchButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleTextCtaButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(ACTION_BLUE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(6, 0, 6, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleClearButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(7, 8, 7, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateClearButtonState(button, false);
    }

    public static void updateClearButtonState(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setForeground(enabled ? ACTION_BLUE : TEXT_SECONDARY);
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public static JLabel createSizeLabel() {
        JLabel label = new JLabel("<html>Maximum file size allowed is: <b>400KB</b></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    public static JPanel createRendererPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        return panel;
    }

    public static void styleRendererPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());
        panel.setOpaque(true);
    }

    public static JPanel createEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        return panel;
    }

    public static void styleActionCell(JPanel panel, boolean selected) {
        panel.setBackground(selected ? ROW_SELECTION : PAGE_BACKGROUND);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                new EmptyBorder(0, 4, 0, 4)
        ));
    }

    public static JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static GridBagConstraints actionCellConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        return gbc;
    }

    public static JButton createActionLink(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(ACTION_BLUE);
        button.setBorder(new EmptyBorder(6, 8, 6, 8));
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

