package com.kgm.dao;

import com.kgm.model.Employee;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmployeeDao {

    private Connection conn;

    public EmployeeDao(Connection conn) {
        this.conn = conn;
    }

    public void insertEmployee(Employee e) throws SQLException {

        String sql = "INSERT INTO EMPLOYEE (" +
                "NID, EMP_NAME, DEPARTMENT, DESIGNATION, PERSONAL_EMAIL, " +
                "RESIGN_DATE, JOINING_DATE, EMP_CONTNO, PERMANENT_ADR, EMPLOYEE_CODE, " +

                "CNIC_COPY, SS_CARD_COPY, EOBI_CARD_COPY, FINAL_SETTLEMENT, CLEARANCE_CERT, " +
                "JOB_APPOINTMENT, APPLICATION_DOC, ISSUANCE_DOC, SETTLEMENT_DOC, TRIAL_CARD, " +
                "INTERVIEW_DOC, SERVICE_LETTER, EXTENSION_LETTER, RETIREMENT_LETTER, COVID_CERT, " +

                "DISCIPLINARY_I, DISCIPLINARY_II, DISCIPLINARY_III, EMP_IMG" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        PreparedStatement ps = conn.prepareStatement(sql);

        // BASIC INFO
        ps.setString(1, e.getNID());
        ps.setString(2, e.getEMP_NAME());
        ps.setString(3, e.getDEPARTMENT());
        ps.setString(4, e.getDESIGNATION());
        ps.setString(5, e.getPERSONAL_EMAIL());

        ps.setString(6, e.getRESIGN_DATE());
        ps.setString(7, e.getJOINING_DATE());
        ps.setString(8, e.getEMP_CONTNO());
        ps.setString(9, e.getPERMANENT_ADR());
        ps.setString(10, e.getEMPLOYEE_CODE());

        // DOCUMENTS
        ps.setString(11, e.getCNIC_COPY());
        ps.setString(12, e.getSS_CARD_COPY());
        ps.setString(13, e.getEOBI_CARD_COPY());
        ps.setString(14, e.getFINAL_SETTLEMENT());
        ps.setString(15, e.getCLEARANCE_CERT());

        ps.setString(16, e.getJOB_APPOINTMENT());
        ps.setString(17, e.getAPPLICATION_DOC());
        ps.setString(18, e.getISSUANCE_DOC());
        ps.setString(19, e.getSETTLEMENT_DOC());
        ps.setString(20, e.getTRIAL_CARD());

        ps.setString(21, e.getINTERVIEW_DOC());
        ps.setString(22, e.getSERVICE_LETTER());
        ps.setString(23, e.getEXTENSION_LETTER());
        ps.setString(24, e.getRETIREMENT_LETTER());
        ps.setString(25, e.getCOVID_CERT());

        ps.setString(26, e.getDISCIPLINARY_I());
        ps.setString(27, e.getDISCIPLINARY_II());
        ps.setString(28, e.getDISCIPLINARY_III());
        ps.setString(29, e.getEMP_IMG());

        ps.executeUpdate();
        ps.close();
    }
}