package com.kgm.ui.panel;

import com.kgm.ui.styling.TableThemeHelper;
import com.kgm.ui.styling.UniversalTablePanelHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UniversalTablePanel extends JPanel {
    private static final int PAGE_SIZE = 12;
    private static final int MIN_VIEWPORT_HEIGHT = 118;

    private final JTable table;
    private final DefaultTableModel model;
    private final List<Object[]> rows = new ArrayList<>();
    private final Set<Integer> hugColumns = new HashSet<>();
    private final Set<Integer> nonSelectingColumns = new HashSet<>();
    private final Set<Integer> wrappedTextColumns = new HashSet<>();
    private final Map<Integer, Integer> preferredWidthLimits = new HashMap<>();
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel rangeLabel = new JLabel();
    private final JButton previousButton = new JButton("Previous");
    private final JButton nextButton = new JButton("Next");
    private final String emptyText;
    private int actionColumn = -1;
    private Consumer<Integer> onAction;
    private Consumer<Integer> statusDeleteAction;
    private java.util.function.Predicate<Integer> statusDeletePredicate;
    private int statusColumn = -1;
    private int linkColumn = -1;
    private int checkboxColumn = -1;
    private Consumer<Integer> onLink;
    private Consumer<Integer> onCheckboxToggle;
    private java.util.function.Predicate<Integer> checkboxSelectedPredicate;
    private boolean linkHighlightOnlyOnHover = false;
    private int hoveredRow = -1;
    private boolean hugRows = true;
    private boolean paginationEnabled = true;
    private int paginationBottomGap = 0;
    private int minimumViewportRows = 0;
    private boolean emptyStateEnabled = true;
    private int currentPage = 0;

    public UniversalTablePanel(String[] columns, String emptyText) {
        this.emptyText = emptyText;
        this.model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(model) {
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                if (nonSelectingColumns.contains(columnIndex)) {
                    clearSelection();
                    return;
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };

        setLayout(new BorderLayout());
        setOpaque(false);
        content.setOpaque(false);

        TableThemeHelper.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().addMouseWheelListener(this::forwardMouseWheel);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (isCheckboxCell(row, column) && onCheckboxToggle != null) {
                    onCheckboxToggle.accept(toAbsoluteRow(row));
                    return;
                }

                if (isLinkCell(row, column) && onLink != null) {
                    onLink.accept(toAbsoluteRow(row));
                    return;
                }

                if (actionColumn < 0 || onAction == null) {
                    return;
                }

                if (row < 0 || column != actionColumn) {
                    return;
                }

                onAction.accept(toAbsoluteRow(row));
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                updateHoveredLink(row, column);
                boolean hoveringLink = isLinkCell(row, column);
                boolean hoveringCheckbox = isCheckboxCell(row, column) && onCheckboxToggle != null;
                boolean hoveringAction = actionColumn >= 0 && row >= 0 && column == actionColumn;
                table.setCursor(Cursor.getPredefinedCursor(
                        hoveringLink || hoveringCheckbox || hoveringAction ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
                ));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent event) {
                updateHoveredLink(-1, -1);
                table.setCursor(Cursor.getDefaultCursor());
            }
        });
        table.addMouseWheelListener(this::forwardMouseWheel);

        previousButton.addActionListener(e -> goToPage(currentPage - 1));
        nextButton.addActionListener(e -> goToPage(currentPage + 1));
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                if (!rows.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        renderPage();
                        revalidate();
                        repaint();
                    });
                }
            }
        });

        add(content, BorderLayout.CENTER);
        refresh();
    }

    public void setActionColumn(int column, String text, Consumer<Integer> onAction) {
        this.actionColumn = column;
        this.onAction = onAction;
        table.getColumnModel().getColumn(column).setCellRenderer(UniversalTablePanelHelper.actionRenderer(text));
        configureColumnWidths();
    }

    public void setColumnAlignment(int column, int alignment) {
        table.getColumnModel().getColumn(column).setCellRenderer(UniversalTablePanelHelper.alignmentRenderer(alignment));
    }

    public void setCheckboxColumn(int column, java.util.function.Predicate<Integer> selectedPredicate) {
        setCheckboxColumn(column, selectedPredicate, null);
    }

    public void setCheckboxColumn(
            int column,
            java.util.function.Predicate<Integer> selectedPredicate,
            Consumer<Integer> onToggle
    ) {
        this.checkboxColumn = column;
        this.checkboxSelectedPredicate = selectedPredicate;
        this.onCheckboxToggle = onToggle;
        nonSelectingColumns.add(column);
        table.getColumnModel().getColumn(column).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(true);
            panel.setBackground(isSelected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(232, 236, 240)));

            JCheckBox checkbox = new JCheckBox();
            checkbox.setOpaque(false);
            checkbox.setFocusable(false);
            checkbox.setEnabled(onToggle != null);
            int absoluteRow = toAbsoluteRow(row);
            boolean checked = selectedPredicate != null && selectedPredicate.test(absoluteRow);
            checkbox.setSelected(checked);
            panel.add(checkbox);
            return panel;
        });
        configureColumnWidths();
    }

    public void setHugColumn(int column) {
        hugColumns.add(column);
        configureColumnWidths();
    }

    public void setPreferredColumnWidthLimit(int column, int maxWidth) {
        preferredWidthLimits.put(column, Math.max(72, maxWidth));
        configureColumnWidths();
    }

    public void setClippedTextColumn(int column) {
        nonSelectingColumns.add(column);
        table.getColumnModel().getColumn(column).setCellRenderer(new ClippedTextCellRenderer());
    }

    public void setWrappedTextColumn(int column) {
        nonSelectingColumns.add(column);
        wrappedTextColumns.add(column);
        table.getColumnModel().getColumn(column).setCellRenderer(new WrappedTextCellRenderer());
        configureColumnWidths();
    }

    public void setLinkColumn(int column, Consumer<Integer> onLink) {
        setLinkColumn(column, onLink, false);
    }

    public void setLinkColumn(int column, Consumer<Integer> onLink, boolean highlightOnlyOnHover) {
        this.linkColumn = column;
        this.onLink = onLink;
        this.linkHighlightOnlyOnHover = highlightOnlyOnHover;
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                String text = value == null ? "" : String.valueOf(value);
                TableThemeHelper.styleTableLink(
                        label,
                        table,
                        isSelected,
                        row == hoveredRow,
                        text,
                        linkHighlightOnlyOnHover
                );
                return label;
            }
        };
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        configureColumnWidths();
    }

    public void setStatusColumn(int column) {
        setStatusColumn(column, null, null);
    }

    public void setStatusColumn(int column, Consumer<Integer> onDeleteAction) {
        setStatusColumn(column, onDeleteAction, null);
    }

    public void setStatusColumn(int column, Consumer<Integer> onDeleteAction, java.util.function.Predicate<Integer> showDeleteFor) {
        this.statusColumn = column;
        this.statusDeleteAction = onDeleteAction;
        this.statusDeletePredicate = showDeleteFor;

        if (table.getRowHeight() < 40) {
            table.setRowHeight(40);
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                JPanel panel = UniversalTablePanelHelper.createStatusPanel(table, isSelected);

                String status = value == null ? "" : String.valueOf(value);
                JLabel statusLabel = UniversalTablePanelHelper.createStatusLabel(status);
                panel.add(statusLabel);

                if (onDeleteAction != null && showDeleteFor != null) {
                    int modelRow = toAbsoluteRow(row);
                    if (showDeleteFor.test(modelRow)) {
                        panel.add(UniversalTablePanelHelper.createDeleteLabel());
                    }
                }
                return panel;
            }
        };
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        configureColumnWidths();

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (statusDeleteAction == null || statusDeletePredicate == null) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                int viewColumn = table.columnAtPoint(e.getPoint());

                if (viewRow < 0 || viewColumn < 0) {
                    return;
                }

                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                if (modelColumn == statusColumn) {
                    int modelRow = toAbsoluteRow(viewRow);
                    if (statusDeletePredicate.test(modelRow)) {
                        statusDeleteAction.accept(modelRow);
                    }
                }
            }
        });

        table.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewColumn = table.columnAtPoint(e.getPoint());

                if (viewRow >= 0 && viewColumn >= 0 && statusDeletePredicate != null) {
                    int modelColumn = table.convertColumnIndexToModel(viewColumn);
                    if (modelColumn == statusColumn) {
                        int modelRow = toAbsoluteRow(viewRow);
                        if (statusDeletePredicate.test(modelRow)) {
                            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                }

                updateHoveredLink(viewRow, viewColumn);
                boolean hoveringLink = isLinkCell(viewRow, viewColumn);
                boolean hoveringCheckbox = isCheckboxCell(viewRow, viewColumn) && onCheckboxToggle != null;
                boolean hoveringAction = actionColumn >= 0 && viewRow >= 0 && viewColumn == actionColumn;
                table.setCursor(Cursor.getPredefinedCursor(
                        hoveringLink || hoveringCheckbox || hoveringAction ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
                ));
            }
        });
    }

    public void setHugRows(boolean hugRows) {
        this.hugRows = hugRows;
        refresh();
    }

    public void setPaginationEnabled(boolean paginationEnabled) {
        this.paginationEnabled = paginationEnabled;
        currentPage = 0;
        refresh();
    }

    public void setPaginationBottomGap(int paginationBottomGap) {
        this.paginationBottomGap = Math.max(0, paginationBottomGap);
        refresh();
    }

    public void setMinimumViewportRows(int minimumViewportRows) {
        this.minimumViewportRows = Math.max(0, minimumViewportRows);
        refresh();
    }

    public void setEmptyStateEnabled(boolean emptyStateEnabled) {
        this.emptyStateEnabled = emptyStateEnabled;
        refresh();
    }

    public void addRow(Object[] row) {
        rows.add(row);
        currentPage = lastPage();
        refresh();
    }

    public void setRows(List<Object[]> newRows) {
        rows.clear();
        rows.addAll(newRows);
        currentPage = 0;
        refresh();
    }

    public void clearRows() {
        rows.clear();
        currentPage = 0;
        refresh();
    }

    public void updateRow(int row, Object[] values) {
        rows.set(row, values);
        refresh();
    }

    public void removeRow(int row) {
        rows.remove(row);
        currentPage = Math.min(currentPage, lastPage());
        refresh();
    }

    public int getRowCount() {
        return rows.size();
    }

    public Object getValueAt(int row, int column) {
        return rows.get(row)[column];
    }

    public void clearSelection() {
        table.clearSelection();
    }

    public int getRenderedTableWidth() {
        return table.getPreferredScrollableViewportSize().width;
    }

    private void refresh() {
        content.removeAll();
        if (rows.isEmpty() && emptyStateEnabled) {
            content.add(TableThemeHelper.emptyState(emptyText), BorderLayout.NORTH);
        } else {
            renderPage();
            content.add(createTableContainer(), BorderLayout.NORTH);
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel createTableContainer() {
        JPanel container = UniversalTablePanelHelper.createTableContainer();

        JScrollPane scrollPane = UniversalTablePanelHelper.createTableScrollPane(
                table,
                shouldShowHorizontalScroll(),
                this::forwardMouseWheel);

        container.add(scrollPane, BorderLayout.CENTER);
        if (paginationEnabled) {
            container.add(createPagination(), BorderLayout.SOUTH);
        }
        return container;
    }

    private void forwardMouseWheel(MouseWheelEvent event) {
        if (event.isShiftDown() && scrollInnerTableHorizontally(event)) {
            return;
        }

        JScrollPane pageScroll = findPageScrollPane();
        if (pageScroll == null) {
            return;
        }

        MouseWheelEvent pageEvent = new MouseWheelEvent(
                pageScroll,
                event.getID(),
                event.getWhen(),
                event.getModifiersEx(),
                0,
                0,
                event.getXOnScreen(),
                event.getYOnScreen(),
                event.getClickCount(),
                event.isPopupTrigger(),
                event.getScrollType(),
                event.getScrollAmount(),
                event.getWheelRotation(),
                event.getPreciseWheelRotation()
        );
        pageScroll.dispatchEvent(pageEvent);
        event.consume();
    }

    private JScrollPane findPageScrollPane() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof JScrollPane) {
                return (JScrollPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private boolean scrollInnerTableHorizontally(MouseWheelEvent event) {
        Component source = event.getComponent();
        JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, source);
        if (tableScroll == null || !SwingUtilities.isDescendingFrom(tableScroll, this)) {
            return false;
        }

        JScrollBar horizontalBar = tableScroll.getHorizontalScrollBar();
        if (horizontalBar == null || !horizontalBar.isVisible()) {
            return false;
        }

        scrollBar(horizontalBar, event);
        event.consume();
        return true;
    }

    private void scrollBar(JScrollBar scrollBar, MouseWheelEvent event) {
        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount = event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
                ? event.getUnitsToScroll() * scrollBar.getUnitIncrement(direction)
                : event.getWheelRotation() * scrollBar.getBlockIncrement(direction);
        int max = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
        int value = Math.max(scrollBar.getMinimum(), Math.min(max, scrollBar.getValue() + amount));
        scrollBar.setValue(value);
    }

    private JPanel createPagination() {
        JPanel pagination = UniversalTablePanelHelper.createPagination();
        pagination.setBorder(BorderFactory.createEmptyBorder(2, 0, paginationBottomGap, 0));

        UniversalTablePanelHelper.styleRangeLabel(rangeLabel, showingText());

        JPanel buttons = UniversalTablePanelHelper.createPagingButtonsPanel();
        UniversalTablePanelHelper.stylePagingButton(previousButton, currentPage > 0);
        UniversalTablePanelHelper.stylePagingButton(nextButton, currentPage < lastPage());
        buttons.add(previousButton);
        buttons.add(nextButton);

        pagination.add(rangeLabel, BorderLayout.WEST);
        pagination.add(buttons, BorderLayout.EAST);
        return pagination;
    }

    private void renderPage() {
        model.setRowCount(0);
        int start = paginationEnabled ? currentPage * PAGE_SIZE : 0;
        int end = paginationEnabled ? Math.min(start + PAGE_SIZE, rows.size()) : rows.size();
        for (int index = start; index < end; index++) {
            model.addRow(rows.get(index));
        }
        configureColumnWidths();
        applyWrappedRowHeights();
        if (hugRows) {
            int headerHeight = table.getTableHeader().getPreferredSize().height;
            int horizontalScrollbarHeight = preferredTableWidth() > availableTableWidth()
                    ? UIManager.getInt("ScrollBar.width")
                    : 0;
            int minimumRowsHeight = minimumViewportRows * table.getRowHeight();
            int contentHeight = headerHeight
                    + Math.max(renderedRowsHeight(), minimumRowsHeight)
                    + horizontalScrollbarHeight;
            int height = Math.max(MIN_VIEWPORT_HEIGHT, contentHeight);
            table.setPreferredScrollableViewportSize(new Dimension(availableTableWidth(), height));
        }
    }

    private void applyWrappedRowHeights() {
        int defaultHeight = table.getRowHeight();
        for (int row = 0; row < model.getRowCount(); row++) {
            int height = defaultHeight;
            for (Integer column : wrappedTextColumns) {
                Object value = model.getValueAt(row, column);
                int columnWidth = table.getColumnModel().getColumn(column).getPreferredWidth();
                height = Math.max(height, wrappedTextHeight(value == null ? "" : String.valueOf(value), columnWidth));
            }
            table.setRowHeight(row, height);
        }
    }

    private int renderedRowsHeight() {
        if (model.getRowCount() == 0) {
            return table.getRowHeight();
        }

        int height = 0;
        for (int row = 0; row < model.getRowCount(); row++) {
            height += table.getRowHeight(row);
        }
        return height;
    }

    private void configureColumnWidths() {
        int columns = table.getColumnCount();
        int availableWidth = availableTableWidth();
        int totalWidth = 0;

        for (int column = 0; column < columns; column++) {
            int width = column == actionColumn ? 80 : measuredColumnWidth(column);
            Integer widthLimit = preferredWidthLimits.get(column);
            if (widthLimit != null) {
                width = Math.min(width, widthLimit);
            }
            table.getColumnModel().getColumn(column).setPreferredWidth(width);
            totalWidth += width;
        }

        int stretchColumns = 0;
        for (int column = 0; column < columns; column++) {
            if (!hugColumns.contains(column)) {
                stretchColumns++;
            }
        }

        if (totalWidth < availableWidth && stretchColumns > 0) {
            int extra = availableWidth - totalWidth;
            int baseExtra = extra / stretchColumns;
            int remainder = extra % stretchColumns;
            int stretchIndex = 0;
            for (int column = 0; column < columns; column++) {
                if (hugColumns.contains(column)) {
                    continue;
                }
                int currentWidth = table.getColumnModel().getColumn(column).getPreferredWidth();
                int addedWidth = baseExtra + (stretchIndex < remainder ? 1 : 0);
                table.getColumnModel().getColumn(column).setPreferredWidth(currentWidth + addedWidth);
                stretchIndex++;
            }
            totalWidth = availableWidth;
        }

        Dimension size = table.getPreferredScrollableViewportSize();
        table.setPreferredScrollableViewportSize(new Dimension(
                availableWidth,
                size.height
        ));
    }

    private int measuredColumnWidth(int column) {
        int padding = 34;
        FontMetrics headerMetrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        FontMetrics cellMetrics = table.getFontMetrics(table.getFont());
        int width = headerMetrics.stringWidth(table.getColumnName(column)) + padding;

        for (Object[] row : rows) {
            Object value = row[column];
            int cellWidth = cellMetrics.stringWidth(value == null ? "" : String.valueOf(value)) + padding;
            if (wrappedTextColumns.contains(column)) {
                cellWidth = Math.min(cellWidth, Math.max(260, availableTableWidth() / 2));
            }

            if (column == statusColumn && statusDeleteAction != null && statusDeletePredicate != null) {
                int deleteWidth = cellMetrics.stringWidth("Delete") + 12;
                cellWidth += deleteWidth;
            }

            width = Math.max(width, cellWidth);
        }

        return Math.max(72, width);
    }

    private int wrappedTextHeight(String text, int columnWidth) {
        FontMetrics metrics = table.getFontMetrics(table.getFont());
        int innerWidth = Math.max(48, columnWidth - 34);
        int lineCount = 0;
        for (String paragraph : text.split("\\R", -1)) {
            lineCount += wrappedLineCount(paragraph, innerWidth, metrics);
        }
        return Math.max(table.getRowHeight(), lineCount * metrics.getHeight() + 22);
    }

    private int wrappedLineCount(String text, int innerWidth, FontMetrics metrics) {
        if (text == null || text.isBlank()) {
            return 1;
        }

        int lines = 1;
        int currentWidth = 0;
        int spaceWidth = metrics.stringWidth(" ");
        for (String word : text.trim().split("\\s+")) {
            int wordWidth = metrics.stringWidth(word);
            if (wordWidth > innerWidth) {
                lines += Math.max(0, (int) Math.ceil(wordWidth / (double) innerWidth) - 1);
                currentWidth = wordWidth % innerWidth;
                continue;
            }
            int nextWidth = currentWidth == 0 ? wordWidth : currentWidth + spaceWidth + wordWidth;
            if (nextWidth > innerWidth) {
                lines++;
                currentWidth = wordWidth;
            } else {
                currentWidth = nextWidth;
            }
        }
        return lines;
    }

    private int preferredTableWidth() {
        int width = 0;
        for (int column = 0; column < table.getColumnCount(); column++) {
            width += table.getColumnModel().getColumn(column).getPreferredWidth();
        }
        return Math.max(width, 1);
    }

    private boolean shouldShowHorizontalScroll() {
        return preferredTableWidth() > availableTableWidth();
    }

    private int availableTableWidth() {
        int width = getWidth();
        if (width <= 0 && getParent() != null) {
            width = getParent().getWidth();
        }
        return Math.max(240, width > 0 ? width : TableThemeHelper.CONTENT_WIDTH - 80);
    }

    private String showingText() {
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, rows.size());
        if (rows.isEmpty()) {
            return "Showing 0 / 0";
        }
        return "Showing " + (start + 1) + "-" + end + " / " + rows.size();
    }

    private int toAbsoluteRow(int pageRow) {
        int offset = paginationEnabled ? currentPage * PAGE_SIZE : 0;
        return offset + table.convertRowIndexToModel(pageRow);
    }

    private boolean isLinkCell(int viewRow, int viewColumn) {
        return linkColumn >= 0
                && onLink != null
                && viewRow >= 0
                && viewColumn >= 0
                && table.convertColumnIndexToModel(viewColumn) == linkColumn;
    }

    private boolean isCheckboxCell(int viewRow, int viewColumn) {
        return checkboxColumn >= 0
                && viewRow >= 0
                && viewColumn >= 0
                && table.convertColumnIndexToModel(viewColumn) == checkboxColumn;
    }

    private void updateHoveredLink(int viewRow, int viewColumn) {
        int nextHoveredRow = canHighlightLinkedRow(viewRow) ? viewRow : -1;
        if (hoveredRow == nextHoveredRow) {
            return;
        }
        hoveredRow = nextHoveredRow;
        table.repaint();
    }

    private boolean canHighlightLinkedRow(int viewRow) {
        return linkColumn >= 0
                && onLink != null
                && viewRow >= 0;
    }

    private void goToPage(int page) {
        currentPage = Math.max(0, Math.min(page, lastPage()));
        refresh();
    }

    private int lastPage() {
        if (rows.isEmpty()) {
            return 0;
        }
        return paginationEnabled ? (rows.size() - 1) / PAGE_SIZE : 0;
    }

    private static class ClippedTextCellRenderer extends JLabel implements TableCellRenderer {
        private String text = "";

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            text = value == null ? "" : String.valueOf(value);
            applyPlainTableStyle(table);
            setText(text);
            setToolTipText(text.isBlank() ? null : text);
            return this;
        }

        private void applyPlainTableStyle(JTable table) {
            UniversalTablePanelHelper.styleClippedTextCell(this, table);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            if (isOpaque()) {
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setFont(getFont());
            g2.setColor(getForeground());
            Insets insets = getInsets();
            FontMetrics metrics = g2.getFontMetrics();
            int x = insets.left;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            Shape oldClip = g2.getClip();
            g2.clipRect(
                    insets.left,
                    insets.top,
                    Math.max(0, getWidth() - insets.left - insets.right),
                    Math.max(0, getHeight() - insets.top - insets.bottom)
            );
            g2.drawString(text, x, y);
            g2.setClip(oldClip);
            g2.dispose();
        }
    }

    private static class WrappedTextCellRenderer extends JTextArea implements TableCellRenderer {
        WrappedTextCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setEditable(false);
            setFocusable(false);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            setText(value == null ? "" : String.valueOf(value));
            setFont(table.getFont());
            setForeground(TableThemeHelper.TEXT_PRIMARY);
            setBackground(isSelected ? TableThemeHelper.ROW_SELECTION : Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(232, 236, 240)),
                    BorderFactory.createEmptyBorder(8, 16, 8, 14)));
            setToolTipText(getText().isBlank() ? null : getText());
            return this;
        }
    }
}

