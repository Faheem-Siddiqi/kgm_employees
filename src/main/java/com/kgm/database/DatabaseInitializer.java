package com.kgm.database;

import com.kgm.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class DatabaseInitializer {

    public static void init() {

        // ================= EMPLOYEE CORE =================
        String employees = """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                UNT_CODE TEXT,
                EMPLOYEE_CODE TEXT UNIQUE,
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
                NID TEXT
            );
        """;

        // ================= EMPLOYMENT =================
        String employment = """
            CREATE TABLE IF NOT EXISTS employment (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
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
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= ORGANIZATION =================
        String organization = """
            CREATE TABLE IF NOT EXISTS organization (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                ORG_ID TEXT,
                DIVISION TEXT,
                BRANCH_CODE TEXT,
                BRANCH_NAME TEXT,
                UNT_CODE TEXT,
                DESCR TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= PAYROLL =================
        String payroll = """
            CREATE TABLE IF NOT EXISTS payroll (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
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
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= BANK / LEGAL =================
        String banking = """
            CREATE TABLE IF NOT EXISTS banking (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                BANK_NAME TEXT,
                BANK_AC_NO TEXT,
                SS_NO TEXT,
                EOBI_NO TEXT,
                TAX_NO TEXT,
                PFUND_DEDUCTION TEXT,
                PF_INTREST TEXT,
                PFUND_CODE TEXT,
                CLIPPER_PFUND_CODE TEXT,
                EFU TEXT,
                EFU_NO TEXT,
                EOBI_STATUS TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= CONTACT =================
        String contact = """
            CREATE TABLE IF NOT EXISTS contact (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                EMP_CONTNO TEXT,
                CURRENT_ADR TEXT,
                PERMANENT_ADR TEXT,
                PERSONAL_EMAIL TEXT,
                OFFICIAL_EMAIL TEXT,
                EMERGENCY_NO TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= REPORTING =================
        String reporting = """
            CREATE TABLE IF NOT EXISTS reporting (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                REP_UNT TEXT,
                REP_EMP_ID TEXT,
                REP_EMP_DESIG_CODE TEXT,
                REP_EMP_DEPT_CODE TEXT,
                REP_EMP_TYPE TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= COMPLIANCE =================
        String compliance = """
            CREATE TABLE IF NOT EXISTS compliance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                FLAG TEXT,
                CLEARANCE_STATUS TEXT,
                HOD_CHECK TEXT,
                SEC_HEAD_CHK TEXT,
                NIC_VERIFY TEXT,
                NIC_VERIFY_DATE TEXT,
                ATT_CATEG TEXT,
                DIS_CERTIFICATE TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= BENEFITS =================
        String benefits = """
            CREATE TABLE IF NOT EXISTS benefits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                WELLNESS_CLUB TEXT,
                WELLNESS_CARD_ISSUE TEXT,
                WELLNESS_CARD_NO TEXT,
                WELLNESS_CLUB_VALID_DATE TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= VACCINATION =================
        String vaccination = """
            CREATE TABLE IF NOT EXISTS vaccination (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                FIRST_DOSE TEXT,
                SECOND_DOSE TEXT,
                FIRST_VACC_DATE TEXT,
                SECOND_VACC_DATE TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        // ================= DOCUMENTS =================
        String documents = """
            CREATE TABLE IF NOT EXISTS documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                EMPLOYEE_CODE TEXT,
                CNIC_COPY TEXT,
                SS_CARD_COPY TEXT,
                EOBI_CARD_COPY TEXT,
                FINAL_SETTLEMENT TEXT,
                CLEARANCE_CERT TEXT,
                JOB_APPOINTMENT TEXT,
                APPLICATION_DOC TEXT,
                ISSUANCE_DOC TEXT,
                SETTLEMENT_DOC TEXT,
                TRIAL_CARD TEXT,
                INTERVIEW_DOC TEXT,
                SERVICE_LETTER TEXT,
                EXTENSION_LETTER TEXT,
                RETIREMENT_LETTER TEXT,
                COVID_CERT TEXT,
                DISCIPLINARY_I TEXT,
                DISCIPLINARY_II TEXT,
                DISCIPLINARY_III TEXT,
                EMP_IMG TEXT,
                FOREIGN KEY (EMPLOYEE_CODE) REFERENCES employees(EMPLOYEE_CODE)
            );
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean exists;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='employees'")) {
                exists = rs.next();
            }

            stmt.execute(employees);
            stmt.execute(employment);
            stmt.execute(organization);
            stmt.execute(payroll);
            stmt.execute(banking);
            stmt.execute(contact);
            stmt.execute(reporting);
            stmt.execute(compliance);
            stmt.execute(benefits);
            stmt.execute(vaccination);
            stmt.execute(documents);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_emp_code ON employees(EMPLOYEE_CODE);");

            if (!exists) {
                System.out.println("=> Schema created.");
            } else {
                System.out.println("=> Schema already exists.");
            }

        } catch (SQLException e) {
            System.out.println("=> Schema failed!");
            e.printStackTrace();
        }
    }
}