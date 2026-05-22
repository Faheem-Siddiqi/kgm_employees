package com.kgm.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record EmployeeFieldDefinition(
        String columnName,
        String label,
        String heading,
        boolean documentField,
        boolean customField,
        boolean protectedField,
        boolean detailField,
        boolean dateField,
        int sortOrder,
        boolean coreField,
        boolean dropdownField,
        boolean variableOptionField,
        String dropdownOptions
) {
    public String usageLabel() {
        if (documentField) {
            return "Documents";
        }
        if (isFundamentalsHeading()) {
            return "Fundamentals";
        }
        if (coreField) {
            return "Core";
        }
        if (detailField) {
            return "Details";
        }
        return "System";
    }

    public String sourceLabel() {
        return customField ? "Custom" : "Built-in";
    }

    public List<String> dropdownOptionList() {
        List<String> options = new ArrayList<>();
        if (dropdownOptions == null || dropdownOptions.isBlank()) {
            return options;
        }

        for (String raw : dropdownOptions.split("[\\r\\n,]+")) {
            String option = raw.trim();
            if (option.isEmpty() || containsIgnoreCase(options, option)) {
                continue;
            }
            options.add(option);
        }
        return options;
    }

    public String normalizedColumnName() {
        return columnName == null ? "" : columnName.toUpperCase(Locale.ROOT);
    }

    private boolean isFundamentalsHeading() {
        String normalized = heading == null
                ? ""
                : heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.equals("fundamental") || normalized.equals("fundamentals");
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
