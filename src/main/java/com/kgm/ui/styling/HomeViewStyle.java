package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class HomeViewStyle {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY = new Color(0, 112, 210);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BORDER = new Color(220, 226, 232);
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);

    private HomeViewStyle() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("Home");
        frame.setSize(1100, 650);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    public static JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel createSearchRow() {
        JPanel card = sectionCard("Employee Filters", "Search and narrow employee records quickly.");
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(24, 24, 0, 24));

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);

        JLabel label = label("Employee Code");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setOpaque(false);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label);
        row.add(Box.createVerticalStrut(6));
        row.add(controls);
        card.putClientProperty("searchControls", controls);
        card.add(row, BorderLayout.CENTER);
        wrapper.putClientProperty("searchControls", controls);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    public static void addSearchControls(JPanel searchRow, JTextField field, JButton searchButton, JButton clearButton) {
        Object controls = searchRow.getClientProperty("searchControls");
        if (controls instanceof JPanel row) {
            row.add(styleField(field, 320));
            row.add(Box.createHorizontalStrut(10));
            row.add(searchButton);
            row.add(Box.createHorizontalStrut(10));
            row.add(clearButton);
        }
    }

    public static void styleSearchButton(JButton button) {
        styleTextButton(button);
    }

    public static void styleClearButton(JButton button) {
        styleTextButton(button);
    }

    public static JPanel createNorthContainer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel createButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        row.setBorder(BorderFactory.createEmptyBorder(20, 15, 10, 15));
        row.setOpaque(false);
        return row;
    }

    public static void styleAddButton(JButton button) {
        styleBaseButton(button, new Dimension(120, 32), Font.PLAIN);
        button.setBackground(PAGE_BACKGROUND);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LIGHT_BORDER),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    }

    public static void styleRefreshButton(JButton button) {
        styleBaseButton(button, new Dimension(100, 32), Font.BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 38, 77));
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    public static JPanel createBodyPanel() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(PAGE_BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(10, 25, 0, 25));
        return body;
    }

    private static void styleBaseButton(JButton button, Dimension size, int fontStyle) {
        button.setPreferredSize(size);
        button.setFont(new Font("Segoe UI", fontStyle, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static void setTextButtonEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setForeground(enabled ? PRIMARY : new Color(155, 155, 155));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private static JPanel sectionCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(PAGE_BACKGROUND);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(16, BORDER),
                new EmptyBorder(20, 20, 20, 20)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitleLabel);
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(new Color(70, 82, 96));
        return label;
    }

    private static JComponent styleField(JComponent component, int width) {
        component.setPreferredSize(new Dimension(width, 34));
        component.setMinimumSize(new Dimension(Math.min(width, 150), 34));
        component.setMaximumSize(new Dimension(Math.max(width, 260), 34));
        component.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        component.setBackground(PAGE_BACKGROUND);
        component.setBorder(new CompoundBorder(
                new LineBorder(FIELD_BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        return component;
    }

    private static void styleTextButton(JButton button) {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(PRIMARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 10, 8, 10));
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }
}
