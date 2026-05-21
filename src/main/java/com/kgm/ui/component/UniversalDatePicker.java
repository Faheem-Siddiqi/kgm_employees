package com.kgm.ui.component;

import com.kgm.ui.styling.UniversalDatePickerHelper;
import com.toedter.calendar.JCalendar;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class UniversalDatePicker extends JPanel {
    private static final String PLACEHOLDER_TEXT = "Choose Date";
    private static final Color PLACEHOLDER_COLOR = new Color(130, 140, 150);
    private static final Color VALUE_COLOR = Color.BLACK;

    private final JTextField displayField;
    private final JPanel iconPanel;

    private final JCalendar calendar;
    private final JDialog calendarDialog;

    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;

    private Date selectedDate;

    private boolean enabled = true;

    private Runnable dateChangeListener;

    public UniversalDatePicker() {
        this(null);
    }

    public UniversalDatePicker(Date initialDate) {

        super(new BorderLayout());

        this.selectedDate = initialDate;

        UniversalDatePickerHelper.stylePicker(this);

        displayField = UniversalDatePickerHelper.createDisplayField(displayText(initialDate));
        applyDisplayState();

        add(displayField, BorderLayout.CENTER);

        iconPanel = UniversalDatePickerHelper.createIconPanel();

        add(iconPanel, BorderLayout.EAST);

        calendar = new JCalendar();

        calendar.setDate(calendarDate());

        UniversalDatePickerHelper.styleCalendar(calendar);

        JPanel calendarContainer = UniversalDatePickerHelper.createCalendarContainer(calendar, createTimePanel());

        Window owner = null;

        try {
            owner = SwingUtilities.getWindowAncestor(this);
        } catch (Exception ignored) {
        }

        calendarDialog = UniversalDatePickerHelper.createCalendarDialog(owner, calendarContainer);

        MouseAdapter showCalendarListener = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (enabled) {
                    showCalendar();
                }
            }
        };

        addMouseListener(showCalendarListener);

        displayField.addMouseListener(showCalendarListener);

        iconPanel.addMouseListener(showCalendarListener);

        calendar.addPropertyChangeListener("date", evt -> {

            Date newDate = (Date) evt.getNewValue();

            if (newDate != null) {

                Calendar current = Calendar.getInstance();

                current.setTime(selectedDate == null ? newDate : selectedDate);

                Calendar picked = Calendar.getInstance();

                picked.setTime(newDate);

                current.set(Calendar.YEAR, picked.get(Calendar.YEAR));

                current.set(Calendar.MONTH, picked.get(Calendar.MONTH));

                current.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH));

                setSelectedDate(current.getTime());

                updateSpinners();
            }
        });
    }

    private void forceCompactCalendar(JCalendar calendar) {
        UniversalDatePickerHelper.forceCompactCalendar(calendar);
    }

    private JPanel createTimePanel() {

        JPanel timePanel = UniversalDatePickerHelper.createTimePanel();

        Calendar cal = Calendar.getInstance();

        cal.setTime(calendarDate());

        JLabel hourLabel = smallLabel("Hr");

        timePanel.add(hourLabel);

        hourSpinner = new JSpinner(new SpinnerNumberModel(
                cal.get(Calendar.HOUR_OF_DAY), 0, 23, 1));

        styleSpinner(hourSpinner);

        UniversalDatePickerHelper.setSmallControlWidth(hourSpinner, 44);

        hourSpinner.addChangeListener(e -> updateTimeFromSpinners());

        timePanel.add(hourSpinner);

        JLabel minuteLabel = smallLabel("Min");

        timePanel.add(minuteLabel);

        minuteSpinner = new JSpinner(new SpinnerNumberModel(
                cal.get(Calendar.MINUTE), 0, 59, 1));

        styleSpinner(minuteSpinner);

        UniversalDatePickerHelper.setSmallControlWidth(minuteSpinner, 44);

        minuteSpinner.addChangeListener(e -> updateTimeFromSpinners());

        timePanel.add(minuteSpinner);

        JButton nowButton = smallButton("Now", false);

        UniversalDatePickerHelper.setSmallControlWidth(nowButton, 56);

        nowButton.addActionListener(e -> {

            Date now = new Date();

            calendar.setDate(now);

            setSelectedDate(now);

            updateSpinners();
        });

        timePanel.add(nowButton);

        JButton doneButton = smallButton("OK", true);

        UniversalDatePickerHelper.setSmallControlWidth(doneButton, 52);

        doneButton.addActionListener(e -> confirmAndClose());

        timePanel.add(doneButton);

        return timePanel;
    }

    private JLabel smallLabel(String text) {
        return UniversalDatePickerHelper.smallLabel(text);
    }

    private void styleSpinner(JSpinner spinner) {
        UniversalDatePickerHelper.styleSpinner(spinner);
    }

    private JButton smallButton(String text, boolean primary) {
        return UniversalDatePickerHelper.smallButton(text, primary);
    }

    private void updateTimeFromSpinners() {

        if (hourSpinner == null || minuteSpinner == null) {
            return;
        }

        Calendar c = Calendar.getInstance();

        c.setTime(selectedDate == null ? calendarDate() : selectedDate);

        c.set(Calendar.HOUR_OF_DAY, (Integer) hourSpinner.getValue());

        c.set(Calendar.MINUTE, (Integer) minuteSpinner.getValue());

        c.set(Calendar.SECOND, 0);

        c.set(Calendar.MILLISECOND, 0);

        setSelectedDate(c.getTime());
    }

    private void confirmAndClose() {

        Date calendarDate = calendar.getDate();

        if (calendarDate != null) {

            Calendar finalDate = Calendar.getInstance();

            finalDate.setTime(calendarDate);

            finalDate.set(Calendar.HOUR_OF_DAY, (Integer) hourSpinner.getValue());

            finalDate.set(Calendar.MINUTE, (Integer) minuteSpinner.getValue());

            finalDate.set(Calendar.SECOND, 0);

            finalDate.set(Calendar.MILLISECOND, 0);

            setSelectedDate(finalDate.getTime());
        }

        calendarDialog.setVisible(false);
    }

    private void updateSpinners() {

        if (hourSpinner == null || minuteSpinner == null) {
            return;
        }

        Calendar cal = Calendar.getInstance();

        cal.setTime(calendarDate());

        hourSpinner.setValue(cal.get(Calendar.HOUR_OF_DAY));

        minuteSpinner.setValue(cal.get(Calendar.MINUTE));
    }

    private CompoundBorder createBorder() {

        return UniversalDatePickerHelper.fieldBorder();
    }

    private String formatDate(Date date) {

        if (date == null) {
            return "";
        }

        return new SimpleDateFormat(UniversalDatePickerHelper.DATE_FORMAT).format(date);
    }

    private void showCalendar() {

        if (!enabled) {
            return;
        }

        calendar.setDate(calendarDate());

        updateSpinners();

        Point screenPoint = getLocationOnScreen();

        Rectangle screen = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        int x = screenPoint.x;

        int y = screenPoint.y + getHeight() + 2;

        if (x + UniversalDatePickerHelper.POPUP_WIDTH > screen.x + screen.width) {

            x = screen.x + screen.width - UniversalDatePickerHelper.POPUP_WIDTH;
        }

        if (y + UniversalDatePickerHelper.POPUP_HEIGHT > screen.y + screen.height) {

            y = screenPoint.y - UniversalDatePickerHelper.POPUP_HEIGHT - 2;
        }

        calendarDialog.setLocation(x, y);

        calendarDialog.setVisible(true);
    }

    public Date getDate() {
        return selectedDate;
    }

    public void setDate(Date date) {

        setSelectedDate(date);

        calendar.setDate(calendarDate());

        updateSpinners();
    }

    private void setSelectedDate(Date date) {

        this.selectedDate = date;

        displayField.setText(displayText(date));
        applyDisplayState();

        if (dateChangeListener != null) {
            dateChangeListener.run();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {

        this.enabled = enabled;

        super.setEnabled(enabled);

        displayField.setEnabled(enabled);

        iconPanel.setEnabled(enabled);

        UniversalDatePickerHelper.applyEnabledStyle(displayField, iconPanel, enabled);
        if (enabled) {
            applyDisplayState();
        }

        if (!enabled) {
            calendarDialog.setVisible(false);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setFocus(boolean focus) {

        if (focus) {

            setBorder(UniversalDatePickerHelper.focusedFieldBorder());

        } else {

            setBorder(createBorder());
        }
    }

    public void addDateChangeListener(Runnable onChange) {

        this.dateChangeListener = onChange;
    }

    public void applyProjectStyling() {
        UniversalDatePickerHelper.applyProjectStyling(this);
    }

    private Date calendarDate() {
        if (selectedDate != null) {
            return selectedDate;
        }
        Date visibleDate = calendar == null ? null : calendar.getDate();
        return visibleDate == null ? new Date() : visibleDate;
    }

    private String displayText(Date date) {
        return date == null ? PLACEHOLDER_TEXT : formatDate(date);
    }

    private void applyDisplayState() {
        displayField.setForeground(selectedDate == null ? PLACEHOLDER_COLOR : VALUE_COLOR);
    }

}
