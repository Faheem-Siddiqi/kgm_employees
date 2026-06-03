package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseWheelListener;

public final class UniversalTablePanelHelper {
    private UniversalTablePanelHelper() {
    }

    public static DefaultTableCellRenderer actionRenderer(String text) {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                styleActionLabel(label, isSelected, text);
                return label;
            }
        };
    }

    public static DefaultTableCellRenderer alignmentRenderer(int alignment) {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                stylePlainTableCell(label, table, isSelected, alignment);
                return label;
            }
        };
    }

    public static JPanel createStatusPanel(JTable table, boolean isSelected) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, table.getRowHeight()));
        panel.setBackground(isSelected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
        panel.setBorder(tableCellBorder(9, 16, 9, 14));
        return panel;
    }

    public static JLabel createStatusLabel(String status) {
        JLabel label = new StatusBadgeLabel(status, statusColor(status));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setOpaque(false);
        return label;
    }

    public static JLabel createDeleteLabel() {
        JLabel label = new JLabel("Delete");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setForeground(TableThemeHelper.DANGER);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setOpaque(false);
        return label;
    }

    public static JPanel createTableContainer() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(6, 0, 0, 0));
        return container;
    }

    public static JScrollPane createTableScrollPane(
            JTable table,
            boolean horizontalScroll,
            MouseWheelListener wheelListener
    ) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new RoundedTableBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getViewport().setBorder(null);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.addMouseWheelListener(wheelListener);
        scrollPane.getViewport().addMouseWheelListener(wheelListener);
        scrollPane.setHorizontalScrollBarPolicy(horizontalScroll
                ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setEnabled(false);
        scrollPane.setPreferredSize(table.getPreferredScrollableViewportSize());
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(96);
        HomeStatsChartHelper.styleHorizontalScrollBar(scrollPane.getHorizontalScrollBar());
        return scrollPane;
    }

    public static JPanel createPagination() {
        JPanel pagination = new JPanel(new BorderLayout());
        pagination.setOpaque(false);
        pagination.setBorder(new EmptyBorder(2, 0, 0, 0));
        return pagination;
    }

    public static void styleRangeLabel(JLabel label, String text) {
        label.setText(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(TableThemeHelper.TEXT_SECONDARY);
        label.setBorder(new EmptyBorder(8, 0, 8, 0));
    }

    public static JPanel createPagingButtonsPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        return buttons;
    }

    public static void stylePagingButton(JButton button, boolean enabled) {
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new CompoundBorder(
                new RoundedButtonBorder(enabled ? TableThemeHelper.BORDER : new Color(235, 239, 244), 8),
                new EmptyBorder(7, 13, 7, 13)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(Color.WHITE);
        button.setForeground(enabled ? TableThemeHelper.PRIMARY : TableThemeHelper.TEXT_SECONDARY);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        ButtonStateHelper.setEnabledForeground(button, TableThemeHelper.PRIMARY);
        ButtonStateHelper.setEnabled(button, enabled);
    }

    public static void styleClippedTextCell(JLabel label, JTable table) {
        stylePlainTableCell(label, table, false, SwingConstants.LEFT);
        label.setToolTipText(label.getText());
    }

    public static void stylePlainTableCell(JLabel label, JTable table, boolean selected, int alignment) {
        label.setOpaque(true);
        label.setBackground(selected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
        label.setForeground(TableThemeHelper.TEXT_PRIMARY);
        label.setFont(table.getFont().deriveFont(Font.PLAIN));
        label.setEnabled(table.isEnabled());
        label.setComponentOrientation(table.getComponentOrientation());
        label.setBorder(tableCellBorder(0, 16, 0, 14));
        label.setHorizontalAlignment(alignment);
        label.setVerticalAlignment(SwingConstants.CENTER);
    }

    // Predefined professional color palette for status badges
    private static final Color[] STATUS_COLORS = {
            new Color(38, 128, 64),      // Green - for positive statuses
            new Color(0, 112, 210),      // Blue - PRIMARY
            new Color(180, 60, 45),      // Red - DANGER
            new Color(245, 158, 11),     // Amber/Orange
            new Color(139, 92, 246),     // Purple
            new Color(236, 72, 153),     // Pink
            new Color(20, 184, 166),     // Teal
            new Color(249, 115, 22),     // Orange
            new Color(34, 197, 94),      // Lime Green
            new Color(59, 130, 246),     // Sky Blue
            new Color(168, 85, 247),     // Violet
            new Color(244, 63, 94),      // Rose
            new Color(16, 185, 129),     // Emerald
            new Color(251, 146, 60),     // Light Orange
            new Color(14, 165, 233),     // Light Blue
            new Color(217, 119, 6),      // Dark Amber
    };

    public static Color statusColor(String status) {
        if (status == null || status.isEmpty()) {
            return TableThemeHelper.TEXT_SECONDARY;
        }

        String normalized = status.trim().toLowerCase();

        // Handle known statuses with specific colors
        switch (normalized) {
            case "currently staying":
            case "active":
            case "employed":
            case "present":
            case "approved":
            case "completed":
            case "yes":
                return new Color(38, 128, 64); // Green

            case "upcoming":
            case "pending":
            case "in progress":
            case "processing":
            case "scheduled":
                return TableThemeHelper.PRIMARY; // Blue

            case "departed":
            case "inactive":
            case "terminated":
            case "resigned":
            case "rejected":
            case "cancelled":
            case "failed":
            case "no":
            case "expired":
                return TableThemeHelper.DANGER; // Red

            case "on leave":
            case "probation":
            case "notice period":
                return new Color(245, 158, 11); // Amber

            case "remote":
            case "work from home":
                return new Color(139, 92, 246); // Purple

            case "part-time":
            case "contract":
                return new Color(20, 184, 166); // Teal

            case "holiday":
            case "vacation":
                return new Color(249, 115, 22); // Orange
        }

        // For unknown statuses, generate a consistent color based on hash
        return getStatusColorFromHash(status);
    }

    /**
     * Generates a consistent color for a status string based on its hash code.
     * This ensures that the same status always gets the same color.
     */
    private static Color getStatusColorFromHash(String status) {
        if (status == null || status.isEmpty()) {
            return TableThemeHelper.TEXT_SECONDARY;
        }

        int hash = Math.abs(status.toLowerCase().hashCode());
        int index = hash % STATUS_COLORS.length;
        return STATUS_COLORS[index];
    }

    private static void styleActionLabel(JLabel label, boolean selected, String text) {
        label.setText(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(TableThemeHelper.PRIMARY);
        label.setBackground(selected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(tableCellBorder(0, 14, 0, 14));
    }

    private static CompoundBorder tableCellBorder(int top, int left, int bottom, int right) {
        return new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TableThemeHelper.CELL_DIVIDER),
                new EmptyBorder(top, left, bottom, right));
    }

    private static class RoundedTableBorder extends AbstractBorder {
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TableThemeHelper.BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    private static class RoundedButtonBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        RoundedButtonBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
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

    private static class StatusBadgeLabel extends JLabel {
        private final Color accent;

        StatusBadgeLabel(String text, Color accent) {
            super(text);
            this.accent = accent == null ? TableThemeHelper.TEXT_SECONDARY : accent;
            setForeground(this.accent);
            setBorder(new EmptyBorder(3, 9, 4, 9));
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(tint(accent));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(graphics);
        }

        private Color tint(Color color) {
            return new Color(
                    mix(color.getRed()),
                    mix(color.getGreen()),
                    mix(color.getBlue())
            );
        }

        private int mix(int value) {
            return Math.min(255, (int) Math.round(value * 0.12 + 255 * 0.88));
        }
    }
}

