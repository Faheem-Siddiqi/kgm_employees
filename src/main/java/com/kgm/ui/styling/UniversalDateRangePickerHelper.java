package com.kgm.ui.styling;

import com.toedter.calendar.JCalendar;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Locale;

public final class UniversalDateRangePickerHelper {
    public static final int FIELD_HEIGHT = 34;
    public static final int FIELD_WIDTH = 250;
    public static final int ICON_SIZE = 16;

    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color ICON_COLOR = TableStyleHelper.TEXT_SECONDARY;
    private static final Color DISABLED_BACKGROUND = new Color(248, 248, 248);
    private static final Color DISABLED_FOREGROUND = new Color(150, 150, 150);

    private UniversalDateRangePickerHelper() {
    }

    public static void stylePicker(JPanel picker) {
        picker.setOpaque(true);
        picker.setBackground(Color.WHITE);
        picker.setBorder(fieldBorder());
        picker.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        picker.setMinimumSize(new Dimension(220, FIELD_HEIGHT));
        picker.setMaximumSize(new Dimension(300, FIELD_HEIGHT));
        picker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JLabel createDisplayLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TableStyleHelper.TEXT_PRIMARY);
        label.setBorder(new EmptyBorder(0, 0, 0, 0));
        return label;
    }

    public static JLabel createIconLabel() {
        JLabel label = new CalendarIcon();
        label.setPreferredSize(new Dimension(26, FIELD_HEIGHT));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return label;
    }

    public static void styleDialog(JDialog dialog, JComponent content) {
        dialog.setUndecorated(true);
        dialog.setModal(false);
        dialog.setLayout(new BorderLayout());
        dialog.getRootPane().setBorder(new LineBorder(new Color(190, 190, 190), 1));
        dialog.add(content, BorderLayout.CENTER);
        dialog.pack();
    }

    public static JPanel createPickerPanel() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        return content;
    }

    public static JPanel createCalendarsPanel() {
        JPanel calendars = new JPanel(new GridLayout(1, 2, 12, 0));
        calendars.setOpaque(false);
        return calendars;
    }

    public static JPanel createActionsPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        return actions;
    }

    public static JPanel createCalendarBlock(String title, JCalendar calendar) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(TableStyleHelper.TEXT_SECONDARY);
        block.add(label, BorderLayout.NORTH);
        block.add(calendar, BorderLayout.CENTER);
        return block;
    }

    public static JButton textButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            button.setForeground(Color.WHITE);
            button.setBackground(TableStyleHelper.PRIMARY);
            button.setBorder(new EmptyBorder(7, 16, 7, 16));
        } else {
            button.setForeground(TableStyleHelper.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorder(new CompoundBorder(
                    new LineBorder(TableStyleHelper.BORDER),
                    new EmptyBorder(6, 14, 6, 14)));
        }
        return button;
    }

    public static CompoundBorder fieldBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8));
    }

    public static void styleCalendar(JCalendar calendar) {
        calendar.setLocale(Locale.ENGLISH);
        calendar.setBackground(Color.WHITE);
        calendar.setDecorationBackgroundColor(Color.WHITE);
        calendar.setWeekdayForeground(TableStyleHelper.TEXT_SECONDARY);
        calendar.setSundayForeground(TableStyleHelper.DANGER);
        Dimension size = new Dimension(250, 220);
        calendar.setPreferredSize(size);
        calendar.setMinimumSize(size);
        calendar.setMaximumSize(size);
        styleChildren(calendar);
    }

    public static void applyEnabledStyle(
            JPanel picker,
            JLabel displayLabel,
            JLabel iconLabel,
            boolean enabled
    ) {
        picker.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        iconLabel.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        picker.setBackground(enabled ? Color.WHITE : DISABLED_BACKGROUND);
        displayLabel.setForeground(enabled ? TableStyleHelper.TEXT_PRIMARY : DISABLED_FOREGROUND);
    }

    private static void styleChildren(Component component) {
        component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleChildren(child);
            }
        }
    }

    private static class CalendarIcon extends JLabel {
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = (getWidth() - ICON_SIZE) / 2;
            int y = (getHeight() - ICON_SIZE) / 2;
            g2.setColor(ICON_COLOR);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x, y + 4, ICON_SIZE, ICON_SIZE - 4, 2, 2);
            g2.fillRect(x, y + 4, ICON_SIZE, 4);
            g2.drawLine(x + 3, y, x + 3, y + 6);
            g2.drawLine(x + ICON_SIZE - 3, y, x + ICON_SIZE - 3, y + 6);
            g2.drawLine(x + 3, y + 10, x + ICON_SIZE - 3, y + 10);
            g2.drawLine(x + 3, y + 13, x + ICON_SIZE - 3, y + 13);
            g2.dispose();
        }
    }
}
