package com.kgm.ui.component;

import com.kgm.ui.styling.UniversalDateRangePickerHelper;
import com.toedter.calendar.JCalendar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class UniversalDateRangePicker extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JLabel displayLabel = UniversalDateRangePickerHelper.createDisplayLabel();
    private final JLabel iconLabel = UniversalDateRangePickerHelper.createIconLabel();
    private final JCalendar startCalendar = new JCalendar();
    private final JCalendar endCalendar = new JCalendar();
    private final JDialog rangeDialog;

    private LocalDate startDate;
    private LocalDate endDate;
    private Runnable rangeChangeListener;
    private boolean enabled = true;

    public UniversalDateRangePicker() {
        super(new BorderLayout(8, 0));
        UniversalDateRangePickerHelper.stylePicker(this);

        add(displayLabel, BorderLayout.CENTER);

        add(iconLabel, BorderLayout.EAST);

        UniversalDateRangePickerHelper.styleCalendar(startCalendar);
        UniversalDateRangePickerHelper.styleCalendar(endCalendar);

        rangeDialog = new JDialog((Window) null);
        UniversalDateRangePickerHelper.styleDialog(rangeDialog, createPickerPanel());

        MouseAdapter openListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                showCalendarDialog();
            }
        };
        addMouseListener(openListener);
        displayLabel.addMouseListener(openListener);
        iconLabel.addMouseListener(openListener);

        updateDisplay();
    }

    public DateRange getDateRange() {
        return new DateRange(startDate, endDate).normalized();
    }

    public String getFilterText() {
        return getDateRange().displayText();
    }

    public boolean hasSelection() {
        return getDateRange().hasSelection();
    }

    public void clearRange() {
        setDateRange(null, null);
    }

    public void addRangeChangeListener(Runnable onChange) {
        this.rangeChangeListener = onChange;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        super.setEnabled(enabled);
        displayLabel.setEnabled(enabled);
        iconLabel.setEnabled(enabled);
        UniversalDateRangePickerHelper.applyEnabledStyle(this, displayLabel, iconLabel, enabled);
        if (!enabled) {
            rangeDialog.setVisible(false);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private JPanel createPickerPanel() {
        JPanel content = UniversalDateRangePickerHelper.createPickerPanel();

        JPanel calendars = UniversalDateRangePickerHelper.createCalendarsPanel();
        calendars.add(calendarBlock("From", startCalendar));
        calendars.add(calendarBlock("To", endCalendar));
        content.add(calendars, BorderLayout.CENTER);

        JPanel actions = UniversalDateRangePickerHelper.createActionsPanel();

        JButton clear = textButton("Clear", false);
        clear.addActionListener(event -> {
            clearRange();
            rangeDialog.setVisible(false);
        });

        JButton apply = textButton("Apply", true);
        apply.addActionListener(event -> {
            LocalDate start = toLocalDate(startCalendar.getDate());
            LocalDate end = toLocalDate(endCalendar.getDate());
            setDateRange(start, end);
            rangeDialog.setVisible(false);
        });

        actions.add(clear);
        actions.add(apply);
        content.add(actions, BorderLayout.SOUTH);
        return content;
    }

    private JPanel calendarBlock(String title, JCalendar calendar) {
        return UniversalDateRangePickerHelper.createCalendarBlock(title, calendar);
    }

    private JButton textButton(String text, boolean primary) {
        return UniversalDateRangePickerHelper.textButton(text, primary);
    }

    private void showCalendarDialog() {
        if (!enabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        DateRange range = getDateRange();
        startCalendar.setDate(toDate(range.startOr(today)));
        endCalendar.setDate(toDate(range.endOr(range.startOr(today))));

        Point screenPoint = getLocationOnScreen();
        Dimension dialogSize = rangeDialog.getSize();
        Rectangle screen = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        int x = screenPoint.x;
        int y = screenPoint.y + getHeight() + 2;
        if (x + dialogSize.width > screen.x + screen.width) {
            x = screen.x + screen.width - dialogSize.width;
        }
        if (y + dialogSize.height > screen.y + screen.height) {
            y = screenPoint.y - dialogSize.height - 2;
        }

        rangeDialog.setLocation(Math.max(screen.x, x), Math.max(screen.y, y));
        rangeDialog.setVisible(true);
    }

    private void setDateRange(LocalDate start, LocalDate end) {
        DateRange normalized = new DateRange(start, end).normalized();
        boolean changed = !sameDate(startDate, normalized.startDate())
                || !sameDate(endDate, normalized.endDate());
        startDate = normalized.startDate();
        endDate = normalized.endDate();
        updateDisplay();
        if (changed && rangeChangeListener != null) {
            rangeChangeListener.run();
        }
    }

    private boolean sameDate(LocalDate first, LocalDate second) {
        return first == null ? second == null : first.equals(second);
    }

    private void updateDisplay() {
        DateRange range = getDateRange();
        displayLabel.setText(range.hasSelection() ? range.displayText() : "Any date");
        displayLabel.setToolTipText(range.hasSelection() ? range.displayText() : "Filter by date range");
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
        public static DateRange empty() {
            return new DateRange(null, null);
        }

        public static DateRange single(LocalDate date) {
            return new DateRange(date, date);
        }

        public boolean hasSelection() {
            return startDate != null || endDate != null;
        }

        public boolean isEmpty() {
            return !hasSelection();
        }

        public DateRange normalized() {
            if (startDate == null && endDate == null) {
                return this;
            }
            if (startDate == null) {
                return new DateRange(endDate, endDate);
            }
            if (endDate == null) {
                return new DateRange(startDate, startDate);
            }
            if (endDate.isBefore(startDate)) {
                return new DateRange(endDate, startDate);
            }
            return this;
        }

        public LocalDate startOr(LocalDate fallback) {
            DateRange normalized = normalized();
            return normalized.startDate == null ? fallback : normalized.startDate;
        }

        public LocalDate endOr(LocalDate fallback) {
            DateRange normalized = normalized();
            return normalized.endDate == null ? fallback : normalized.endDate;
        }

        public String displayText() {
            DateRange normalized = normalized();
            if (!normalized.hasSelection()) {
                return "";
            }
            String start = DATE_FORMAT.format(normalized.startDate);
            String end = DATE_FORMAT.format(normalized.endDate);
            return start.equals(end) ? start : start + " to " + end;
        }
    }
}
