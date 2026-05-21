package com.kgm.model;

public record EmployeeFieldDefinition(
        String columnName,
        String label,
        String heading,
        boolean documentField,
        boolean customField,
        boolean protectedField,
        boolean detailField,
        boolean dateField,
        int sortOrder
) {
    public String usageLabel() {
        if (documentField) {
            return "Documents";
        }
        if (detailField) {
            return "Details";
        }
        return "System";
    }

    public String sourceLabel() {
        return customField ? "Custom" : "Built-in";
    }
}
