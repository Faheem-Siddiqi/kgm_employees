package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class RoomDetailHelper {
    public static final int KPI_BOTTOM_MARGIN = 36;

    private RoomDetailHelper() {
    }

    public static JPanel pagePanel() {
        return TableStyleHelper.pagePanel();
    }

    public static GridBagConstraints pageConstraints(int y) {
        return TableStyleHelper.pageConstraints(y);
    }

    public static GridBagConstraints kpiConstraints(int y) {
        GridBagConstraints constraints = pageConstraints(y);
        constraints.insets = new Insets(0, 0, KPI_BOTTOM_MARGIN, 0);
        return constraints;
    }

    public static JScrollPane scrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
            scrollPane.getHorizontalScrollBar().setValue(0);
        });
        return scrollPane;
    }

    public static void styleTableLink(
            JLabel label,
            JTable table,
            boolean selected,
            boolean hovered,
            String text
    ) {
        label.setText(hovered ? "<html><u>" + escapeHtml(text) + "</u></html>" : text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setForeground(TableStyleHelper.PRIMARY);
        label.setBackground(selected ? TableStyleHelper.ROW_SELECTION : Color.WHITE);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(232, 236, 240)),
                new EmptyBorder(0, 16, 0, 14)
        ));
    }

    public static String textOrDash(String text) {
        return text == null || text.isBlank() ? "-" : text.trim();
    }

    private static String escapeHtml(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u0026') {
                sb.append("\u0026").append("amp;");
            } else if (c == '\u003c') {
                sb.append("\u003c").append("lt;");
            } else if (c == '\u003e') {
                sb.append("\u003e").append("gt;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}