package com.kgm.ui.styling;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.function.IntConsumer;

public final class TablePaginationHelper {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BORDER = new Color(220, 226, 232);
    private static final Color CELL_DIVIDER = new Color(232, 236, 240);
    private static final Color ROW_SELECTION = new Color(229, 242, 255);
    private static final Color PRIMARY = new Color(0, 112, 210);
    private static final Color DISABLED_BUTTON = new Color(225, 225, 225);
    private static final Color DISABLED_TEXT = new Color(145, 145, 145);

    private TablePaginationHelper() {
    }

    public static JTable createEmployeeTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        applyBaseTableStyle(table);
        applyEmployeeRenderers(table);
        return table;
    }

    public static JTable createDocumentTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        applyBaseTableStyle(table);
        applyDocumentRenderers(table);
        return table;
    }

    public static JTable createDocumentViewTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        applyBaseTableStyle(table);
        applyCenteredDocumentRenderers(table);
        return table;
    }

    public static JScrollPane createScrollPane(JTable table, boolean horizontalScroll) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new RoundedTableBorder());
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        scrollPane.getViewport().setBorder(null);
        if (horizontalScroll) {
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        } else {
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        }
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(96);
        return scrollPane;
    }

    public static JPanel createPaginationButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static JLabel createShowingLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        label.setBorder(new EmptyBorder(8, 0, 8, 0));
        return label;
    }

    public static JPanel createPaginationContainer(JLabel showingLabel, JPanel paginationPanel) {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
        bottom.add(showingLabel, BorderLayout.WEST);
        bottom.add(paginationPanel, BorderLayout.EAST);
        return bottom;
    }

    public static void updateShowingLabel(JLabel label, int currentPage, int rowsPerPage, int totalRecords) {
        int start = ((currentPage - 1) * rowsPerPage) + 1;
        int end = Math.min(currentPage * rowsPerPage, totalRecords);
        if (totalRecords == 0) {
            label.setText("Showing 0 /  0");
        } else {
            label.setText("Showing " + start + " - " + end + " /  " + totalRecords);
        }
    }

    public static void showSingleRecord(JLabel label, JPanel paginationPanel) {
        label.setText("Showing 1 / 1");
        clearPagination(paginationPanel);
    }

    public static void clearPagination(JPanel paginationPanel) {
        paginationPanel.removeAll();
        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    public static void buildPagination(
            JPanel paginationPanel,
            int totalRecords,
            int rowsPerPage,
            int currentPage,
            IntConsumer onPageSelected) {

        paginationPanel.removeAll();
        int totalPages = Math.max(1, (int) Math.ceil(totalRecords / (double) rowsPerPage));
        JButton previousButton = createPaginationButton("Previous", currentPage > 1);
        JButton nextButton = createPaginationButton("Next", currentPage < totalPages);

        previousButton.addActionListener(e -> onPageSelected.accept(Math.max(1, currentPage - 1)));
        nextButton.addActionListener(e -> onPageSelected.accept(Math.min(totalPages, currentPage + 1)));

        paginationPanel.add(previousButton);
        paginationPanel.add(nextButton);
        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    public static void autoResizeColumns(JTable table) {
        for (int col = 0; col < table.getColumnCount(); col++) {
            int width = 60;
            for (int row = 0; row < table.getRowCount(); row++) {
                Component comp = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                width = Math.max(comp.getPreferredSize().width + 20, width);
            }
            JTableHeader header = table.getTableHeader();
            Component headerComp = header.getDefaultRenderer().getTableCellRendererComponent(
                    table,
                    table.getColumnName(col),
                    false,
                    false,
                    0,
                    col);
            width = Math.max(width, headerComp.getPreferredSize().width + 20);
            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }
    }

    private static void applyBaseTableStyle(JTable table) {
        table.setRowHeight(44);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(PAGE_BACKGROUND);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ROW_SELECTION);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setDefaultEditor(Object.class, null);
        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFocusable(false);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setBorder(new LineBorder(new Color(190, 204, 218)));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBackground(PRIMARY);
                label.setForeground(Color.WHITE);
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, Color.WHITE),
                        new EmptyBorder(0, 14, 0, 14)));
                label.setOpaque(true);
                return label;
            }
        });
    }

    private static void applyEmployeeRenderers(JTable table) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                boolean actionColumn = "Action".equalsIgnoreCase(table.getColumnName(column));
                label.setHorizontalAlignment(actionColumn ? SwingConstants.CENTER : SwingConstants.LEFT);
                label.setBackground(isSelected ? ROW_SELECTION : PAGE_BACKGROUND);
                label.setForeground(actionColumn ? PRIMARY : TEXT_PRIMARY);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                        new EmptyBorder(0, 16, 0, 14)));
                return label;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private static void applyDocumentRenderers(JTable table) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(isSelected ? ROW_SELECTION : PAGE_BACKGROUND);
                label.setForeground(TEXT_PRIMARY);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                        new EmptyBorder(0, 16, 0, 14)));

                if (column == 1 || column == 2) {
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                }

                if (column == 0 && value != null && value.toString().contains("*")) {
                    label.setText("<html>" + value.toString().replace("*",
                            "<font color='red'>*</font>") + "</html>");
                }

                return label;
            }
        };
        for (int i = 0; i < Math.min(3, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private static void applyCenteredDocumentRenderers(JTable table) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(isSelected ? ROW_SELECTION : PAGE_BACKGROUND);
                label.setForeground(TEXT_PRIMARY);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                        new EmptyBorder(0, 16, 0, 14)));

                if (column == 0 && value != null && value.toString().contains("*")) {
                    label.setText("<html>" + value.toString().replace("*",
                            "<font color='red'>*</font>") + "</html>");
                }

                return label;
            }
        };
        for (int i = 0; i < Math.min(3, table.getColumnCount()); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private static JButton createPaginationButton(String text, boolean enabled) {
        JButton button = new JButton(text);
        button.setEnabled(enabled);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        button.setBackground(enabled ? PRIMARY : DISABLED_BUTTON);
        button.setForeground(enabled ? Color.WHITE : DISABLED_TEXT);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        return button;
    }

    public static class RoundedTableBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, 4, 4);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }
}

