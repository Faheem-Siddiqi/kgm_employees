package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseWheelListener;

public final class UniversalTablePanelHelper {
    private static final Color CELL_DIVIDER = new Color(232, 236, 240);
    private static final Color DISABLED_BUTTON = new Color(225, 225, 225);
    private static final Color DISABLED_TEXT = new Color(145, 145, 145);

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
                label.setHorizontalAlignment(alignment);
                label.setBorder(tableCellBorder());
                return label;
            }
        };
    }

    public static JPanel createStatusPanel(JTable table, boolean isSelected) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, table.getRowHeight()));
        panel.setBackground(isSelected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                new EmptyBorder(12, 16, 0, 14)));
        return panel;
    }

    public static JLabel createStatusLabel(String status) {
        JLabel label = new JLabel(status);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setForeground(statusColor(status));
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setOpaque(false);
        return label;
    }

    public static JLabel createDeleteLabel() {
        JLabel label = new JLabel("Delete");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setForeground(new Color(220, 53, 69));
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
        button.setEnabled(enabled);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        button.setBackground(enabled ? TableThemeHelper.PRIMARY : DISABLED_BUTTON);
        button.setForeground(enabled ? Color.WHITE : DISABLED_TEXT);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
    }

    public static void styleClippedTextCell(JLabel label, JTable table) {
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setForeground(TableThemeHelper.TEXT_PRIMARY);
        label.setFont(table.getFont().deriveFont(Font.PLAIN));
        label.setEnabled(table.isEnabled());
        label.setComponentOrientation(table.getComponentOrientation());
        label.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                new EmptyBorder(0, 16, 0, 14)));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.CENTER);
    }

    public static Color statusColor(String status) {
        if (status.equalsIgnoreCase("Currently Staying")) {
            return new Color(38, 128, 64);
        }
        if (status.equalsIgnoreCase("Upcoming")) {
            return TableThemeHelper.PRIMARY;
        }
        if (status.equalsIgnoreCase("Departed")) {
            return TableThemeHelper.DANGER;
        }
        return TableThemeHelper.TEXT_SECONDARY;
    }

    private static void styleActionLabel(JLabel label, boolean selected, String text) {
        label.setText(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(TableThemeHelper.PRIMARY);
        label.setBackground(selected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                new EmptyBorder(0, 14, 0, 14)));
    }

    private static CompoundBorder tableCellBorder() {
        return new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                new EmptyBorder(0, 14, 0, 14));
    }

    private static class RoundedTableBorder extends AbstractBorder {
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TableThemeHelper.BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, 4, 4);
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }
}

