package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class HomeViewHelper {
    private static final int SEARCH_CONTROL_WIDTH = 560;
    private static final int SEARCH_CONTROL_HEIGHT = 36;

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY = new Color(0, 112, 210);
    private static final Color ACTION_BLUE = new Color(30, 144, 255);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BORDER = new Color(220, 226, 232);
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);

    private HomeViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("Home");
        AppWindowStateHelper.lockFullSize(frame);
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
        wrapper.setBorder(new EmptyBorder(24, 28, 0, 28));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(PAGE_BACKGROUND);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(SEARCH_CONTROL_WIDTH, SEARCH_CONTROL_HEIGHT));
        row.setMaximumSize(new Dimension(SEARCH_CONTROL_WIDTH, SEARCH_CONTROL_HEIGHT));

        JPanel rowWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowWrapper.setOpaque(false);
        rowWrapper.add(row);

        card.putClientProperty("searchControls", row);
        card.add(rowWrapper, BorderLayout.CENTER);
        wrapper.putClientProperty("searchControls", row);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    public static JTextField createSearchField(String placeholder) {
        return new PlaceholderTextField(placeholder);
    }

    public static void addSearchControls(JPanel searchRow, JTextField field, JButton searchButton, JButton clearButton) {
        Object controls = searchRow.getClientProperty("searchControls");
        if (controls instanceof JPanel row) {
            styleSearchField(field);

            JPanel searchBox = new JPanel(new BorderLayout(6, 0));
            searchBox.setBackground(PAGE_BACKGROUND);
            searchBox.setPreferredSize(new Dimension(430, SEARCH_CONTROL_HEIGHT));
            searchBox.setBorder(new CompoundBorder(
                    new LineBorder(FIELD_BORDER),
                    new EmptyBorder(0, 10, 0, 4)
            ));
            searchBox.add(field, BorderLayout.CENTER);
            searchBox.add(clearButton, BorderLayout.EAST);

            row.add(searchBox, BorderLayout.CENTER);
            row.add(searchButton, BorderLayout.EAST);
        }
    }

    public static void styleSearchButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
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
    }

    public static JPanel createNorthContainer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel createButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        row.setBorder(BorderFactory.createEmptyBorder(20, 28, 10, 28));
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
        body.setBorder(BorderFactory.createEmptyBorder(10, 28, 0, 28));
        return body;
    }

    public static JPanel createMainContentPanel() {
        JPanel panel = new HomeContentPanel();
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JScrollPane createMainScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    private static void styleBaseButton(JButton button, Dimension size, int fontStyle) {
        button.setPreferredSize(size);
        button.setFont(new Font("Segoe UI", fontStyle, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static void setTextButtonEnabled(JButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setForeground(enabled ? ACTION_BLUE : TEXT_SECONDARY);
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private static void styleSearchField(JTextField field) {
        field.setBorder(null);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(PAGE_BACKGROUND);
        field.setPreferredSize(new Dimension(260, 34));
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

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(130, 140, 150));
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, 0, y);
            g2.dispose();
        }
    }

    private static class HomeContentPanel extends JPanel implements Scrollable {
        private HomeContentPanel() {
            super(new BorderLayout());
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(80, visibleRect.height - 80);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
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

