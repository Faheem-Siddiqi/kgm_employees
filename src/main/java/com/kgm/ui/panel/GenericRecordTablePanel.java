package com.kgm.ui.panel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class GenericRecordTablePanel<T> extends JPanel {
    private final UniversalTablePanel tablePanel;
    private final Function<T, Object[]> rowMapper;
    private final List<T> records = new ArrayList<>();

    public GenericRecordTablePanel(
            String[] columns,
            String emptyText,
            Function<T, Object[]> rowMapper
    ) {
        this.tablePanel = new UniversalTablePanel(columns, emptyText);
        this.rowMapper = rowMapper;

        setLayout(new BorderLayout());
        setOpaque(false);
        add(tablePanel, BorderLayout.CENTER);
    }

    public void setRows(List<T> newRecords) {
        records.clear();
        records.addAll(newRecords == null ? List.of() : newRecords);

        List<Object[]> tableRows = new ArrayList<>();
        for (T record : records) {
            tableRows.add(rowMapper.apply(record));
        }
        tablePanel.setRows(tableRows);
    }

    public void clearRows() {
        records.clear();
        tablePanel.clearRows();
    }

    public void setEmptyText(String emptyText) {
        tablePanel.setEmptyText(emptyText);
    }

    public void setActionColumn(int column, String text, Consumer<T> onAction) {
        tablePanel.setActionColumn(column, text, row -> {
            T record = recordAt(row);
            if (record != null && onAction != null) {
                onAction.accept(record);
            }
        });
    }

    public void setLinkColumn(int column, Consumer<T> onLink, boolean highlightOnlyOnHover) {
        tablePanel.setLinkColumn(column, row -> {
            T record = recordAt(row);
            if (record != null && onLink != null) {
                onLink.accept(record);
            }
        }, highlightOnlyOnHover);
    }

    public void setWrappedTextColumn(int column) {
        tablePanel.setWrappedTextColumn(column);
    }

    public void setColumnAlignment(int column, int alignment) {
        tablePanel.setColumnAlignment(column, alignment);
    }

    public void setPreferredColumnWidthLimit(int column, int maxWidth) {
        tablePanel.setPreferredColumnWidthLimit(column, maxWidth);
    }

    public void setPaginationBottomGap(int paginationBottomGap) {
        tablePanel.setPaginationBottomGap(paginationBottomGap);
    }

    private T recordAt(int row) {
        return row >= 0 && row < records.size() ? records.get(row) : null;
    }
}
