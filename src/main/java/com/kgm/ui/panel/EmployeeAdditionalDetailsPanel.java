package com.kgm.ui.panel;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.EmployeeAdditionalDetailsPanelHelper;
import com.kgm.util.EmployeeBasicFieldUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeAdditionalDetailsPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String[] READ_DATE_FORMATS = {
            "yyyy-MM-dd",
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "dd/MM/yyyy"
    };

    private final Employee data;
    private final Map<String, JTextField> textFieldMap = new LinkedHashMap<>();
    private final Map<String, JComboBox<String>> dropdownFieldMap = new LinkedHashMap<>();
    private final Map<String, UniversalDatePicker> dateFieldMap = new LinkedHashMap<>();
    private final Map<String, Boolean> dateDirtyMap = new LinkedHashMap<>();
    private JComponent topAnchor;

    public EmployeeAdditionalDetailsPanel() {
        this(null);
    }

    public EmployeeAdditionalDetailsPanel(Employee data) {
        this.data = data;
        EmployeeAdditionalDetailsPanelHelper.stylePanel(this);
        add(EmployeeAdditionalDetailsPanelHelper.createContent(buildUI()), BorderLayout.NORTH);
    }

    private JPanel buildUI() {
        JPanel root = EmployeeAdditionalDetailsPanelHelper.createRootPanel();
        List<EmployeeFieldDefinition> definitions = loadDefinitions();
        Map<String, List<EmployeeFieldDefinition>> grouped = groupByHeading(definitions);

        topAnchor = EmployeeAdditionalDetailsPanelHelper.createBreadcrumbPanel();
        root.add(topAnchor);

        List<String> headings = new ArrayList<>(grouped.keySet());
        List<JComponent> sectionRefs = new ArrayList<>();
        for (int index = 0; index < headings.size(); index++) {
            if (index > 0) {
                root.add(Box.createVerticalStrut(18));
            }
            String heading = headings.get(index);
            JComponent section = createSection(heading, grouped.get(heading));
            sectionRefs.add(section);
            root.add(section);
        }

        if (headings.isEmpty()) {
            JLabel empty = EmployeeAdditionalDetailsPanelHelper.createSectionHeader("No additional fields available");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(empty);
        } else {
            root.add(EmployeeAdditionalDetailsPanelHelper.createReturnToTopPanel(() -> scrollToComponent(topAnchor)));
            installBreadcrumbLinks((JPanel) topAnchor, headings, sectionRefs);
        }

        return root;
    }

    private List<EmployeeFieldDefinition> loadDefinitions() {
        try {
            return new EmployeeFieldDefinitionDao().listDetailFields();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return List.of();
        }
    }

    private Map<String, List<EmployeeFieldDefinition>> groupByHeading(List<EmployeeFieldDefinition> definitions) {
        Map<String, List<EmployeeFieldDefinition>> grouped = new LinkedHashMap<>();
        for (EmployeeFieldDefinition definition : definitions) {
            grouped.computeIfAbsent(definition.heading(), ignored -> new ArrayList<>()).add(definition);
        }
        return grouped;
    }

    private void installBreadcrumbLinks(JPanel breadcrumb, List<String> labels, List<JComponent> targets) {
        for (int index = 0; index < labels.size(); index++) {
            JButton link = EmployeeAdditionalDetailsPanelHelper.createBreadcrumbLink(labels.get(index));
            JComponent target = targets.get(index);
            link.addActionListener(event -> scrollToComponent(target));
            breadcrumb.add(link);

            if (index < labels.size() - 1) {
                breadcrumb.add(EmployeeAdditionalDetailsPanelHelper.createBreadcrumbSeparator());
            }
        }
    }

    private void scrollToComponent(JComponent component) {
        if (component == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Container parent = component.getParent();
            if (parent instanceof JComponent parentComponent) {
                Rectangle bounds = component.getBounds();
                bounds.y = Math.max(0, bounds.y - 12);
                bounds.height = component.getHeight() + 24;
                parentComponent.scrollRectToVisible(bounds);
                return;
            }
            component.scrollRectToVisible(new Rectangle(0, 0, component.getWidth(), component.getHeight()));
        });
    }

    private JPanel createSection(String title, List<EmployeeFieldDefinition> definitions) {
        JPanel section = EmployeeAdditionalDetailsPanelHelper.createSectionPanel();
        JLabel header = EmployeeAdditionalDetailsPanelHelper.createSectionHeader(title);
        section.add(header, BorderLayout.NORTH);
        section.add(buildGrid(definitions), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildGrid(List<EmployeeFieldDefinition> definitions) {
        JPanel grid = EmployeeAdditionalDetailsPanelHelper.createGridPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (int index = 0; index < definitions.size(); index += 2) {
            gbc.gridy = row++;
            gbc.gridx = 0;
            gbc.weightx = 0.5;
            grid.add(createField(definitions.get(index)), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.5;
            if (index + 1 < definitions.size()) {
                grid.add(createField(definitions.get(index + 1)), gbc);
            } else {
                grid.add(EmployeeAdditionalDetailsPanelHelper.createGridFiller(), gbc);
            }
        }
        return grid;
    }

    private JPanel createField(EmployeeFieldDefinition definition) {
        JPanel panel = EmployeeAdditionalDetailsPanelHelper.createFieldPanel();
        JLabel label = EmployeeAdditionalDetailsPanelHelper.createFieldLabel(definition.label());
        String value = valueFor(definition.columnName());
        JComponent field;
        if (EmployeeBasicFieldUtil.isDateField(definition)) {
            UniversalDatePicker datePicker = EmployeeAdditionalDetailsPanelHelper.createDateField(parseDate(value));
            datePicker.addDateChangeListener(() -> dateDirtyMap.put(definition.columnName(), true));
            dateFieldMap.put(definition.columnName(), datePicker);
            dateDirtyMap.put(definition.columnName(), false);
            field = datePicker;
        } else if (EmployeeBasicFieldUtil.isDropdownField(definition)) {
            JComboBox<String> combo = EmployeeAdditionalDetailsPanelHelper.createDropdownField(
                    EmployeeBasicFieldUtil.dropdownOptions(definition, true),
                    value == null || value.trim().isEmpty() ? "" : value,
                    definition.variableOptionField()
            );
            dropdownFieldMap.put(definition.columnName(), combo);
            field = combo;
        } else {
            JTextField textField = EmployeeAdditionalDetailsPanelHelper.createField(displayValue(value));
            textField.setEditable(true);
            textFieldMap.put(definition.columnName(), textField);
            field = textField;
        }

        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    public Employee getUpdatedOtherDetails() {
        Employee employee = new Employee();
        textFieldMap.forEach((column, field) -> {
            if (!field.isEditable()) {
                return;
            }

            String value = field.getText();
            if (isEmpty(value)) {
                return;
            }

            writeValue(employee, column, value.trim());
        });
        dropdownFieldMap.forEach((column, combo) -> {
            String value = DropdownFieldSupport.value(combo);
            if (isEmpty(value)) {
                return;
            }

            writeValue(employee, column, value.trim());
        });
        dateFieldMap.forEach((column, picker) -> {
            if (!Boolean.TRUE.equals(dateDirtyMap.get(column))) {
                return;
            }

            Date value = picker.getDate();
            if (value == null) {
                return;
            }

            writeValue(employee, column, DB_DATE_FORMAT.format(value));
        });
        return employee;
    }

    private String valueFor(String column) {
        if (data == null || column == null || column.isBlank()) {
            return "";
        }

        try {
            Field field = Employee.class.getDeclaredField(column);
            field.setAccessible(true);
            Object value = field.get(data);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException exception) {
            String value = data.getDynamicField(column);
            return value == null ? "" : value;
        }
    }

    private void writeValue(Employee employee, String column, String value) {
        try {
            Field field = Employee.class.getDeclaredField(column);
            field.setAccessible(true);
            field.set(employee, value);
        } catch (ReflectiveOperationException exception) {
            employee.setDynamicField(column, value);
        }
    }

    private String displayValue(String value) {
        return isEmpty(value) ? "N/A" : value;
    }

    private Date parseDate(String value) {
        if (isEmpty(value) || "Choose Date".equalsIgnoreCase(value.trim())) {
            return null;
        }

        for (String pattern : READ_DATE_FORMATS) {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setLenient(false);
            try {
                return format.parse(value.trim());
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private boolean isEmpty(String value) {
        if (value == null) {
            return true;
        }

        String text = value.trim();
        return text.isEmpty()
                || text.equalsIgnoreCase("N/A")
                || text.equalsIgnoreCase("NA")
                || text.equalsIgnoreCase("NULL")
                || text.equals("-");
    }

}
