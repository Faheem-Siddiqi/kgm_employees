package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.EmployeeFieldDefinition;

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

    private static final List<EmployeeFieldDefinition> BUILT_IN_FIELDS = List.of(
            def("ID", "ID", "System", false, false, 1),
            def("UNT_CODE", "Unit Code", "Employment Details", false, true, 10),
            def("EMPLOYEE_CODE", "Employee Code", "System", false, false, 20),
            def("EMP_NAME", "Employee Name", "Core / Registration", false, false, 30),
            def("FATHER_NAME", "Father Name", "Core / Registration", false, false, 40),
            def("MOTHER_NAME", "Mother Name", "Personal / HR Details", false, true, 50),
            def("GENDER", "Gender", "Core / Registration", false, false, 60),
            def("DOB", "Date of Birth", "Personal / HR Details", false, true, 70),
            def("CITY_OF_BIRTH", "City of Birth", "Personal / HR Details", false, true, 80),
            def("NATIONALITY", "Nationality", "Personal / HR Details", false, true, 90),
            def("RELIGION", "Religion", "Personal / HR Details", false, true, 100),
            def("BLOOD_GROUP", "Blood Group", "Personal / HR Details", false, true, 110),
            def("M_STATUS", "Marital Status", "Personal / HR Details", false, true, 120),
            def("NID", "CNIC / NID", "Core / Registration", false, false, 130),

            def("DEPARTMENT", "Department", "Core / Registration", false, false, 200),
            def("DESIG_CODE", "Designation Code", "Employment Details", false, true, 210),
            def("DESIGNATION", "Designation", "Core / Registration", false, false, 220),
            def("GRADE", "Grade", "Employment Details", false, true, 230),
            def("JOINING_DATE", "Joining Date", "Core / Registration", false, false, 240),
            def("CONFIRMING_ON", "Confirming On", "Organization / Structure", false, true, 250),
            def("EMP_STATUS", "Employee Status", "Employment Details", false, true, 260),
            def("SHIFT", "Shift", "Organization / Structure", false, true, 270),
            def("PROB_PERIOD", "Probation Period", "Organization / Structure", false, true, 280),
            def("EXP_IN_KTML", "Experience in KTML", "Employment Details", false, true, 290),
            def("APPLICATION_DATE", "Application Date", "Employment Details", false, true, 300),
            def("RESIGN_REASON", "Resign Reason", "Core / Registration", false, false, 310),
            def("RESIGN_DATE", "Resign Date", "Core / Registration", false, false, 320),

            def("ORG_ID", "ORG ID", "Organization / Structure", false, true, 400),
            def("DIVISION", "Division", "Organization / Structure", false, true, 410),
            def("BRANCH_CODE", "Branch Code", "Organization / Structure", false, true, 420),
            def("BRANCH_NAME", "Branch Name", "Organization / Structure", false, true, 430),
            def("DESCR", "Description", "Organization / Structure", false, true, 440),

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

            def("EMP_CONTNO", "Contact Number", "Core / Registration", false, false, 1000),
            def("CURRENT_ADR", "Current Address", "Core / Registration", false, false, 1010),
            def("PERMANENT_ADR", "Permanent Address", "Core / Registration", false, false, 1020),
            def("PERSONAL_EMAIL", "Personal Email", "Core / Registration", false, false, 1030),
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
                        sort_order INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (column_name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }
        ensureMetadataColumn("date_field", "TINYINT(1) NOT NULL DEFAULT 0");

        for (EmployeeFieldDefinition definition : BUILT_IN_FIELDS) {
            insertMetadataIgnore(definition);
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
                    3000 + column.getValue()
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
            if (!definition.documentField() && definition.detailField()) {
                definitions.add(definition);
            }
        }
        definitions.sort(fieldOrder());
        return definitions;
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
            if (!definition.documentField() && definition.detailField()) {
                headings.add(definition.heading());
            }
        }
        headings.add("Additional Details");
        return new ArrayList<>(headings);
    }

    public EmployeeFieldDefinition addField(String label, String heading, boolean documentField, boolean dateField) {
        String cleanLabel = requireText(label, "Field label is required.");
        String cleanHeading = documentField ? "Documents" : normalizeHeading(heading);
        String column = nextAvailableColumnName(toColumnName(cleanLabel));
        int sortOrder = nextSortOrder(cleanHeading);
        boolean effectiveDateField = !documentField && dateField;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE employees ADD COLUMN " + quoteIdentifier(column) + " TEXT");
            EmployeeFieldDefinition definition = new EmployeeFieldDefinition(
                    column,
                    cleanLabel,
                    cleanHeading,
                    documentField,
                    true,
                    false,
                    !documentField,
                    effectiveDateField,
                    sortOrder
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
        String cleanHeading = current.documentField() ? "Documents" : normalizeHeading(newHeading);
        String newColumn = toColumnName(cleanLabel);
        boolean effectiveDateField = !current.documentField() && dateField;

        try {
            if (!current.columnName().equalsIgnoreCase(newColumn)) {
                if (columnExists(newColumn)) {
                    newColumn = nextAvailableColumnName(newColumn);
                }
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
                    true,
                    false,
                    !current.documentField(),
                    effectiveDateField,
                    current.sortOrder()
            );
            insertMetadataReplace(renamed);
            return renamed;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to rename employee field: " + exception.getMessage(), exception);
        }
    }

    public EmployeeFieldDefinition updateFieldSettings(String columnName, String label, String heading, boolean dateField) {
        EmployeeFieldDefinition current = requireExistingField(columnName);
        String cleanLabel = requireText(label, "Field label is required.");
        String cleanHeading = current.documentField() ? "Documents" : normalizeHeading(heading);
        boolean effectiveDateField = !current.documentField() && dateField;

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE employee_field_metadata
                SET display_label = ?, heading = ?, date_field = ?
                WHERE column_name = ?
                """)) {
            ps.setString(1, cleanLabel);
            ps.setString(2, cleanHeading);
            ps.setBoolean(3, effectiveDateField);
            ps.setString(4, current.columnName());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update employee field: " + exception.getMessage(), exception);
        }

        return new EmployeeFieldDefinition(
                current.columnName(),
                cleanLabel,
                cleanHeading,
                current.documentField(),
                current.customField(),
                current.protectedField(),
                current.detailField(),
                effectiveDateField,
                current.sortOrder()
        );
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
                "UPDATE employee_field_metadata SET date_field = ? WHERE column_name = ?")) {
            ps.setBoolean(1, effectiveDateField);
            ps.setString(2, definition.columnName());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update date field setting: " + exception.getMessage(), exception);
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
                       protected_field, detail_field, date_field, sort_order
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
                        rs.getInt("sort_order")
                );
                fields.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
            }
        }
        return fields;
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

    private void insertMetadataIgnore(EmployeeFieldDefinition definition) throws SQLException {
        String sql = """
                INSERT IGNORE INTO employee_field_metadata
                    (column_name, display_label, heading, document_field, custom_field,
                     protected_field, detail_field, date_field, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        writeMetadata(definition, sql);
    }

    private void insertMetadataReplace(EmployeeFieldDefinition definition) throws SQLException {
        String sql = """
                REPLACE INTO employee_field_metadata
                    (column_name, display_label, heading, document_field, custom_field,
                     protected_field, detail_field, date_field, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.executeUpdate();
        }
    }

    private void ensureMetadataColumn(String columnName, String ddl) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, TABLE, columnName)) {
            if (rs.next()) {
                return;
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + quoteIdentifier(columnName) + " " + ddl);
        }
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

    private String nextAvailableColumnName(String baseColumn) {
        String candidate = baseColumn;
        int suffix = 2;
        try {
            while (columnExists(candidate)) {
                String end = "_" + suffix++;
                int maxBaseLength = Math.max(1, 64 - end.length());
                candidate = baseColumn.substring(0, Math.min(baseColumn.length(), maxBaseLength)) + end;
            }
            return candidate;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to check field column names.", exception);
        }
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

    private static String requireText(String value, String message) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return clean;
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

    private static EmployeeFieldDefinition def(
            String column,
            String label,
            String heading,
            boolean document,
            boolean detail,
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
                sortOrder
        );
    }
}
