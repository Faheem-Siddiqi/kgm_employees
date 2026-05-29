package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public final class EmployeeDocumentUploadPanelHelper {
    private static final int SEARCH_CONTROL_HEIGHT = 38;
    private static final int TOP_PANEL_BOTTOM_MARGIN = 10;

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color CARD_BACKGROUND = new Color(248, 250, 252);
    private static final Color ACTION_BLUE = new Color(30, 144, 255);
    private static final Color DISABLED_TEXT = Color.GRAY;
    private static final Color PRIMARY = new Color(0, 112, 210);
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color BORDER = new Color(220, 226, 232);
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
        JPanel panel = new RoundedPanel(CARD_BACKGROUND, 8, TOP_PANEL_BOTTOM_MARGIN);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, TOP_PANEL_BOTTOM_MARGIN, 0),
                new CompoundBorder(
                        new RoundedBorder(8, BORDER),
                        new EmptyBorder(16, 16, 16, 16)
                )
        ));
        return panel;
    }

    public static JLabel createUploadedCountLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JPanel createSummaryPanel(JLabel primaryLabel, JLabel secondaryLabel, JButton actionButton) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        summary.setOpaque(false);
        summary.add(primaryLabel);
        if (secondaryLabel != null) {
            summary.add(createTextDivider());
            summary.add(secondaryLabel);
        }

        row.add(summary, BorderLayout.CENTER);
        if (actionButton != null) {
            row.add(actionButton, BorderLayout.EAST);
        }
        return row;
    }

    public static JPanel createSearchPanel(JTextField searchField, JButton clearButton, JButton actionButton) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(560, SEARCH_CONTROL_HEIGHT));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, SEARCH_CONTROL_HEIGHT));

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(PAGE_BACKGROUND);
        searchBox.setBorder(new CompoundBorder(
                new LineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(0, 10, 0, 4)
        ));
        searchBox.add(searchField, BorderLayout.CENTER);
        searchBox.add(clearButton, BorderLayout.EAST);

        row.add(searchBox, BorderLayout.CENTER);
        if (actionButton != null) {
            row.add(actionButton, BorderLayout.EAST);
        }
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
        button.setForeground(TEXT_SECONDARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setPreferredSize(new Dimension(96, SEARCH_CONTROL_HEIGHT));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
    }

    public static void styleTextCtaButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(TEXT_SECONDARY);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setPreferredSize(new Dimension(124, SEARCH_CONTROL_HEIGHT));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
    }

    public static void styleClearButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(7, 8, 7, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
        updateClearButtonState(button, false);
    }

    public static void updateClearButtonState(JButton button, boolean enabled) {
        ButtonStateHelper.setEnabled(button, enabled);
    }

    public static JLabel createSizeLabel() {
        JLabel label = new JLabel("Maximum file size allowed: 400 KB per file");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
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
        button.setForeground(TEXT_SECONDARY);
        button.setBorder(new EmptyBorder(6, 8, 6, 8));
        ButtonStateHelper.install(button);
        return button;
    }

    public static void styleViewLink(JButton button, boolean uploaded) {
        button.setForeground(TEXT_SECONDARY);
    }

    public static void stylePreviewFrame(JFrame frame, Component relativeTo) {
        AppWindowStateHelper.lockFullSize(frame);
        frame.setLocationRelativeTo(relativeTo);
    }

    private static JLabel createTextDivider() {
        JLabel divider = new JLabel("|");
        divider.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        divider.setForeground(BORDER);
        return divider;
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color color;
        private final int radius;
        private final int bottomGap;

        private RoundedPanel(Color color, int radius, int bottomGap) {
            this.color = color;
            this.radius = radius;
            this.bottomGap = bottomGap;
            setOpaque(false);
        }

        protected void paintComponent(Graphics graphics) {
            int paintedHeight = Math.max(0, getHeight() - bottomGap);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), paintedHeight, radius, radius);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }
}

