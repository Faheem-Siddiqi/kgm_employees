package com.kgm.ui.styling;

import com.kgm.util.DateDisplayFormatter;
import com.toedter.calendar.JCalendar;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Locale;

public final class UniversalDatePickerHelper {
    public static final String DATE_FORMAT = DateDisplayFormatter.DISPLAY_PATTERN;
    public static final int FIELD_HEIGHT = 34;
    public static final int FIELD_WIDTH = 340;
    public static final int POPUP_WIDTH = 330;
    public static final int POPUP_HEIGHT = 330;

    private static final int ICON_SIZE = 16;
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color BORDER_FOCUS_COLOR = new Color(0, 112, 210);
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color DISABLED_BACKGROUND = new Color(248, 248, 248);
    private static final Color DISABLED_FOREGROUND = new Color(150, 150, 150);
    private static final Color ICON_COLOR = new Color(120, 120, 120);
    private static final Font DISPLAY_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private UniversalDatePickerHelper() {
    }

    public static void stylePicker(JPanel picker) {
        picker.setOpaque(false);
        picker.setLayout(new BorderLayout());
        picker.setBorder(fieldBorder());
        picker.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        picker.setMinimumSize(new Dimension(260, FIELD_HEIGHT));
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
    }

    public static JTextField createDisplayField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(DISPLAY_FONT);
        field.setEditable(false);
        field.setFocusable(false);
        field.setBorder(null);
        field.setBackground(BACKGROUND_COLOR);
        field.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return field;
    }

    public static JPanel createIconPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(34, FIELD_HEIGHT));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(new CalendarIcon(ICON_COLOR));
        return panel;
    }

    public static void styleCalendar(JCalendar calendar) {
        calendar.setLocale(Locale.ENGLISH);
        calendar.setDecorationBackgroundColor(BACKGROUND_COLOR);
        calendar.setWeekdayForeground(new Color(60, 60, 60));
        forceCompactCalendar(calendar);
    }

    public static JPanel createCalendarContainer(JCalendar calendar, JPanel timePanel) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(BACKGROUND_COLOR);
        container.setBorder(new EmptyBorder(8, 8, 8, 8));
        Dimension size = new Dimension(POPUP_WIDTH, POPUP_HEIGHT);
        container.setPreferredSize(size);
        container.setMinimumSize(size);
        container.setMaximumSize(size);
        container.add(calendar, BorderLayout.CENTER);
        container.add(timePanel, BorderLayout.SOUTH);
        return container;
    }

    public static JDialog createCalendarDialog(Window owner, JComponent content) {
        JDialog dialog = new JDialog(owner);
        dialog.setUndecorated(true);
        dialog.setModal(false);
        dialog.setAlwaysOnTop(false);
        dialog.setLayout(new BorderLayout());
        dialog.getRootPane().setBorder(new LineBorder(new Color(190, 190, 190), 1));
        dialog.add(content, BorderLayout.CENTER);
        dialog.setSize(POPUP_WIDTH, POPUP_HEIGHT);
        return dialog;
    }

    public static void forceCompactCalendar(JCalendar calendar) {
        Dimension compactSize = new Dimension(310, 230);
        calendar.setPreferredSize(compactSize);
        calendar.setMinimumSize(compactSize);
        calendar.setMaximumSize(compactSize);
        calendar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        for (Component component : calendar.getComponents()) {
            component.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        }
    }

    public static JPanel createTimePanel() {
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        timePanel.setBackground(new Color(245, 245, 245));
        timePanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                new EmptyBorder(4, 4, 4, 4)));
        return timePanel;
    }

    public static JLabel smallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(new Color(60, 60, 60));
        return label;
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(48, 24));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField field = ((JSpinner.DefaultEditor) editor).getTextField();
            field.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            field.setHorizontalAlignment(JTextField.CENTER);
        }
    }

    public static void setSmallControlWidth(JComponent component, int width) {
        component.setPreferredSize(new Dimension(width, 24));
    }

    public static JButton smallButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setFocusPainted(false);
        if (primary) {
            button.setBackground(BORDER_FOCUS_COLOR);
            button.setForeground(Color.WHITE);
            button.setBorder(new EmptyBorder(4, 10, 4, 10));
        } else {
            button.setBackground(new Color(245, 245, 245));
            button.setForeground(new Color(80, 80, 80));
            button.setBorder(new CompoundBorder(
                    new LineBorder(new Color(170, 170, 170)),
                    new EmptyBorder(4, 10, 4, 10)));
        }
        return button;
    }

    public static CompoundBorder fieldBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8));
    }

    public static CompoundBorder focusedFieldBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_FOCUS_COLOR, 2),
                new EmptyBorder(5, 7, 5, 7));
    }

    public static void applyEnabledStyle(JTextField displayField, JPanel iconPanel, boolean enabled) {
        displayField.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        iconPanel.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        displayField.setBackground(enabled ? BACKGROUND_COLOR : DISABLED_BACKGROUND);
        displayField.setForeground(enabled ? Color.BLACK : DISABLED_FOREGROUND);
        iconPanel.setVisible(enabled);
    }

    public static void applyProjectStyling(JPanel picker) {
        picker.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        picker.setMinimumSize(new Dimension(260, FIELD_HEIGHT));
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        picker.setFont(DISPLAY_FONT);
        picker.setBackground(BACKGROUND_COLOR);
        picker.setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(6, 8, 6, 8)));
    }

    private static class CalendarIcon extends JLabel {
        private final Color iconColor;

        CalendarIcon(Color color) {
            this.iconColor = color;
            setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = (getWidth() - ICON_SIZE) / 2;
            int y = (getHeight() - ICON_SIZE) / 2;
            int w = ICON_SIZE;
            int h = ICON_SIZE;
            g2.setColor(iconColor);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(x, y + 4, w, h - 4, 2, 2);
            g2.fillRect(x, y + 4, w, 4);
            g2.drawLine(x + 3, y, x + 3, y + 6);
            g2.drawLine(x + w - 3, y, x + w - 3, y + 6);
            g2.drawLine(x + w / 3, y + 8, x + w / 3, y + h - 2);
            g2.drawLine(x + 2 * w / 3, y + 8, x + 2 * w / 3, y + h - 2);
            g2.drawLine(x + 2, y + 12, x + w - 2, y + 12);
            g2.dispose();
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(4, 4, 4, 4);
        }
    }
}
