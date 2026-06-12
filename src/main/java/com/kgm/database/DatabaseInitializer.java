package com.kgm.database;

import com.kgm.config.DatabaseConfig;
import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.util.EmployeeAdditionalFieldDefaults;
import com.kgm.util.EmployeeDocumentUtil;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class DatabaseInitializer {
    private static volatile boolean initialized;

    private static final String EMPLOYEES_TABLE = """
            CREATE TABLE IF NOT EXISTS employees (
                ID INT NOT NULL AUTO_INCREMENT,

                -- CORE
                UNT_CODE TEXT,
                EMPLOYEE_CODE VARCHAR(100) NOT NULL DEFAULT '',
                EMP_NAME TEXT,
                FATHER_NAME TEXT,
                MOTHER_NAME TEXT,
                GENDER TEXT,
                DOB TEXT,
                CITY_OF_BIRTH TEXT,
                NATIONALITY TEXT,
                RELIGION TEXT,
                BLOOD_GROUP TEXT,
                M_STATUS TEXT,
                NID TEXT,

                -- EMPLOYMENT
                DEPARTMENT TEXT,
                DESIG_CODE TEXT,
                DESIGNATION TEXT,
                DEPT_CODE TEXT,
                SECTION TEXT,
                GRADE TEXT,
                JOINING_DATE TEXT,
                CONFIRMING_ON TEXT,
                EMP_STATUS TEXT,
                SHIFT TEXT,
                PROB_PERIOD TEXT,
                EXP_IN_KTML TEXT,
                REST_DAY TEXT,
                STAFF TEXT,
                PRE_WORKEXP TEXT,
                CARDNO TEXT,
                CHEST_CARD_STATUS TEXT,
                REHIRING_STATUS TEXT,
                TAILOR_CATEGORY TEXT,
                VAC_ID TEXT,
                APPLICATION_DATE TEXT,
                RESIGN_REASON TEXT,
                RESIGN_DATE TEXT,

                -- ORGANIZATION
                ORG_ID TEXT,
                DIVISION TEXT,
                BRANCH_CODE TEXT,
                BRANCH_NAME TEXT,
                DESCR TEXT,

                -- PAYROLL
                GROSS_SALARY TEXT,
                PAY_SHEET TEXT,
                PAY_CATEGORY TEXT,
                BASIC TEXT,
                COLA1 TEXT,
                COLA2 TEXT,
                COLA3 TEXT,
                COLA4 TEXT,
                COLA5 TEXT,
                COLA6_7 TEXT,
                COLA8 TEXT,
                COLA9 TEXT,
                COLA10 TEXT,
                COLA11 TEXT,
                H_RENT TEXT,
                H_MAINTENANCE TEXT,
                PB_SPECIAL1_2 TEXT,
                PB_SPECIAL3 TEXT,
                PB_SPECIAL4 TEXT,
                SPECIAL TEXT,
                OTHER1 TEXT,
                OTHER2 TEXT,
                OTHER3 TEXT,
                MEDICAL TEXT,
                CONVEYANCE TEXT,
                UTILITY TEXT,
                ENTERTAINMENT TEXT,
                PAY_GROUP TEXT,
                PAY_GROUP_DESC TEXT,
                PAY_AT_JOINING TEXT,
                EXTRA_DUTY TEXT,
                EXTRA_DUTY_ALLOWANCE_DATE TEXT,
                PAYROLL_FLAG TEXT,

                -- BANKING
                BANK_NAME TEXT,
                BANK_AC_NO TEXT,
                SS_NO TEXT,
                EOBI_NO TEXT,
                TAX_NO TEXT,
                PFUND_DEDUCTION TEXT,
                PF_INTEREST TEXT,
                PFUND_CODE TEXT,
                CLIPPER_PFUND_CODE TEXT,
                EFU TEXT,
                EFU_NO TEXT,
                EOBI_STATUS TEXT,

                -- CONTACT
                EMP_CONTNO TEXT,
                CURRENT_ADR TEXT,
                PERMANENT_ADR TEXT,
                CITY_VILLAGE TEXT,
                DISTRICT TEXT,
                REFERENCE TEXT,
                RELATIVE_DETAIL TEXT,
                REFEMP_NAME TEXT,
                REFEMP_DESIG TEXT,
                REFEMP_DEPT TEXT,
                CNIC_EXP_DATE TEXT,
                CNIC_FAMILY_NO TEXT,
                CNIC_ISSUANCE_DATE TEXT,
                PERSONAL_EMAIL TEXT,
                OFFICIAL_EMAIL TEXT,
                EMERGENCY_NO TEXT,

                -- REPORTING
                REP_UNT TEXT,
                REP_EMP_ID TEXT,
                REP_EMP_DESIG_CODE TEXT,
                REP_EMP_DEPT_CODE TEXT,
                REP_EMP_TYPE TEXT,
                REPORT_TO_EMP_ID TEXT,
                REPORT_TO_UNT TEXT,

                -- COMPLIANCE
                FLAG TEXT,
                CLEARANCE_STATUS TEXT,
                HOD_CHECK TEXT,
                SEC_HEAD_CHK TEXT,
                NIC_VERIFY TEXT,
                NIC_VERIFY_DATE TEXT,
                ATT_CATEG TEXT,
                DIS_CERTIFICATE TEXT,
                SS TEXT,
                DED_UNION TEXT,

                -- BENEFITS
                COLONY_RESIDENT TEXT,
                COMPANY_CAR TEXT,
                PERSONAL_HOUSE_RENT TEXT,
                COLONY_HOUSE_NUMBER TEXT,
                WELLNESS_CLUB TEXT,
                WELLNESS_CARD_ISSUE TEXT,
                WELLNESS_CARD_NO TEXT,
                WELLNESS_CLUB_VALID_DATE TEXT,

                -- VACCINATION
                FIRST_DOSE TEXT,
                SECOND_DOSE TEXT,
                FIRST_VACC_DATE TEXT,
                SECOND_VACC_DATE TEXT,

                -- IT / ALTERNATE SATURDAY
                USER_ID TEXT,
                IT_EQUIPMENT TEXT,
                IT_EMAIL TEXT,
                IT_INTERNET TEXT,
                INTERNET_JUSTIFY TEXT,
                IT_SERVICE_ALERT TEXT,
                ALT_SAT_TEAM TEXT,
                ALT_SAT_START_DATE TEXT,
                ALT_SAT_END_DATE TEXT,
                ALT_SAT_NEXT_YEAR TEXT,
                ALT_SAT_SHUFFLE TEXT,
                ALT_SAT_UNLOCK_NEXT_YEAR TEXT,

                -- DOCUMENTS
                CNIC_FRONT TEXT,
                CNIC_BACK TEXT,
                EOBI TEXT,
                SS_CARD TEXT,
                FINAL_SETTLEMENT TEXT,
                APPOINTMENT_LETTER_FRONT TEXT,
                APPOINTMENT_LETTER_BACK TEXT,
                APPLICATION_FRONT TEXT,
                APPLICATION_BACK TEXT,
                CLEARANCE_CERTIFICATE TEXT,
                SERVICE_CERTIFICATE TEXT,
                PAYMENT_VOUCHER TEXT,
                TRIAL_CARD TEXT,
                MEDICAL_DOC TEXT,
                INTERVIEW_FORMS TEXT,
                COVID_CERTIFICATE TEXT,
                DISCIPLINARY_I TEXT,
                DISCIPLINARY_II TEXT,
                DISCIPLINARY_III TEXT,
                MISCELLANEOUS_I TEXT,
                MISCELLANEOUS_II TEXT,
                MISCELLANEOUS_III TEXT,
                EMP_IMG TEXT,

                PRIMARY KEY (ID),
                UNIQUE KEY uk_employee_code (EMPLOYEE_CODE)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final Map<String, String> LEGACY_DOCUMENT_MAPPINGS = Map.ofEntries(
            Map.entry("CNIC_COPY", "CNIC_FRONT"),
            Map.entry("EOBI_CARD_COPY", "EOBI"),
            Map.entry("SS_CARD_COPY", "SS_CARD"),
            Map.entry("CLEARANCE_CERT", "CLEARANCE_CERTIFICATE"),
            Map.entry("JOB_APPOINTMENT", "APPOINTMENT_LETTER_FRONT"),
            Map.entry("APPLICATION_DOC", "APPLICATION_FRONT"),
            Map.entry("SETTLEMENT_DOC", "PAYMENT_VOUCHER"),
            Map.entry("INTERVIEW_DOC", "INTERVIEW_FORMS"),
            Map.entry("SERVICE_LETTER", "SERVICE_CERTIFICATE"),
            Map.entry("COVID_CERT", "COVID_CERTIFICATE")
    );

    private static final List<String> STANDARD_EMPLOYEE_TEXT_COLUMNS = List.of(
            "UNT_CODE",
            "EMP_NAME",
            "FATHER_NAME",
            "MOTHER_NAME",
            "GENDER",
            "DOB",
            "CITY_OF_BIRTH",
            "NATIONALITY",
            "RELIGION",
            "BLOOD_GROUP",
            "M_STATUS",
            "NID",
            "DEPARTMENT",
            "DESIG_CODE",
            "DESIGNATION",
            "SECTION",
            "GRADE",
            "JOINING_DATE",
            "CONFIRMING_ON",
            "EMP_STATUS",
            "SHIFT",
            "PROB_PERIOD",
            "EXP_IN_KTML",
            "APPLICATION_DATE",
            "RESIGN_REASON",
            "RESIGN_DATE",
            "ORG_ID",
            "DIVISION",
            "BRANCH_CODE",
            "BRANCH_NAME",
            "DESCR",
            "GROSS_SALARY",
            "PAY_CATEGORY",
            "BASIC",
            "COLA1",
            "COLA2",
            "COLA3",
            "COLA4",
            "COLA5",
            "COLA6_7",
            "COLA8",
            "COLA9",
            "COLA10",
            "COLA11",
            "PB_SPECIAL1_2",
            "PB_SPECIAL3",
            "PB_SPECIAL4",
            "SPECIAL",
            "OTHER1",
            "OTHER2",
            "OTHER3",
            "MEDICAL",
            "CONVEYANCE",
            "UTILITY",
            "ENTERTAINMENT",
            "PAY_GROUP",
            "PAY_GROUP_DESC",
            "PAY_AT_JOINING",
            "EXTRA_DUTY",
            "PAYROLL_FLAG",
            "BANK_NAME",
            "BANK_AC_NO",
            "SS_NO",
            "EOBI_NO",
            "TAX_NO",
            "PFUND_DEDUCTION",
            "PF_INTEREST",
            "PFUND_CODE",
            "CLIPPER_PFUND_CODE",
            "EFU",
            "EFU_NO",
            "EOBI_STATUS",
            "EMP_CONTNO",
            "CURRENT_ADR",
            "PERMANENT_ADR",
            "PERSONAL_EMAIL",
            "OFFICIAL_EMAIL",
            "EMERGENCY_NO",
            "REP_UNT",
            "REP_EMP_ID",
            "REP_EMP_DESIG_CODE",
            "REP_EMP_DEPT_CODE",
            "REP_EMP_TYPE",
            "FLAG",
            "CLEARANCE_STATUS",
            "HOD_CHECK",
            "SEC_HEAD_CHK",
            "NIC_VERIFY",
            "NIC_VERIFY_DATE",
            "ATT_CATEG",
            "DIS_CERTIFICATE",
            "WELLNESS_CLUB",
            "WELLNESS_CARD_ISSUE",
            "WELLNESS_CARD_NO",
            "WELLNESS_CLUB_VALID_DATE",
            "FIRST_DOSE",
            "SECOND_DOSE",
            "FIRST_VACC_DATE",
            "SECOND_VACC_DATE",
            "EMP_IMG"
    );

    public static synchronized void init() {
        if (initialized && schemaReady()) {
            return;
        }
        initialized = false;

        try {
            createDatabaseIfNeeded();
        } catch (SQLException e) {
            logStartupIssue("MySQL database creation failed", e);
            DatabaseConnection.reportConnectionFailure(e);
            throw startupFailure("MySQL database creation failed.", e);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean exists = tableExists(conn);
            stmt.execute(EMPLOYEES_TABLE);
            migrateCurrentAddressColumn(conn);
            ensureCoreColumns(conn);
            ensureDocumentColumns(conn);
            migrateLegacyDocumentColumns(conn);
            EmployeeFieldDefinitionDao fieldDefinitionDao = new EmployeeFieldDefinitionDao(conn);
            fieldDefinitionDao.ensureMetadata();
            fieldDefinitionDao.syncMetadataWithDatabase();
            EmployeeDocumentUtil.refreshDocumentTypes();
            ensureSearchIndexes(conn);
            ensureReportingIndexes(conn);

            System.out.println(exists ? "=> MySQL schema already exists." : "=> MySQL schema created.");
            initialized = true;

        } catch (SQLException e) {
            logStartupIssue("MySQL schema initialization failed", e);
            DatabaseConnection.reportConnectionFailure(e);
            throw startupFailure("MySQL schema initialization failed.", e);
        }
    }

    public static synchronized void reconnect() {
        initialized = false;
        init();
    }

    private static IllegalStateException startupFailure(String message, SQLException exception) {
        return new IllegalStateException(message + " Check the server connection and database credentials.", exception);
    }

    private static void logStartupIssue(String context, SQLException exception) {
        String message = exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No database error message was provided."
                : exception.getMessage();
        System.out.println("=> " + context + ": " + message);
    }

    private static boolean schemaReady() {
        try {
            createDatabaseIfNeeded();
            try (Connection conn = DatabaseConnection.getConnection()) {
                return tableExists(conn, "employees")
                        && tableExists(conn, "employee_field_metadata");
            }
        } catch (SQLException exception) {
            return false;
        }
    }

    private static void createDatabaseIfNeeded() throws SQLException {
        String sql = "CREATE DATABASE IF NOT EXISTS `" + DatabaseConfig.escapedDatabaseName()
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Connection conn = DatabaseConnection.getServerConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static boolean tableExists(Connection conn) throws SQLException {
        return tableExists(conn, "employees");
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void ensureDocumentColumns(Connection conn) throws SQLException {
        for (EmployeeDocumentUtil.DocumentType documentType : EmployeeDocumentUtil.documentTypes()) {
            String column = documentType.employeeFieldName();
            if (!columnExists(conn, column)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE employees ADD COLUMN " + quoteIdentifier(column) + " TEXT");
                }
            }
        }
    }

    private static void ensureCoreColumns(Connection conn) throws SQLException {
        for (String column : STANDARD_EMPLOYEE_TEXT_COLUMNS) {
            ensureColumn(conn, column);
        }
        for (String column : EmployeeAdditionalFieldDefaults.columnNames()) {
            ensureColumn(conn, column);
        }
    }

    private static void migrateCurrentAddressColumn(Connection conn) throws SQLException {
        boolean hasLegacyColumn = columnExists(conn, "CURRENT_ADDRESS");
        boolean hasCurrentColumn = columnExists(conn, "CURRENT_ADR");
        if (hasLegacyColumn && !hasCurrentColumn) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE employees CHANGE COLUMN "
                        + quoteIdentifier("CURRENT_ADDRESS") + " "
                        + quoteIdentifier("CURRENT_ADR") + " TEXT");
            }
        } else if (hasLegacyColumn) {
            String sql = "UPDATE employees SET " + quoteIdentifier("CURRENT_ADR")
                    + " = " + quoteIdentifier("CURRENT_ADDRESS")
                    + " WHERE " + emptyOrPlaceholder("CURRENT_ADR")
                    + " AND NOT " + emptyOrPlaceholder("CURRENT_ADDRESS");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                stmt.execute("ALTER TABLE employees DROP COLUMN " + quoteIdentifier("CURRENT_ADDRESS"));
            }
        }
        migrateCurrentAddressMetadata(conn);
    }

    private static void migrateCurrentAddressMetadata(Connection conn) throws SQLException {
        if (!tableExists(conn, "employee_field_metadata")
                || !metadataRowExists(conn, "CURRENT_ADDRESS")) {
            return;
        }

        if (metadataRowExists(conn, "CURRENT_ADR")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM employee_field_metadata WHERE UPPER(column_name) = 'CURRENT_ADDRESS'");
            }
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE employee_field_metadata SET column_name = 'CURRENT_ADR' "
                    + "WHERE UPPER(column_name) = 'CURRENT_ADDRESS'");
        }
    }

    private static void ensureColumn(Connection conn, String column) throws SQLException {
        if (columnExists(conn, column)) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE employees ADD COLUMN " + quoteIdentifier(column) + " TEXT");
        }
    }

    private static void migrateLegacyDocumentColumns(Connection conn) throws SQLException {
        for (Map.Entry<String, String> mapping : LEGACY_DOCUMENT_MAPPINGS.entrySet()) {
            String oldColumn = mapping.getKey();
            String newColumn = mapping.getValue();
            if (!columnExists(conn, oldColumn) || !columnExists(conn, newColumn)) {
                continue;
            }

            String sql = "UPDATE employees SET " + quoteIdentifier(newColumn) + " = " + quoteIdentifier(oldColumn)
                    + " WHERE " + emptyOrPlaceholder(newColumn)
                    + " AND NOT " + emptyOrPlaceholder(oldColumn);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    private static void ensureSearchIndexes(Connection conn) throws SQLException {
        if (indexExists(conn, "uk_employee_code") || indexExists(conn, "idx_employee_code")) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX idx_employee_code ON employees (EMPLOYEE_CODE)");
        }
    }

    private static void ensureReportingIndexes(Connection conn) throws SQLException {
        ensureTextPrefixIndex(conn, "idx_employees_department", "DEPARTMENT");
        ensureTextPrefixIndex(conn, "idx_employees_section", "SECTION");
        ensureTextPrefixIndex(conn, "idx_employees_grade", "GRADE");
        ensureTextPrefixIndex(conn, "idx_employees_designation", "DESIGNATION");
        ensureTextPrefixIndex(conn, "idx_employees_resign_reason", "RESIGN_REASON");
    }

    private static void ensureTextPrefixIndex(Connection conn, String indexName, String columnName) throws SQLException {
        if (indexExists(conn, indexName) || !columnExists(conn, columnName)) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX " + quoteIdentifier(indexName)
                    + " ON employees (" + quoteIdentifier(columnName) + "(120))");
        }
    }

    private static boolean columnExists(Connection conn, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, "employees", columnName)) {
            return rs.next();
        }
    }

    private static boolean metadataRowExists(Connection conn, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM employee_field_metadata WHERE UPPER(column_name) = UPPER(?) LIMIT 1")) {
            ps.setString(1, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean indexExists(Connection conn, String indexName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(conn.getCatalog(), null, "employees", false, false)) {
            while (rs.next()) {
                if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String emptyOrPlaceholder(String column) {
        String quoted = quoteIdentifier(column);
        return "(" + quoted + " IS NULL OR TRIM(" + quoted + ") = '' OR UPPER(TRIM(" + quoted
                + ")) IN ('N/A', 'NA', 'NULL') OR TRIM(" + quoted + ") = '-')";
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
