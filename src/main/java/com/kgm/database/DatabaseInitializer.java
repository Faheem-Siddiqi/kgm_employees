package com.kgm.database;

import com.kgm.config.DatabaseConfig;
import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.util.EmployeeDocumentUtil;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class DatabaseInitializer {
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
                GRADE TEXT,
                JOINING_DATE TEXT,
                CONFIRMING_ON TEXT,
                EMP_STATUS TEXT,
                SHIFT TEXT,
                PROB_PERIOD TEXT,
                EXP_IN_KTML TEXT,
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
                PERSONAL_EMAIL TEXT,
                OFFICIAL_EMAIL TEXT,
                EMERGENCY_NO TEXT,

                -- REPORTING
                REP_UNT TEXT,
                REP_EMP_ID TEXT,
                REP_EMP_DESIG_CODE TEXT,
                REP_EMP_DEPT_CODE TEXT,
                REP_EMP_TYPE TEXT,

                -- COMPLIANCE
                FLAG TEXT,
                CLEARANCE_STATUS TEXT,
                HOD_CHECK TEXT,
                SEC_HEAD_CHK TEXT,
                NIC_VERIFY TEXT,
                NIC_VERIFY_DATE TEXT,
                ATT_CATEG TEXT,
                DIS_CERTIFICATE TEXT,

                -- BENEFITS
                WELLNESS_CLUB TEXT,
                WELLNESS_CARD_ISSUE TEXT,
                WELLNESS_CARD_NO TEXT,
                WELLNESS_CLUB_VALID_DATE TEXT,

                -- VACCINATION
                FIRST_DOSE TEXT,
                SECOND_DOSE TEXT,
                FIRST_VACC_DATE TEXT,
                SECOND_VACC_DATE TEXT,

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

    public static void init() {
        try {
            createDatabaseIfNeeded();
        } catch (SQLException e) {
            System.out.println("=> MySQL database creation failed!");
            e.printStackTrace();
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean exists = tableExists(conn);
            stmt.execute(EMPLOYEES_TABLE);
            ensureDocumentColumns(conn);
            migrateLegacyDocumentColumns(conn);
            EmployeeFieldDefinitionDao fieldDefinitionDao = new EmployeeFieldDefinitionDao(conn);
            fieldDefinitionDao.ensureMetadata();
            fieldDefinitionDao.syncMetadataWithDatabase();
            ensureSearchIndexes(conn);

            System.out.println(exists ? "=> MySQL schema already exists." : "=> MySQL schema created.");

        } catch (SQLException e) {
            System.out.println("=> MySQL schema failed!");
            e.printStackTrace();
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
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "employees", new String[]{"TABLE"})) {
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

    private static boolean columnExists(Connection conn, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, "employees", columnName)) {
            return rs.next();
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
