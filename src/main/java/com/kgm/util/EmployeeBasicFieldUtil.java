package com.kgm.util;

import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EmployeeBasicFieldUtil {
    public static final String FUNDAMENTALS_HEADING = "Fundamentals";
    private static final Set<String> FUNDAMENTALS_HEADING_KEYS = Set.of("fundamental", "fundamentals");
    public static final List<String> BASIC_COLUMNS = List.of(
            "EMPLOYEE_CODE",
            "UNT_CODE",
            "DESCR",
            "EMP_NAME",
            "FATHER_NAME",
            "NID",
            "EMP_CONTNO",
            "PERSONAL_EMAIL",
            "DEPARTMENT",
            "DESIGNATION",
            "SECTION",
            "GRADE",
            "SHIFT",
            "DOB",
            "GENDER",
            "RESIGN_REASON",
            "JOINING_DATE",
            "RESIGN_DATE",
            "CURRENT_ADR",
            "PERMANENT_ADR"
    );
    public static final Set<String> DATE_COLUMNS = Set.of("DOB", "JOINING_DATE", "RESIGN_DATE");
    public static final Set<String> REQUIRED_COLUMNS = defaultRequiredColumns();

    private static final Map<String, String> DEFAULT_LABELS = defaultLabels();

    private EmployeeBasicFieldUtil() {
    }

    private static Set<String> defaultRequiredColumns() {
        Set<String> required = new LinkedHashSet<>(BASIC_COLUMNS);
        required.remove("PERSONAL_EMAIL");
        return Set.copyOf(required);
    }

    public static List<EmployeeFieldDefinition> loadBasicDefinitions() {
        try {
            return EmployeeFieldDefinitionCache.basicFields();
        } catch (RuntimeException exception) {
            return fallbackDefinitions();
        }
    }

    public static List<EmployeeFieldDefinition> basicDefinitions(List<EmployeeFieldDefinition> definitions) {
        Map<String, EmployeeFieldDefinition> byColumn = new LinkedHashMap<>();
        for (EmployeeFieldDefinition definition : definitions) {
            if (isFundamentalsField(definition)) {
                byColumn.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
            }
        }

        List<EmployeeFieldDefinition> ordered = new ArrayList<>();
        for (int index = 0; index < BASIC_COLUMNS.size(); index++) {
            String column = BASIC_COLUMNS.get(index);
            EmployeeFieldDefinition definition = byColumn.remove(column);
            if (definition != null) {
                ordered.add(definition);
            }
        }
        List<EmployeeFieldDefinition> extraCoreFields = new ArrayList<>(byColumn.values());
        extraCoreFields.sort(Comparator
                .comparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER));
        ordered.addAll(extraCoreFields);
        if (ordered.isEmpty()) {
            return fallbackDefinitions();
        }
        ordered.sort(Comparator.comparingInt(definition -> basicOrder(definition.columnName())));
        return ordered;
    }

    public static boolean isBasicField(String columnName) {
        return columnName != null && BASIC_COLUMNS.contains(columnName.toUpperCase(Locale.ROOT));
    }

    public static boolean isRequired(String columnName) {
        return columnName != null && REQUIRED_COLUMNS.contains(columnName.toUpperCase(Locale.ROOT));
    }

    public static boolean isRequired(EmployeeFieldDefinition definition) {
        return definition != null && definition.requiredField();
    }

    public static boolean isFundamentalsField(EmployeeFieldDefinition definition) {
        return definition != null
                && !definition.documentField()
                && (definition.coreField() || isFundamentalsHeading(definition.heading()));
    }

    public static boolean isFundamentalsHeading(String heading) {
        return FUNDAMENTALS_HEADING_KEYS.contains(headingKey(heading));
    }

    public static boolean isDateField(String columnName) {
        return columnName != null && DATE_COLUMNS.contains(columnName.toUpperCase(Locale.ROOT));
    }

    public static boolean isDateField(EmployeeFieldDefinition definition) {
        return definition != null && (definition.dateField() || isDateField(definition.columnName()));
    }

    public static boolean isComboField(String columnName) {
        return "GENDER".equalsIgnoreCase(columnName) || "RESIGN_REASON".equalsIgnoreCase(columnName);
    }

    public static boolean isDropdownField(EmployeeFieldDefinition definition) {
        return definition != null && definition.dropdownField();
    }

    public static String[] comboOptions(String columnName, boolean includeBlank) {
        String[] values;
        if ("GENDER".equalsIgnoreCase(columnName)) {
            values = new String[]{"Male", "Female", "Other"};
        } else if ("RESIGN_REASON".equalsIgnoreCase(columnName)) {
            values = new String[]{"Layoff", "Resignation", "Other"};
        } else {
            values = new String[0];
        }
        if (!includeBlank) {
            return values;
        }
        String placeholder = columnName != null && "RESIGN_REASON".equalsIgnoreCase(columnName)
                ? dropdownPlaceholder(true) : dropdownPlaceholder(false);
        String[] result = new String[values.length + 1];
        result[0] = placeholder;
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    public static String[] dropdownOptions(EmployeeFieldDefinition definition, boolean includeBlank) {
        if (definition == null) {
            return new String[0];
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (definition.dropdownField()) {
            for (String option : definition.dropdownOptionList()) {
                if (!option.isBlank()) {
                    values.add(option);
                }
            }
        }
        if (values.isEmpty() && isComboField(definition.columnName())) {
            for (String option : comboOptions(definition.columnName(), false)) {
                values.add(option);
            }
        }
        if (includeBlank) {
            String placeholder = dropdownPlaceholder(definition.variableOptionField());
            String[] result = new String[values.size() + 1];
            result[0] = placeholder;
            int index = 1;
            for (String v : values) {
                result[index++] = v;
            }
            return result;
        }
        return values.toArray(new String[0]);
    }

    /**
     * Returns the default placeholder label for a dropdown based on whether it allows custom values.
     * Fixed dropdowns show "Choose", variable (type-to-select) dropdowns show "Type to select".
     */
    public static String dropdownPlaceholder(boolean variableOptionField) {
        return variableOptionField ? "Type to select" : "Choose";
    }

    public static boolean isMultilineField(String columnName) {
        return "PERMANENT_ADR".equalsIgnoreCase(columnName)
                || "CURRENT_ADR".equalsIgnoreCase(columnName);
    }

    public static boolean isMultilineField(EmployeeFieldDefinition definition) {
        return definition != null && (definition.textAreaField() || isMultilineField(definition.columnName()));
    }

    public static String valueFor(Employee employee, String columnName) {
        if (employee == null || columnName == null || columnName.isBlank()) {
            return "";
        }
        try {
            Field field = Employee.class.getDeclaredField(columnName.toUpperCase(Locale.ROOT));
            field.setAccessible(true);
            Object value = field.get(employee);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException exception) {
            String value = employee.getDynamicField(columnName);
            return value == null ? "" : value;
        }
    }

    public static void writeValue(Employee employee, String columnName, String value) {
        if (employee == null || columnName == null || columnName.isBlank()) {
            return;
        }
        String column = columnName.toUpperCase(Locale.ROOT);
        try {
            Field field = Employee.class.getDeclaredField(column);
            field.setAccessible(true);
            field.set(employee, value);
        } catch (ReflectiveOperationException exception) {
            employee.setDynamicField(column, value);
        }
    }

    public static String defaultLabel(String columnName) {
        String column = columnName == null ? "" : columnName.toUpperCase(Locale.ROOT);
        return DEFAULT_LABELS.getOrDefault(column, titleFromColumn(column));
    }

    public static List<EmployeeFieldDefinition> fallbackDefinitions() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < BASIC_COLUMNS.size(); index++) {
            definitions.add(fallbackDefinition(BASIC_COLUMNS.get(index), index));
        }
        return definitions;
    }

    private static EmployeeFieldDefinition fallbackDefinition(String column, int index) {
        return new EmployeeFieldDefinition(
                column,
                defaultLabel(column),
                FUNDAMENTALS_HEADING,
                false,
                false,
                true,
                false,
                isDateField(column),
                20 + index * 10,
                true,
                isComboField(column),
                isComboField(column),
                isMultilineField(column),
                String.join("\n", comboOptions(column, false)),
                true
        );
    }

    private static int basicOrder(String columnName) {
        if (columnName == null) {
            return Integer.MAX_VALUE;
        }
        int index = BASIC_COLUMNS.indexOf(columnName.toUpperCase(Locale.ROOT));
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static String headingKey(String heading) {
        return heading == null ? "" : heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static Map<String, String> defaultLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("EMPLOYEE_CODE", "Employee ID");
        labels.put("UNT_CODE", "Unit Code");
        labels.put("DESCR", "DESCR");
        labels.put("EMP_NAME", "Name");
        labels.put("FATHER_NAME", "Father Name");
        labels.put("NID", "CNIC");
        labels.put("EMP_CONTNO", "Phone");
        labels.put("PERSONAL_EMAIL", "Email");
        labels.put("DEPARTMENT", "Department");
        labels.put("DESIGNATION", "Designation");
        labels.put("SECTION", "Section");
        labels.put("GRADE", "Grade");
        labels.put("SHIFT", "Shift");
        labels.put("DOB", "Date of Birth");
        labels.put("GENDER", "Gender");
        labels.put("RESIGN_REASON", "Resign Reason");
        labels.put("JOINING_DATE", "Date of Joining");
        labels.put("RESIGN_DATE", "Date of Resignation");
        labels.put("CURRENT_ADR", "Current Address");
        labels.put("PERMANENT_ADR", "Permanent Address");
        return labels;
    }

    private static String titleFromColumn(String column) {
        String[] parts = column.toLowerCase(Locale.ROOT).split("_+");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return title.isEmpty() ? column : title.toString();
    }
}
