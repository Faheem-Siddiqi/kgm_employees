package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.util.EmployeeAdditionalFieldDefaults;
import com.kgm.util.EmployeeBasicFieldUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EmployeeFieldDefinitionDao {
    private static final String TABLE = "employee_field_metadata";
    public static final String FUNDAMENTALS_HEADING = "Fundamentals";
    private static final Set<String> RETIRED_FIELD_KEYS = Set.of("RRR");
    private static final Set<String> DEFAULT_REQUIRED_DOCUMENT_COLUMNS = Set.of(
            "CNIC_FRONT",
            "CNIC_BACK",
            "FINAL_SETTLEMENT",
            "APPOINTMENT_LETTER_FRONT",
            "APPOINTMENT_LETTER_BACK",
            "APPLICATION_FRONT",
            "APPLICATION_BACK",
            "CLEARANCE_CERTIFICATE",
            "EMP_IMG"
    );

    private static final List<EmployeeFieldDefinition> BUILT_IN_FIELDS = List.of(
            def("ID", "ID", "System", false, false, 1),
            def("EMPLOYEE_CODE", "Employee ID", FUNDAMENTALS_HEADING, false, false, 20),
            def("UNT_CODE", "Unit Code", FUNDAMENTALS_HEADING, false, false, 21),
            def("DESCR", "DESCR", FUNDAMENTALS_HEADING, false, false, 22),
            def("EMP_NAME", "Name", FUNDAMENTALS_HEADING, false, false, 30),
            def("FATHER_NAME", "Father Name", FUNDAMENTALS_HEADING, false, false, 40),
            def("MOTHER_NAME", "Mother Name", "Personal / HR Details", false, true, 50),
            def("GENDER", "Gender", FUNDAMENTALS_HEADING, false, false, 60),
            def("DOB", "Date of Birth", FUNDAMENTALS_HEADING, false, false, 70),
            def("CITY_OF_BIRTH", "City of Birth", "Personal / HR Details", false, true, 80),
            def("NATIONALITY", "Nationality", "Personal / HR Details", false, true, 90),
            def("RELIGION", "Religion", "Personal / HR Details", false, true, 100),
            def("BLOOD_GROUP", "Blood Group", "Personal / HR Details", false, true, 110),
            def("M_STATUS", "Marital Status", "Personal / HR Details", false, true, 120),
            def("NID", "CNIC", FUNDAMENTALS_HEADING, false, false, 130),

            def("DEPARTMENT", "Department", FUNDAMENTALS_HEADING, false, false, 200),
            def("DESIG_CODE", "Designation Code", "Employment Details", false, true, 210),
            def("DESIGNATION", "Designation", FUNDAMENTALS_HEADING, false, false, 220),
            def("SECTION", "Section", FUNDAMENTALS_HEADING, false, false, 225),
            def("GRADE", "Grade", FUNDAMENTALS_HEADING, false, false, 230),
            def("JOINING_DATE", "Date of Joining", FUNDAMENTALS_HEADING, false, false, 240),
            def("CONFIRMING_ON", "Confirming On", "Organization / Structure", false, true, 250),
            def("EMP_STATUS", "Employee Status", "Employment Details", false, true, 260),
            def("SHIFT", "Shift", FUNDAMENTALS_HEADING, false, false, 270),
            def("PROB_PERIOD", "Probation Period", "Organization / Structure", false, true, 280),
            def("EXP_IN_KTML", "Experience in KTML", "Employment Details", false, true, 290),
            def("APPLICATION_DATE", "Application Date", "Employment Details", false, true, 300),
            def("RESIGN_REASON", "Resign Reason", FUNDAMENTALS_HEADING, false, false, 310),
            def("RESIGN_DATE", "Date of Resignation", FUNDAMENTALS_HEADING, false, false, 320),

            def("ORG_ID", "ORG ID", "Organization / Structure", false, true, 400),
            def("DIVISION", "Division", "Organization / Structure", false, true, 410),
            def("BRANCH_CODE", "Branch Code", "Organization / Structure", false, true, 420),
            def("BRANCH_NAME", "Branch Name", "Organization / Structure", false, true, 430),

            def("GROSS_SALARY", "Gross Salary", "Payroll / Allowances", false, true, 500),
            def("PAY_CATEGORY", "Pay Category", "Payroll / Allowances", false, true, 510),
            def("BASIC", "Basic", "Payroll / Allowances", false, true, 520),
            def("COLA1", "COLA1", "Payroll / Allowances", false, true, 530),
            def("COLA2", "COLA2", "Payroll / Allowances", false, true, 540),
            def("COLA3", "COLA3", "Payroll / Allowances", false, true, 550),
            def("COLA4", "COLA4", "Payroll / Allowances", false, true, 560),
            def("COLA5", "COLA5", "Payroll / Allowances", false, true, 570),
            def("COLA6_7", "COLA6 / 7", "Payroll / Allowances", false, true, 580),
            def("COLA8", "COLA8", "Payroll / Allowances", false, true, 590),
            def("COLA9", "COLA9", "Payroll / Allowances", false, true, 600),
            def("COLA10", "COLA10", "Payroll / Allowances", false, true, 610),
            def("COLA11", "COLA11", "Payroll / Allowances", false, true, 620),
            def("PB_SPECIAL1_2", "PB Special 1 / 2", "Payroll / Allowances", false, true, 630),
            def("PB_SPECIAL3", "PB Special 3", "Payroll / Allowances", false, true, 640),
            def("PB_SPECIAL4", "PB Special 4", "Payroll / Allowances", false, true, 650),
            def("SPECIAL", "Special", "Payroll / Allowances", false, true, 660),
            def("OTHER1", "Other 1", "Payroll / Allowances", false, true, 670),
            def("OTHER2", "Other 2", "Payroll / Allowances", false, true, 680),
            def("OTHER3", "Other 3", "Payroll / Allowances", false, true, 690),
            def("MEDICAL", "Medical", "Payroll / Allowances", false, true, 700),
            def("CONVEYANCE", "Conveyance", "Payroll / Allowances", false, true, 710),
            def("UTILITY", "Utility", "Payroll / Allowances", false, true, 720),
            def("ENTERTAINMENT", "Entertainment", "Payroll / Allowances", false, true, 730),
            def("PAY_GROUP", "Pay Group", "Payroll / Allowances", false, true, 740),
            def("PAY_GROUP_DESC", "Pay Group Description", "Payroll / Allowances", false, true, 750),
            def("PAY_AT_JOINING", "Pay at Joining", "Payroll / Allowances", false, true, 760),
            def("EXTRA_DUTY", "Extra Duty", "Payroll / Allowances", false, true, 770),
            def("PAYROLL_FLAG", "Payroll Flag", "Payroll / Allowances", false, true, 780),

            def("BANK_NAME", "Bank Name", "Banking / Finance", false, true, 800),
            def("BANK_AC_NO", "Account No", "Banking / Finance", false, true, 810),
            def("SS_NO", "SS No", "Banking / Finance", false, true, 820),
            def("EOBI_NO", "EOBI No", "Banking / Finance", false, true, 830),
            def("TAX_NO", "Tax No", "Banking / Finance", false, true, 840),
            def("PFUND_DEDUCTION", "PFUND Deduction", "Banking / Finance", false, true, 850),
            def("PF_INTEREST", "PF Interest", "Banking / Finance", false, true, 860),
            def("PFUND_CODE", "PFUND Code", "Banking / Finance", false, true, 870),
            def("CLIPPER_PFUND_CODE", "Clipper PFUND Code", "Banking / Finance", false, true, 880),
            def("EFU", "EFU", "Banking / Finance", false, true, 890),
            def("EFU_NO", "EFU No", "Banking / Finance", false, true, 900),
            def("EOBI_STATUS", "EOBI Status", "Compliance / Status", false, true, 910),

            def("EMP_CONTNO", "Phone", FUNDAMENTALS_HEADING, false, false, 1000),
            def("CURRENT_ADR", "Current Address", FUNDAMENTALS_HEADING, false, false, true, 1010),
            def("PERMANENT_ADR", "Permanent Address", FUNDAMENTALS_HEADING, false, false, true, 1020),
            def("PERSONAL_EMAIL", "Email", FUNDAMENTALS_HEADING, false, false, 1030),
            def("OFFICIAL_EMAIL", "Official Email", "Contact", false, true, 1040),
            def("EMERGENCY_NO", "Emergency No", "Emergency / Misc", false, true, 1050),

            def("REP_UNT", "Reporting Unit", "Organization / Structure", false, true, 1100),
            def("REP_EMP_ID", "Reporting Employee ID", "Organization / Structure", false, true, 1110),
            def("REP_EMP_DESIG_CODE", "Reporting Designation Code", "Organization / Structure", false, true, 1120),
            def("REP_EMP_DEPT_CODE", "Reporting Department Code", "Organization / Structure", false, true, 1130),
            def("REP_EMP_TYPE", "Reporting Type", "Organization / Structure", false, true, 1140),

            def("FLAG", "Flag", "Compliance / Status", false, true, 1200),
            def("CLEARANCE_STATUS", "Clearance Status", "Compliance / Status", false, true, 1210),
            def("HOD_CHECK", "HOD Check", "Compliance / Status", false, true, 1220),
            def("SEC_HEAD_CHK", "Security Head Check", "Compliance / Status", false, true, 1230),
            def("NIC_VERIFY", "NIC Verify", "Compliance / Status", false, true, 1240),
            def("NIC_VERIFY_DATE", "NIC Verify Date", "Compliance / Status", false, true, 1250),
            def("ATT_CATEG", "Attendance Category", "Emergency / Misc", false, true, 1260),
            def("DIS_CERTIFICATE", "Disciplinary Certificate", "Compliance / Status", false, true, 1270),

            def("WELLNESS_CLUB", "Wellness Club", "Vaccination / Wellness", false, true, 1300),
            def("WELLNESS_CARD_ISSUE", "Wellness Card Issue", "Vaccination / Wellness", false, true, 1310),
            def("WELLNESS_CARD_NO", "Wellness Card No", "Vaccination / Wellness", false, true, 1320),
            def("WELLNESS_CLUB_VALID_DATE", "Wellness Valid Date", "Vaccination / Wellness", false, true, 1330),
            def("FIRST_DOSE", "First Dose", "Vaccination / Wellness", false, true, 1340),
            def("SECOND_DOSE", "Second Dose", "Vaccination / Wellness", false, true, 1350),
            def("FIRST_VACC_DATE", "First Vacc Date", "Vaccination / Wellness", false, true, 1360),
            def("SECOND_VACC_DATE", "Second Vacc Date", "Vaccination / Wellness", false, true, 1370),

            def("CNIC_FRONT", "CNIC Front", "Documents", true, false, 2000),
            def("CNIC_BACK", "CNIC Back", "Documents", true, false, 2010),
            def("EOBI", "EOBI", "Documents", true, false, 2020),
            def("SS_CARD", "Social Security Card", "Documents", true, false, 2030),
            def("FINAL_SETTLEMENT", "Final Settlement", "Documents", true, false, 2040),
            def("APPOINTMENT_LETTER_FRONT", "Appointment Letter Front", "Documents", true, false, 2050),
            def("APPOINTMENT_LETTER_BACK", "Appointment Letter Back", "Documents", true, false, 2060),
            def("APPLICATION_FRONT", "Application Front", "Documents", true, false, 2070),
            def("APPLICATION_BACK", "Application Back", "Documents", true, false, 2080),
            def("CLEARANCE_CERTIFICATE", "Clearance Certificate", "Documents", true, false, 2090),
            def("SERVICE_CERTIFICATE", "Service Certificate", "Documents", true, false, 2100),
            def("PAYMENT_VOUCHER", "Payment Voucher", "Documents", true, false, 2110),
            def("TRIAL_CARD", "Trial Card", "Documents", true, false, 2120),
            def("MEDICAL_DOC", "Medical Document", "Documents", true, false, 2130),
            def("INTERVIEW_FORMS", "Interview Forms", "Documents", true, false, 2140),
            def("COVID_CERTIFICATE", "Covid Certificate", "Documents", true, false, 2150),
            def("DISCIPLINARY_I", "Disciplinary I", "Documents", true, false, 2160),
            def("DISCIPLINARY_II", "Disciplinary II", "Documents", true, false, 2170),
            def("DISCIPLINARY_III", "Disciplinary III", "Documents", true, false, 2180),
            def("MISCELLANEOUS_I", "Miscellaneous I", "Documents", true, false, 2190),
            def("MISCELLANEOUS_II", "Miscellaneous II", "Documents", true, false, 2200),
            def("MISCELLANEOUS_III", "Miscellaneous III", "Documents", true, false, 2210),
            def("EMP_IMG", "Employee Photo", "Documents", true, false, 2220)
    );

    private final Connection conn;

    public EmployeeFieldDefinitionDao() {
        try {
            this.conn = DatabaseConnection.getConnection();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to connect to MySQL database.", exception);
        }
    }

    public EmployeeFieldDefinitionDao(Connection conn) {
        this.conn = conn;
    }

    public void ensureMetadata() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS employee_field_metadata (
                        column_name VARCHAR(64) NOT NULL,
                        display_label VARCHAR(160) NOT NULL,
                        heading VARCHAR(160) NOT NULL,
                        document_field TINYINT(1) NOT NULL DEFAULT 0,
                        custom_field TINYINT(1) NOT NULL DEFAULT 0,
                        protected_field TINYINT(1) NOT NULL DEFAULT 0,
                        detail_field TINYINT(1) NOT NULL DEFAULT 1,
                        date_field TINYINT(1) NOT NULL DEFAULT 0,
                        core_field TINYINT(1) NOT NULL DEFAULT 0,
                        dropdown_field TINYINT(1) NOT NULL DEFAULT 0,
                        variable_option_field TINYINT(1) NOT NULL DEFAULT 0,
                        text_area_field TINYINT(1) NOT NULL DEFAULT 0,
                        required_field TINYINT(1) NOT NULL DEFAULT 0,
                        dropdown_options TEXT,
                        sort_order INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (column_name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
        ensureMetadataColumn("date_field", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureMetadataColumn("core_field", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureMetadataColumn("dropdown_field", "TINYINT(1) NOT NULL DEFAULT 0");
        ensureMetadataColumn("variable_option_field", "TINYINT(1) NOT NULL DEFAULT 0");
        boolean textAreaFieldColumnAdded = ensureMetadataColumnAdded(
                "text_area_field",
                "TINYINT(1) NOT NULL DEFAULT 0"
        );
        ensureMetadataColumn("dropdown_options", "TEXT");
        boolean requiredFieldColumnAdded = ensureMetadataColumnAdded(
                "required_field",
                "TINYINT(1) NOT NULL DEFAULT 0"
        );

        removeRetiredFields();
        syncBuiltInMetadata();
        if (textAreaFieldColumnAdded) {
            seedDefaultTextAreaFields();
        }
        if (requiredFieldColumnAdded) {
            seedDefaultRequiredFields();
        }
    }

    public void syncMetadataWithDatabase() throws SQLException {
        ensureMetadata();
        Map<String, Integer> columns = employeeColumns();
        Map<String, EmployeeFieldDefinition> metadata = metadataByColumn();

        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            if (metadata.containsKey(column.getKey())) {
                continue;
            }
            EmployeeFieldDefinition inferred = new EmployeeFieldDefinition(
                    column.getKey(),
                    titleFromColumn(column.getKey()),
                    "Additional Details",
                    false,
                    true,
                    false,
                    true,
                    false,
                    3000 + column.getValue(),
                    false,
                    false,
                    false,
                    false,
                    "",
                    false
            );
            insertMetadataIgnore(inferred);
        }

        for (String column : metadata.keySet()) {
            if (columns.containsKey(column)) {
                continue;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM employee_field_metadata WHERE column_name = ?")) {
                ps.setString(1, column);
                ps.executeUpdate();
            }
        }
    }

    public List<EmployeeFieldDefinition> listFields() {
        try {
            syncMetadataWithDatabase();
            return new ArrayList<>(metadataByColumn().values());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load employee field settings.", exception);
        }
    }

    public List<EmployeeFieldDefinition> listDetailFields() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (EmployeeFieldDefinition definition : listFields()) {
            if (!definition.documentField()
                    && definition.detailField()
                    && !definition.coreField()
                    && !EmployeeBasicFieldUtil.isFundamentalsHeading(definition.heading())) {
                definitions.add(definition);
            }
        }
        definitions.sort(fieldOrder());
        return definitions;
    }

    public List<EmployeeFieldDefinition> listFundamentalsFields() {
        List<EmployeeFieldDefinition> list = new ArrayList<>();
        for (EmployeeFieldDefinition def : listFields()) {
            if (!def.documentField() && (def.coreField() || isFundamentalsHeading(def.heading()))) {
                list.add(def);
            }
        }
        list.sort(Comparator.comparingInt(EmployeeFieldDefinition::sortOrder));
        return list;
    }

    public List<EmployeeFieldDefinition> listDocumentFields() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (EmployeeFieldDefinition definition : listFields()) {
            if (definition.documentField() && definition.customField()) {
                definitions.add(definition);
            }
        }
        definitions.sort(fieldOrder());
        return definitions;
    }

    public List<String> listDetailHeadings() {
        Set<String> headings = new LinkedHashSet<>();
        for (EmployeeFieldDefinition definition : listFields()) {
            if (!definition.documentField()
                    && definition.detailField()
                    && !definition.coreField()
                    && !EmployeeBasicFieldUtil.isFundamentalsHeading(definition.heading())) {
                headings.add(definition.heading());
            }
        }
        headings.add(EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING);
        headings.add("Additional Details");
        return new ArrayList<>(headings);
    }

    public EmployeeFieldDefinition addField(String label, String heading, boolean documentField, boolean dateField) {
        return addField(label, heading, documentField, dateField, false, false, false, "");
    }

    public EmployeeFieldDefinition addField(
            String label,
            String heading,
            boolean documentField,
            boolean dateField,
            boolean dropdownField,
            boolean variableOptionField,
            boolean textAreaField,
            String dropdownOptions
    ) {
        String cleanLabel = requireText(label, "Field label is required.");
        String cleanHeading = documentField ? "Documents" : normalizeHeading(heading);
        if (!documentField && isFundamentalsHeading(cleanHeading)) {
            cleanHeading = EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING;
        }
        String column = toColumnName(cleanLabel);
        int sortOrder = nextSortOrder(cleanHeading);
        boolean fundamentalsField = isFundamentalsHeading(cleanHeading);
        boolean effectiveDateField = !documentField && dateField;
        boolean effectiveDropdownField = !documentField && dropdownField;
        boolean effectiveVariableOptionField = effectiveDropdownField && variableOptionField;
        boolean effectiveTextAreaField = !documentField && !effectiveDateField && !effectiveDropdownField && textAreaField;
        String cleanDropdownOptions = effectiveDropdownField ? normalizeOptions(dropdownOptions) : "";

        try (Statement stmt = conn.createStatement()) {
            ensureUniqueFieldIdentity(cleanLabel, column, null);
            stmt.execute("ALTER TABLE employees ADD COLUMN " + quoteIdentifier(column) + " TEXT");
            if (!documentField && !effectiveDateField) {
                fillMissingTextValues(column);
            }
            EmployeeFieldDefinition definition = new EmployeeFieldDefinition(
                    column,
                    cleanLabel,
                    cleanHeading,
                    documentField,
                    !documentField && !fundamentalsField,
                    !documentField && fundamentalsField,
                    !documentField && !fundamentalsField,
                    effectiveDateField,
                    sortOrder,
                    fundamentalsField,
                    effectiveDropdownField,
                    effectiveVariableOptionField,
                    effectiveTextAreaField,
                    cleanDropdownOptions,
                    fundamentalsField
            );
            insertMetadataReplace(definition);
            return definition;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to add employee field: " + exception.getMessage(), exception);
        }
    }

    public EmployeeFieldDefinition renameField(String currentColumn, String newLabel, String newHeading, boolean dateField) {
        EmployeeFieldDefinition current = requireMutableField(currentColumn, "Only custom fields can be renamed.");
        String cleanLabel = requireText(newLabel, "New field label is required.");
        String cleanHeading = current.documentField() ? current.heading() : normalizeHeading(newHeading);
        if (!current.documentField() && isFundamentalsHeading(cleanHeading)) {
            cleanHeading = EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING;
        }
        String newColumn = toColumnName(cleanLabel);
        boolean fundamentalsField = !current.documentField() && isFundamentalsHeading(cleanHeading);
        boolean effectiveDateField = !current.documentField() && dateField;

        try {
            ensureUniqueFieldIdentity(cleanLabel, newColumn, current.columnName());
            if (!current.columnName().equalsIgnoreCase(newColumn)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE employees CHANGE COLUMN "
                            + quoteIdentifier(current.columnName()) + " "
                            + quoteIdentifier(newColumn) + " TEXT");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM employee_field_metadata WHERE column_name = ?")) {
                ps.setString(1, current.columnName());
                ps.executeUpdate();
            }

            EmployeeFieldDefinition renamed = new EmployeeFieldDefinition(
                    newColumn,
                    cleanLabel,
                    cleanHeading,
                    current.documentField(),
                    !current.documentField() && !fundamentalsField,
                    current.documentField() || fundamentalsField,
                    !current.documentField() && !fundamentalsField,
                    effectiveDateField,
                    current.sortOrder(),
                    fundamentalsField || current.coreField(),
                    current.dropdownField(),
                    current.variableOptionField(),
                    current.textAreaField(),
                    current.dropdownOptions(),
                    current.requiredField()
            );
            insertMetadataReplace(renamed);
            applyValueDefaultsForType(newColumn, renamed.documentField(), effectiveDateField, renamed.dropdownField());
            return renamed;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to rename employee field: " + exception.getMessage(), exception);
        }
    }

    public EmployeeFieldDefinition updateFieldSettings(String columnName, String label, String heading, boolean dateField) {
        EmployeeFieldDefinition current = requireExistingField(columnName);
        return updateFieldSettings(
                columnName,
                label,
                heading,
                dateField,
                current.dropdownField(),
                current.variableOptionField(),
                current.textAreaField(),
                current.dropdownOptions()
        );
    }

    public EmployeeFieldDefinition updateFieldSettings(
            String columnName,
            String label,
            String heading,
            boolean dateField,
            boolean dropdownField,
            boolean variableOptionField,
            boolean textAreaField,
            String dropdownOptions
    ) {
        EmployeeFieldDefinition current = requireExistingField(columnName);
        String cleanLabel = requireText(label, "Field label is required.");
        try {
            ensureUniqueFieldIdentity(cleanLabel, current.columnName(), current.columnName());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to check field uniqueness: " + exception.getMessage(), exception);
        }
        String cleanHeading = current.documentField() ? current.heading() : normalizeHeading(heading);
        if (!current.documentField() && isFundamentalsHeading(cleanHeading)) {
            cleanHeading = EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING;
        }
        boolean fundamentalsField = !current.documentField() && isFundamentalsHeading(cleanHeading);
        boolean protectedSystemField = current.protectedField() && !current.customField();
        boolean effectiveDateField = !current.documentField() && dateField;
        boolean effectiveDropdownField = !current.documentField() && dropdownField;
        boolean effectiveVariableOptionField = effectiveDropdownField && variableOptionField;
        boolean effectiveTextAreaField = !current.documentField()
                && !effectiveDateField
                && !effectiveDropdownField
                && textAreaField;
        String cleanDropdownOptions = effectiveDropdownField ? normalizeOptions(dropdownOptions) : "";
        boolean effectiveProtectedField = current.documentField() || fundamentalsField || protectedSystemField;
        boolean effectiveCustomField = !effectiveProtectedField;
        boolean effectiveDetailField = !current.documentField() && !fundamentalsField;
        boolean effectiveCoreField = fundamentalsField || current.coreField();

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE employee_field_metadata
                SET display_label = ?, heading = ?, custom_field = ?, protected_field = ?,
                    detail_field = ?, core_field = ?, date_field = ?, dropdown_field = ?,
                    variable_option_field = ?, text_area_field = ?, dropdown_options = ?
                WHERE column_name = ?
                """)) {
            ps.setString(1, cleanLabel);
            ps.setString(2, cleanHeading);
            ps.setBoolean(3, effectiveCustomField);
            ps.setBoolean(4, effectiveProtectedField);
            ps.setBoolean(5, effectiveDetailField);
            ps.setBoolean(6, effectiveCoreField);
            ps.setBoolean(7, effectiveDateField);
            ps.setBoolean(8, effectiveDropdownField);
            ps.setBoolean(9, effectiveVariableOptionField);
            ps.setBoolean(10, effectiveTextAreaField);
            ps.setString(11, cleanDropdownOptions);
            ps.setString(12, current.columnName());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update employee field: " + exception.getMessage(), exception);
        }

        try {
            applyValueDefaultsForType(
                    current.columnName(),
                    current.documentField(),
                    effectiveDateField,
                    effectiveDropdownField
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update employee field defaults: " + exception.getMessage(), exception);
        }

        return new EmployeeFieldDefinition(
                current.columnName(),
                cleanLabel,
                cleanHeading,
                current.documentField(),
                effectiveCustomField,
                effectiveProtectedField,
                effectiveDetailField,
                effectiveDateField,
                current.sortOrder(),
                effectiveCoreField,
                effectiveDropdownField,
                effectiveVariableOptionField,
                effectiveTextAreaField,
                cleanDropdownOptions,
                current.requiredField()
        );
    }

    public EmployeeFieldDefinition updateRequiredField(String columnName, boolean required) {
        EmployeeFieldDefinition current = requireExistingField(columnName);
        if ("ID".equalsIgnoreCase(current.columnName())) {
            throw new IllegalArgumentException("ID cannot be marked as a required employee field.");
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE employee_field_metadata
                SET required_field = ?
                WHERE column_name = ?
                """)) {
            ps.setBoolean(1, required);
            ps.setString(2, current.columnName());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update required field setting: " + exception.getMessage(), exception);
        }
        return new EmployeeFieldDefinition(
                current.columnName(),
                current.label(),
                current.heading(),
                current.documentField(),
                current.customField(),
                current.protectedField(),
                current.detailField(),
                current.dateField(),
                current.sortOrder(),
                current.coreField(),
                current.dropdownField(),
                current.variableOptionField(),
                current.textAreaField(),
                current.dropdownOptions(),
                required
        );
    }

    public int renameHeading(String currentHeading, String newHeading) {
        String oldHeading = requireText(currentHeading, "Current category is required.");
        String cleanHeading = normalizeHeading(newHeading);
        if (isFundamentalsHeading(cleanHeading)) {
            cleanHeading = EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING;
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE employee_field_metadata
                SET heading = ?
                WHERE UPPER(heading) = UPPER(?)
                """)) {
            ps.setString(1, cleanHeading);
            ps.setString(2, oldHeading);
            int updated = ps.executeUpdate();
            if (isFundamentalsHeading(cleanHeading)) {
                promoteFundamentalsFields();
            }
            return updated;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to rename category: " + exception.getMessage(), exception);
        }
    }

    private void promoteFundamentalsFields() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE employee_field_metadata
                SET custom_field = 0, protected_field = 1, detail_field = 0, core_field = 1
                WHERE document_field = 0
                  AND %s
                """.formatted(fundamentalsHeadingSqlCondition("heading")))) {
            ps.executeUpdate();
        }
    }

    private void removeRetiredFields() throws SQLException {
        Set<String> columnsToDrop = new LinkedHashSet<>();
        for (String column : employeeColumns().keySet()) {
            if (isRetiredFieldKey(column)) {
                columnsToDrop.add(column);
            }
        }
        for (EmployeeFieldDefinition definition : metadataByColumn().values()) {
            if (isRetiredFieldKey(definition.columnName()) || isRetiredFieldKey(definition.label())) {
                columnsToDrop.add(definition.columnName().toUpperCase(Locale.ROOT));
            }
        }

        for (String column : columnsToDrop) {
            if (columnExists(column)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE employees DROP COLUMN " + quoteIdentifier(column));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM employee_field_metadata WHERE UPPER(column_name) = UPPER(?)")) {
                ps.setString(1, column);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM employee_field_metadata WHERE UPPER(TRIM(display_label)) = UPPER(?)")) {
            for (String key : RETIRED_FIELD_KEYS) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
        }
    }

    private static boolean isRetiredFieldKey(String value) {
        if (value == null) {
            return false;
        }
        String key = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return RETIRED_FIELD_KEYS.contains(key);
    }

    private static String fundamentalsHeadingSqlCondition(String columnExpression) {
        String keyExpression = "UPPER(REPLACE(REPLACE(REPLACE(TRIM(" + columnExpression
                + "), ' ', ''), '-', ''), '_', ''))";
        return keyExpression + " IN ('FUNDAMENTAL', 'FUNDAMENTALS')";
    }

    public int deleteHeading(String heading) {
        String cleanHeading = requireText(heading, "Category is required.");
        List<EmployeeFieldDefinition> fields = new ArrayList<>();
        for (EmployeeFieldDefinition definition : listFields()) {
            if (definition.heading().equalsIgnoreCase(cleanHeading)) {
                fields.add(definition);
            }
        }
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Category was not found: " + cleanHeading);
        }
        for (EmployeeFieldDefinition definition : fields) {
            if (!definition.customField() || definition.protectedField()) {
                throw new IllegalArgumentException("Only categories with custom fields can be deleted.");
            }
        }

        try (Statement stmt = conn.createStatement()) {
            for (EmployeeFieldDefinition definition : fields) {
                stmt.execute("ALTER TABLE employees DROP COLUMN " + quoteIdentifier(definition.columnName()));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete category fields: " + exception.getMessage(), exception);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM employee_field_metadata WHERE UPPER(heading) = UPPER(?)")) {
            ps.setString(1, cleanHeading);
            return ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Category columns were deleted, but metadata could not be removed.", exception);
        }
    }

    public void deleteField(String columnName) {
        EmployeeFieldDefinition definition = requireMutableField(columnName, "Only custom fields can be deleted.");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE employees DROP COLUMN " + quoteIdentifier(definition.columnName()));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete employee field: " + exception.getMessage(), exception);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM employee_field_metadata WHERE column_name = ?")) {
            ps.setString(1, definition.columnName());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Field column was deleted, but its metadata could not be removed.", exception);
        }
    }

    public void updateDateField(String columnName, boolean dateField) {
        String cleanColumn = requireText(columnName, "Field column is required.").toUpperCase(Locale.ROOT);
        EmployeeFieldDefinition definition = null;
        for (EmployeeFieldDefinition field : listFields()) {
            if (field.columnName().equalsIgnoreCase(cleanColumn)) {
                definition = field;
                break;
            }
        }
        if (definition == null) {
            throw new IllegalArgumentException("Field was not found: " + cleanColumn);
        }
        boolean effectiveDateField = !definition.documentField() && dateField;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employee_field_metadata SET date_field = ?, text_area_field = CASE WHEN ? THEN 0 ELSE text_area_field END WHERE column_name = ?")) {
            ps.setBoolean(1, effectiveDateField);
            ps.setBoolean(2, effectiveDateField);
            ps.setString(3, definition.columnName());
            ps.executeUpdate();
            applyValueDefaultsForType(definition.columnName(), definition.documentField(), effectiveDateField);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update date field setting: " + exception.getMessage(), exception);
        }
    }

    public void applyCustomFieldDefaultsForEmployee(String employeeCode) {
        String cleanCode = requireText(employeeCode, "Employee code is required.");
        try {
            for (EmployeeFieldDefinition definition : listFields()) {
                if (definition.documentField() || definition.dateField()) {
                    continue;
                }
                fillMissingTextValueForEmployee(definition.columnName(), cleanCode);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to apply employee field defaults: " + exception.getMessage(), exception);
        }
    }

    public void applyCustomFieldDefaultsForEmployees(List<String> employeeCodes) {
        List<String> cleanCodes = new ArrayList<>();
        for (String employeeCode : employeeCodes == null ? List.<String>of() : employeeCodes) {
            String cleanCode = employeeCode == null ? "" : employeeCode.trim();
            if (!cleanCode.isEmpty() && !containsIgnoreCase(cleanCodes, cleanCode)) {
                cleanCodes.add(cleanCode);
            }
        }
        if (cleanCodes.isEmpty()) {
            return;
        }

        try {
            List<EmployeeFieldDefinition> definitions = listFields();
            for (EmployeeFieldDefinition definition : definitions) {
                if (definition.documentField() || definition.dateField()) {
                    continue;
                }
                fillMissingTextValueForEmployees(definition.columnName(), cleanCodes);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to apply employee field defaults: " + exception.getMessage(), exception);
        }
    }

    private EmployeeFieldDefinition requireExistingField(String columnName) {
        String cleanColumn = requireText(columnName, "Field column is required.").toUpperCase(Locale.ROOT);
        for (EmployeeFieldDefinition definition : listFields()) {
            if (definition.columnName().equalsIgnoreCase(cleanColumn)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Field was not found: " + cleanColumn);
    }

    private EmployeeFieldDefinition requireMutableField(String columnName, String message) {
        String cleanColumn = requireText(columnName, "Field column is required.").toUpperCase(Locale.ROOT);
        for (EmployeeFieldDefinition definition : listFields()) {
            if (definition.columnName().equalsIgnoreCase(cleanColumn)) {
                if (!definition.customField() || definition.protectedField()) {
                    throw new IllegalArgumentException(message);
                }
                return definition;
            }
        }
        throw new IllegalArgumentException("Field was not found: " + cleanColumn);
    }

    private Map<String, EmployeeFieldDefinition> metadataByColumn() throws SQLException {
        Map<String, EmployeeFieldDefinition> fields = new LinkedHashMap<>();
        String sql = """
                SELECT column_name, display_label, heading, document_field, custom_field,
                       protected_field, detail_field, date_field, sort_order,
                       core_field, dropdown_field, variable_option_field, text_area_field, dropdown_options, required_field
                FROM employee_field_metadata
                ORDER BY document_field, heading, sort_order, display_label
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                EmployeeFieldDefinition definition = new EmployeeFieldDefinition(
                        rs.getString("column_name"),
                        rs.getString("display_label"),
                        rs.getString("heading"),
                        rs.getBoolean("document_field"),
                        rs.getBoolean("custom_field"),
                        rs.getBoolean("protected_field"),
                        rs.getBoolean("detail_field"),
                        rs.getBoolean("date_field"),
                        rs.getInt("sort_order"),
                        rs.getBoolean("core_field"),
                        rs.getBoolean("dropdown_field"),
                        rs.getBoolean("variable_option_field"),
                        rs.getBoolean("text_area_field"),
                        rs.getString("dropdown_options"),
                        rs.getBoolean("required_field")
                );
                fields.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
            }
        }
        return fields;
    }

    private void syncBuiltInMetadata() throws SQLException {
        Map<String, EmployeeFieldDefinition> existingFields = metadataByColumn();
        for (EmployeeFieldDefinition builtIn : seededFieldDefinitions()) {
            syncSeededField(existingFields, builtIn);
        }
        demoteNonCoreDetailMetadata();
        promoteFundamentalsFields();
    }

    private List<EmployeeFieldDefinition> seededFieldDefinitions() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>(BUILT_IN_FIELDS);
        definitions.addAll(EmployeeAdditionalFieldDefaults.definitions());
        return definitions;
    }

    private void syncSeededField(
            Map<String, EmployeeFieldDefinition> existingFields,
            EmployeeFieldDefinition builtIn
    ) throws SQLException {
        String column = builtIn.columnName().toUpperCase(Locale.ROOT);
        boolean core = isCoreColumn(column);
        boolean document = builtIn.documentField();
        boolean internal = isInternalColumn(column);
        if (!core && !document && !internal) {
            EmployeeFieldDefinition existing = existingFields.get(column);
            EmployeeFieldDefinition knownCustom = customDetailDefinition(builtIn, existing);
            if (existing == null) {
                insertMetadataIgnore(knownCustom);
            } else {
                insertMetadataReplace(knownCustom);
            }
            if (EmployeeAdditionalFieldDefaults.isSeededColumn(column)) {
                applyValueDefaultsForType(
                        knownCustom.columnName(),
                        knownCustom.documentField(),
                        knownCustom.dateField(),
                        knownCustom.dropdownField()
                );
            }
            return;
        }

        EmployeeFieldDefinition existing = existingFields.get(column);
        if (existing == null) {
            insertMetadataIgnore(systemDefinition(builtIn, null, core, document, internal));
            return;
        }

        insertMetadataReplace(systemDefinition(builtIn, existing, core, document, internal));
    }

    private void seedDefaultRequiredFields() throws SQLException {
        List<String> requiredColumns = new ArrayList<>();
        requiredColumns.addAll(EmployeeBasicFieldUtil.BASIC_COLUMNS);
        requiredColumns.addAll(DEFAULT_REQUIRED_DOCUMENT_COLUMNS);
        String sql = "UPDATE employee_field_metadata SET required_field = 1 WHERE UPPER(column_name) IN ("
                + placeholders(requiredColumns.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int index = 0; index < requiredColumns.size(); index++) {
                ps.setString(index + 1, requiredColumns.get(index));
            }
            ps.executeUpdate();
        }
    }

    private void seedDefaultTextAreaFields() throws SQLException {
        List<String> textAreaColumns = List.of("CURRENT_ADR", "PERMANENT_ADR");
        String sql = "UPDATE employee_field_metadata SET text_area_field = 1 WHERE UPPER(column_name) IN ("
                + placeholders(textAreaColumns.size()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int index = 0; index < textAreaColumns.size(); index++) {
                ps.setString(index + 1, textAreaColumns.get(index));
            }
            ps.executeUpdate();
        }
    }

    private EmployeeFieldDefinition customDetailDefinition(
            EmployeeFieldDefinition builtIn,
            EmployeeFieldDefinition existing
    ) {
        String column = builtIn.columnName().toUpperCase(Locale.ROOT);
        boolean seededAdditional = EmployeeAdditionalFieldDefaults.isSeededColumn(column);
        String label = seededAdditional || existing == null || isBlank(existing.label())
                ? builtIn.label()
                : existing.label();
        String heading = seededAdditional || existing == null || isBlank(existing.heading())
                ? builtIn.heading()
                : existing.heading();
        if (isFundamentalsHeading(heading)) {
            heading = EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING;
        }
        boolean fundamentalsField = isFundamentalsHeading(heading);
        boolean dateField = builtIn.dateField() || (existing != null && existing.dateField());
        boolean dropdownField = !dateField && (builtIn.dropdownField() || (existing != null && existing.dropdownField()));
        boolean variableOptionField = dropdownField
                && (builtIn.variableOptionField() || (existing != null && existing.variableOptionField()));
        boolean textAreaField = !dateField && !dropdownField && (
                builtIn.textAreaField()
                        || (existing != null ? existing.textAreaField() : defaultTextAreaField(builtIn.columnName()))
        );
        String dropdownOptions = dropdownField
                ? !isBlank(builtIn.dropdownOptions())
                        ? normalizeOptions(builtIn.dropdownOptions())
                        : existing == null ? "" : normalizeOptions(existing.dropdownOptions())
                : "";

        return new EmployeeFieldDefinition(
                builtIn.columnName(),
                label,
                heading,
                false,
                !fundamentalsField,
                fundamentalsField,
                !fundamentalsField,
                dateField,
                builtIn.sortOrder(),
                fundamentalsField,
                dropdownField,
                variableOptionField,
                textAreaField,
                dropdownOptions,
                existing != null && existing.requiredField()
        );
    }

    private EmployeeFieldDefinition systemDefinition(
            EmployeeFieldDefinition builtIn,
            EmployeeFieldDefinition existing,
            boolean core,
            boolean document,
            boolean internal
    ) {
        String column = builtIn.columnName().toUpperCase(Locale.ROOT);
        String label = existing == null ? builtIn.label() : builtInLabel(existing, builtIn);
        String heading = existing == null ? builtIn.heading() : builtInHeading(existing, builtIn);
        if (isFundamentalsHeading(heading)) {
            heading = EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING;
        }
        boolean defaultDateField = EmployeeBasicFieldUtil.DATE_COLUMNS.contains(column);
        boolean defaultDropdownField = isDefaultDropdownColumn(column);
        boolean defaultTextAreaField = defaultTextAreaField(column);
        String defaultOptions = defaultDropdownOptions(column);

        boolean dateField = document
                ? false
                : existing == null || !existing.coreField()
                        ? defaultDateField || (existing != null && existing.dateField())
                        : existing.dateField();
        boolean dropdownField = document
                ? false
                : existing == null || !existing.coreField()
                        ? defaultDropdownField || (existing != null && existing.dropdownField())
                        : existing.dropdownField();
        boolean variableOptionField = dropdownField && (
                existing == null || !existing.coreField()
                        ? defaultDropdownField || (existing != null && existing.variableOptionField())
                        : existing.variableOptionField()
        );
        String dropdownOptions = dropdownField
                ? existing == null || isBlank(existing.dropdownOptions())
                        ? defaultOptions
                        : normalizeOptions(existing.dropdownOptions())
                : "";
        boolean textAreaField = document || dateField || dropdownField
                ? false
                : existing == null
                        ? defaultTextAreaField
                        : existing.textAreaField();
        boolean requiredField = existing == null
                ? defaultRequiredField(builtIn)
                : existing.requiredField();

        return new EmployeeFieldDefinition(
                builtIn.columnName(),
                label,
                heading,
                document,
                false,
                true,
                !document && !core && !internal && builtIn.detailField(),
                dateField,
                builtIn.sortOrder(),
                core,
                dropdownField,
                variableOptionField,
                textAreaField,
                dropdownOptions,
                requiredField
        );
    }

    private void demoteNonCoreDetailMetadata() throws SQLException {
        List<String> coreColumns = EmployeeBasicFieldUtil.BASIC_COLUMNS;
        String placeholders = String.join(", ", coreColumns.stream().map(ignored -> "?").toList());
        String sql = """
                UPDATE employee_field_metadata
                SET custom_field = 1, protected_field = 0, detail_field = 1, core_field = 0
                WHERE document_field = 0
                  AND UPPER(column_name) <> 'ID'
                  AND UPPER(heading) <> UPPER(?)
                  AND UPPER(column_name) NOT IN (%s)
                """.formatted(placeholders);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EmployeeBasicFieldUtil.FUNDAMENTALS_HEADING);
            for (int index = 0; index < coreColumns.size(); index++) {
                ps.setString(index + 2, coreColumns.get(index));
            }
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE employee_field_metadata
                SET custom_field = 0, protected_field = 1, detail_field = 0, core_field = 0,
                    dropdown_field = 0, variable_option_field = 0, text_area_field = 0, dropdown_options = ''
                WHERE UPPER(column_name) = 'ID'
                """)) {
            ps.executeUpdate();
        }
    }

    private String builtInLabel(EmployeeFieldDefinition existing, EmployeeFieldDefinition builtIn) {
        if (existing.customField() || isBlank(existing.label())) {
            return builtIn.label();
        }
        String column = builtIn.columnName().toUpperCase(Locale.ROOT);
        String current = existing.label().trim();
        if (isLegacyBasicLabel(column, current)) {
            return builtIn.label();
        }
        return current;
    }

    private boolean defaultRequiredField(EmployeeFieldDefinition builtIn) {
        String column = builtIn.columnName().toUpperCase(Locale.ROOT);
        if (builtIn.documentField()) {
            return DEFAULT_REQUIRED_DOCUMENT_COLUMNS.contains(column);
        }
        return EmployeeBasicFieldUtil.BASIC_COLUMNS.contains(column)
                || isFundamentalsHeading(builtIn.heading());
    }

    private String builtInHeading(EmployeeFieldDefinition existing, EmployeeFieldDefinition builtIn) {
        if (existing.customField() || isBlank(existing.heading())) {
            return builtIn.heading();
        }
        String column = builtIn.columnName().toUpperCase(Locale.ROOT);
        String current = existing.heading().trim();
        if (isLegacyBasicHeading(column, current)) {
            return builtIn.heading();
        }
        return current;
    }

    private boolean isLegacyBasicLabel(String column, String label) {
        String normalized = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (column) {
            case "EMPLOYEE_CODE" -> normalized.equals("employeecode");
            case "EMP_NAME" -> normalized.equals("employeename");
            case "NID" -> normalized.equals("cnicnid") || normalized.equals("nid");
            case "EMP_CONTNO" -> normalized.equals("contactnumber");
            case "PERSONAL_EMAIL" -> normalized.equals("personalemail");
            case "JOINING_DATE" -> normalized.equals("joiningdate") || normalized.equals("dateofarrival");
            case "RESIGN_DATE" -> normalized.equals("resigndate")
                    || normalized.equals("leavingdate")
                    || normalized.equals("dateofleaving");
            default -> false;
        };
    }

    private boolean isLegacyBasicHeading(String column, String heading) {
        String normalized = heading.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (EmployeeBasicFieldUtil.isBasicField(column)
                && (normalized.equals("coreregistration") || normalized.equals("basic"))) {
            return true;
        }
        return switch (column) {
            case "EMPLOYEE_CODE" -> normalized.equals("system");
            case "DOB" -> normalized.equals("personalhrdetails");
            case "GRADE" -> normalized.equals("employmentdetails");
            case "SHIFT" -> normalized.equals("organizationstructure");
            default -> false;
        };
    }

    private boolean isCoreColumn(String column) {
        return EmployeeBasicFieldUtil.isBasicField(column);
    }

    private boolean isInternalColumn(String column) {
        return "ID".equalsIgnoreCase(column);
    }

    private boolean isDefaultDropdownColumn(String column) {
        return "GENDER".equalsIgnoreCase(column) || "RESIGN_REASON".equalsIgnoreCase(column);
    }

    private boolean defaultTextAreaField(String column) {
        return "CURRENT_ADR".equalsIgnoreCase(column) || "PERMANENT_ADR".equalsIgnoreCase(column);
    }

    private String defaultDropdownOptions(String column) {
        if ("GENDER".equalsIgnoreCase(column)) {
            return "Male\nFemale\nOther";
        }
        if ("RESIGN_REASON".equalsIgnoreCase(column)) {
            return "Layoff\nRetirement\nOther";
        }
        return "";
    }

    private Map<String, Integer> employeeColumns() throws SQLException {
        Map<String, Integer> columns = new LinkedHashMap<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, "employees", null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                int ordinal = rs.getInt("ORDINAL_POSITION");
                columns.put(column.toUpperCase(Locale.ROOT), ordinal);
            }
        }
        return columns;
    }

    private boolean columnExists(String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, "employees", columnName)) {
            if (rs.next()) {
                return true;
            }
        }
        for (String existing : employeeColumns().keySet()) {
            if (existing.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private void ensureUniqueFieldIdentity(
            String label,
            String columnName,
            String currentColumn
    ) throws SQLException {
        String labelKey = normalizeIdentity(label);
        String column = columnName.toUpperCase(Locale.ROOT);
        String current = currentColumn == null ? "" : currentColumn.toUpperCase(Locale.ROOT);

        for (EmployeeFieldDefinition definition : metadataByColumn().values()) {
            String existingColumn = definition.columnName().toUpperCase(Locale.ROOT);
            if (existingColumn.equals(current)) {
                continue;
            }
            if (normalizeIdentity(definition.label()).equals(labelKey)) {
                throw new IllegalArgumentException(
                        "A field with this label already exists: "
                                + definition.label()
                                + ". Use a unique field label."
                );
            }
            if (existingColumn.equals(column)) {
                throw new IllegalArgumentException(
                        "A database column with this name already exists: "
                                + column
                                + ". Use a different field label."
                );
            }
        }

        if ((current.isBlank() || !current.equals(column)) && columnExists(column)) {
            throw new IllegalArgumentException(
                    "A database column with this name already exists: "
                            + column
                            + ". Use a different field label."
            );
        }
    }

    private void insertMetadataIgnore(EmployeeFieldDefinition definition) throws SQLException {
        String sql = """
                INSERT IGNORE INTO employee_field_metadata
                    (column_name, display_label, heading, document_field, custom_field,
                     protected_field, detail_field, date_field, sort_order,
                     core_field, dropdown_field, variable_option_field, text_area_field, dropdown_options, required_field)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        writeMetadata(definition, sql);
    }

    private void insertMetadataReplace(EmployeeFieldDefinition definition) throws SQLException {
        String sql = """
                REPLACE INTO employee_field_metadata
                    (column_name, display_label, heading, document_field, custom_field,
                     protected_field, detail_field, date_field, sort_order,
                     core_field, dropdown_field, variable_option_field, text_area_field, dropdown_options, required_field)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        writeMetadata(definition, sql);
    }

    private void writeMetadata(EmployeeFieldDefinition definition, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, definition.columnName());
            ps.setString(2, definition.label());
            ps.setString(3, definition.heading());
            ps.setBoolean(4, definition.documentField());
            ps.setBoolean(5, definition.customField());
            ps.setBoolean(6, definition.protectedField());
            ps.setBoolean(7, definition.detailField());
            ps.setBoolean(8, definition.dateField());
            ps.setInt(9, definition.sortOrder());
            ps.setBoolean(10, definition.coreField());
            ps.setBoolean(11, definition.dropdownField());
            ps.setBoolean(12, definition.variableOptionField());
            ps.setBoolean(13, definition.textAreaField());
            ps.setString(14, normalizeOptions(definition.dropdownOptions()));
            ps.setBoolean(15, definition.requiredField());
            ps.executeUpdate();
        }
    }

    private void ensureMetadataColumn(String columnName, String ddl) throws SQLException {
        ensureMetadataColumnAdded(columnName, ddl);
    }

    private boolean ensureMetadataColumnAdded(String columnName, String ddl) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, TABLE, columnName)) {
            if (rs.next()) {
                return false;
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + quoteIdentifier(columnName) + " " + ddl);
        }
        return true;
    }

    private void applyValueDefaultsForType(String columnName, boolean documentField, boolean dateField) throws SQLException {
        applyValueDefaultsForType(columnName, documentField, dateField, false);
    }

    private void applyValueDefaultsForType(
            String columnName,
            boolean documentField,
            boolean dateField,
            boolean dropdownField
    ) throws SQLException {
        if (documentField) {
            return;
        }
        if (dateField) {
            clearPlaceholderValues(columnName);
        } else {
            fillMissingTextValues(columnName);
        }
    }

    private void fillMissingTextValues(String columnName) throws SQLException {
        String quoted = quoteIdentifier(columnName);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employees SET " + quoted + " = 'N/A' WHERE " + missingValueCondition(quoted))) {
            ps.executeUpdate();
        }
    }

    private void fillMissingTextValueForEmployee(String columnName, String employeeCode) throws SQLException {
        String quoted = quoteIdentifier(columnName);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employees SET " + quoted + " = 'N/A' WHERE EMPLOYEE_CODE = ? AND ("
                        + missingValueCondition(quoted) + ")")) {
            ps.setString(1, employeeCode);
            ps.executeUpdate();
        }
    }

    private void fillMissingTextValueForEmployees(String columnName, List<String> employeeCodes) throws SQLException {
        String quoted = quoteIdentifier(columnName);
        for (int start = 0; start < employeeCodes.size(); start += 500) {
            List<String> batch = employeeCodes.subList(start, Math.min(start + 500, employeeCodes.size()));
            String sql = "UPDATE employees SET " + quoted + " = 'N/A' WHERE EMPLOYEE_CODE IN ("
                    + placeholders(batch.size()) + ") AND (" + missingValueCondition(quoted) + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int index = 0; index < batch.size(); index++) {
                    ps.setString(index + 1, batch.get(index));
                }
                ps.executeUpdate();
            }
        }
    }

    private void clearPlaceholderValues(String columnName) throws SQLException {
        String quoted = quoteIdentifier(columnName);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE employees SET " + quoted + " = NULL WHERE " + missingValueCondition(quoted))) {
            ps.executeUpdate();
        }
    }

    private static String missingValueCondition(String quotedColumn) {
        return quotedColumn + " IS NULL OR TRIM(" + quotedColumn + ") = '' OR UPPER(TRIM(" + quotedColumn
                + ")) IN ('N/A', 'NA', 'NULL') OR TRIM(" + quotedColumn + ") = '-'";
    }

    private int nextSortOrder(String heading) {
        int max = 3000;
        for (EmployeeFieldDefinition definition : listFields()) {
            if (definition.heading().equalsIgnoreCase(heading)) {
                max = Math.max(max, definition.sortOrder());
            }
        }
        return max + 10;
    }

    private static Comparator<EmployeeFieldDefinition> fieldOrder() {
        return Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER);
    }

    private static String normalizeHeading(String heading) {
        String clean = heading == null ? "" : heading.trim();
        return clean.isEmpty() ? "Additional Details" : clean;
    }

    private static boolean isFundamentalsHeading(String heading) {
        return EmployeeBasicFieldUtil.isFundamentalsHeading(normalizeHeading(heading));
    }

    private static String normalizeOptions(String options) {
        if (options == null || options.isBlank()) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String raw : options.split("[\\r\\n,]+")) {
            String option = raw.trim();
            if (option.isEmpty() || containsIgnoreCase(normalized, option)) {
                continue;
            }
            normalized.add(option);
        }
        return String.join("\n", normalized);
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String requireText(String value, String message) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return clean;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeIdentity(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String toColumnName(String label) {
        String clean = label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
        clean = clean.replaceAll("[^A-Z0-9]+", "_").replaceAll("_+", "_");
        clean = clean.replaceAll("^_+", "").replaceAll("_+$", "");
        if (clean.isBlank()) {
            throw new IllegalArgumentException("Field label must contain letters or numbers.");
        }
        if (!Character.isLetter(clean.charAt(0))) {
            clean = "FIELD_" + clean;
        }
        if (clean.length() > 64) {
            clean = clean.substring(0, 64).replaceAll("_+$", "");
        }
        return clean;
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

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    private static EmployeeFieldDefinition def(
            String column,
            String label,
            String heading,
            boolean document,
            boolean detail,
            int sortOrder
    ) {
        return def(column, label, heading, document, detail, false, sortOrder);
    }

    private static EmployeeFieldDefinition def(
            String column,
            String label,
            String heading,
            boolean document,
            boolean detail,
            boolean textAreaField,
            int sortOrder
    ) {
        return new EmployeeFieldDefinition(
                column,
                label,
                heading,
                document,
                false,
                true,
                detail,
                false,
                sortOrder,
                false,
                false,
                false,
                textAreaField,
                "",
                EmployeeBasicFieldUtil.BASIC_COLUMNS.contains(column)
                        || DEFAULT_REQUIRED_DOCUMENT_COLUMNS.contains(column)
        );
    }
}
