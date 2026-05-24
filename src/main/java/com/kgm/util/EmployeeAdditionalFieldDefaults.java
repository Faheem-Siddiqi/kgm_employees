package com.kgm.util;

import com.kgm.model.EmployeeFieldDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmployeeAdditionalFieldDefaults {
    private static final String YES_NO = "Yes\nNo";
    private static final List<FieldSpec> FIELDS = List.of(
            field("DEPT_CODE", "Department Code", "Organization / Structure", false, false, false, false, "", "02020500", 440),
            field("PAY_SHEET", "Pay Sheet", "Payroll / Allowances", false, true, true, false, "Monthly\nWeekly\nDaily", "Monthly", 790),
            field("H_RENT", "House Rent", "Payroll / Allowances", false, false, false, false, "", "27842", 800),
            field("H_MAINTENANCE", "House Maintenance", "Payroll / Allowances", false, false, false, false, "", "0", 810),
            field("CITY_VILLAGE", "City / Village", "Personal / HR Details", false, false, false, false, "", "Gujar Khan", 140),
            field("DISTRICT", "District", "Personal / HR Details", false, false, false, false, "", "Rawalpindi", 150),
            field("REST_DAY", "Rest Day", "Employment Details", false, true, false, false, "Monday\nTuesday\nWednesday\nThursday\nFriday\nSaturday\nSunday", "Sunday", 330),
            field("STAFF", "Staff", "Employment Details", false, true, false, false, YES_NO, "Yes", 340),
            field("SS", "Social Security", "Compliance / Status", false, true, false, false, YES_NO, "Yes", 1280),
            field("COLONY_RESIDENT", "Colony Resident", "Benefits / Housing", false, true, false, false, YES_NO, "No", 1380),
            field("DED_UNION", "Union Deduction", "Compliance / Status", false, true, false, false, YES_NO, "No", 1290),
            field("REFERENCE", "Reference", "Personal / HR Details", false, false, false, false, "", "Internal Reference", 160),
            field("RELATIVE_DETAIL", "Relative Detail", "Personal / HR Details", false, false, false, true, "", "Brother working in company", 170),
            field("REFEMP_NAME", "Reference Employee Name", "Personal / HR Details", false, false, false, false, "", "Ali Khan", 180),
            field("REFEMP_DESIG", "Reference Employee Designation", "Personal / HR Details", false, false, false, false, "", "Supervisor", 190),
            field("REFEMP_DEPT", "Reference Employee Department", "Personal / HR Details", false, false, false, false, "", "Human Resource", 200),
            field("COMPANY_CAR", "Company Car", "Benefits / Housing", false, true, false, false, YES_NO, "No", 1390),
            field("PRE_WORKEXP", "Previous Work Experience", "Employment Details", false, false, false, false, "", "3 Years", 350),
            field("PERSONAL_HOUSE_RENT", "Personal House Rent", "Benefits / Housing", false, false, false, false, "", "15000", 1400),
            field("COLONY_HOUSE_NUMBER", "Colony House Number", "Benefits / Housing", false, false, false, false, "", "B-12", 1410),
            field("CNIC_EXP_DATE", "CNIC Expiry Date", "Personal / HR Details", true, false, false, false, "", "12/31/2030 00:00:00", 210),
            field("CARDNO", "Card Number", "Employment Details", false, false, false, false, "", "100245", 360),
            field("CHEST_CARD_STATUS", "Chest Card Status", "Employment Details", false, true, true, false, "Issued\nPending\nNot Issued", "Issued", 370),
            field("EXTRA_DUTY_ALLOWANCE_DATE", "Extra Duty Allowance Date", "Payroll / Allowances", true, false, false, false, "", "01/15/2026 00:00:00", 820),
            field("CNIC_FAMILY_NO", "CNIC Family Number", "Personal / HR Details", false, false, false, false, "", "1234567", 220),
            field("REHIRING_STATUS", "Rehiring Status", "Employment Details", false, true, true, false, "New Hiring\nRehiring\nNot Eligible", "New Hiring", 380),
            field("CNIC_ISSUANCE_DATE", "CNIC Issuance Date", "Personal / HR Details", true, false, false, false, "", "01/01/2021 00:00:00", 230),
            field("REPORT_TO_EMP_ID", "Report To Employee ID", "Reporting", false, false, false, false, "", "000125", 1150),
            field("REPORT_TO_UNT", "Report To Unit", "Reporting", false, false, false, false, "", "KGM", 1160),
            field("TAILOR_CATEGORY", "Tailor Category", "Employment Details", false, true, true, false, "Regular\nContract\nTemporary", "Regular", 390),
            field("USER_ID", "User ID", "IT Access", false, false, false, false, "", "admin01", 1420),
            field("IT_EQUIPMENT", "IT Equipment", "IT Access", false, true, true, false, "Laptop\nDesktop\nTablet\nNone", "Laptop", 1430),
            field("IT_EMAIL", "IT Email Required", "IT Access", false, true, false, false, YES_NO, "Yes", 1440),
            field("IT_INTERNET", "Internet Access Required", "IT Access", false, true, false, false, YES_NO, "Yes", 1450),
            field("INTERNET_JUSTIFY", "Internet Access Justification", "IT Access", false, false, false, true, "", "Required for official email and ERP access", 1460),
            field("IT_SERVICE_ALERT", "IT Service Alert", "IT Access", false, true, false, false, YES_NO, "Yes", 1470),
            field("VAC_ID", "Vacancy ID", "Employment Details", false, false, false, false, "", "VAC-2026-001", 400),
            field("ALT_SAT_TEAM", "Alternate Saturday Team", "Alternate Saturday", false, true, true, false, "Team A\nTeam B\nTeam C", "Team A", 1480),
            field("ALT_SAT_START_DATE", "Alternate Saturday Start Date", "Alternate Saturday", true, false, false, false, "", "01/01/2026 00:00:00", 1490),
            field("ALT_SAT_END_DATE", "Alternate Saturday End Date", "Alternate Saturday", true, false, false, false, "", "12/31/2026 00:00:00", 1500),
            field("ALT_SAT_NEXT_YEAR", "Alternate Saturday Next Year", "Alternate Saturday", false, true, false, false, YES_NO, "Yes", 1510),
            field("ALT_SAT_SHUFFLE", "Alternate Saturday Shuffle", "Alternate Saturday", false, true, false, false, YES_NO, "No", 1520),
            field("ALT_SAT_UNLOCK_NEXT_YEAR", "Unlock Alternate Saturday Next Year", "Alternate Saturday", false, true, false, false, YES_NO, "No", 1530)
    );

    private EmployeeAdditionalFieldDefaults() {
    }

    public static List<EmployeeFieldDefinition> definitions() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (FieldSpec field : FIELDS) {
            definitions.add(new EmployeeFieldDefinition(
                    field.columnName(),
                    field.label(),
                    field.heading(),
                    false,
                    true,
                    false,
                    true,
                    field.dateField(),
                    field.sortOrder(),
                    false,
                    field.dropdownField(),
                    field.variableOptionField(),
                    field.textAreaField(),
                    field.dropdownOptions(),
                    false
            ));
        }
        return definitions;
    }

    public static List<String> columnNames() {
        List<String> columns = new ArrayList<>();
        for (FieldSpec field : FIELDS) {
            columns.add(field.columnName());
        }
        return columns;
    }

    public static boolean isSeededColumn(String columnName) {
        String normalized = normalizeColumn(columnName);
        for (FieldSpec field : FIELDS) {
            if (field.columnName().equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDateField(String columnName) {
        FieldSpec field = fieldSpec(columnName);
        return field != null && field.dateField();
    }

    public static String sampleValue(String columnName) {
        FieldSpec field = fieldSpec(columnName);
        return field == null ? null : field.sampleValue();
    }

    private static FieldSpec fieldSpec(String columnName) {
        String normalized = normalizeColumn(columnName);
        for (FieldSpec field : FIELDS) {
            if (field.columnName().equals(normalized)) {
                return field;
            }
        }
        return null;
    }

    private static FieldSpec field(
            String columnName,
            String label,
            String heading,
            boolean dateField,
            boolean dropdownField,
            boolean variableOptionField,
            boolean textAreaField,
            String dropdownOptions,
            String sampleValue,
            int sortOrder
    ) {
        return new FieldSpec(
                normalizeColumn(columnName),
                label,
                heading,
                dateField,
                dropdownField,
                variableOptionField,
                textAreaField,
                dropdownOptions,
                sampleValue,
                sortOrder
        );
    }

    private static String normalizeColumn(String columnName) {
        return columnName == null ? "" : columnName.trim().toUpperCase(Locale.ROOT);
    }

    private record FieldSpec(
            String columnName,
            String label,
            String heading,
            boolean dateField,
            boolean dropdownField,
            boolean variableOptionField,
            boolean textAreaField,
            String dropdownOptions,
            String sampleValue,
            int sortOrder
    ) {
    }
}
