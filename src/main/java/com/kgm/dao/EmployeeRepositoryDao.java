package com.kgm.dao;
import com.kgm.config.DatabaseConnection;
import com.kgm.model.Employee;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class EmployeeRepositoryDao {
    private final Connection con;
    public EmployeeRepositoryDao() {
        this.con = DatabaseConnection.getConnection();
    }
    // ==============================
    // 🔹 SAFE VALUE HANDLER
    // ==============================
    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value;
    }
    // ==============================
    // 🔹 PAGINATED LIST (LIMIT FIXED = 2500)
    // ==============================
    public List<Employee> getEmployees(int offset) {
        int limit = 2500; // FIXED PAGE SIZE
        List<Employee> list = new ArrayList<>();
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DESIGNATION,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    ORDER BY ID DESC
                    LIMIT ? OFFSET ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Employee e = new Employee();
                e.setID(rs.getInt("ID"));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setNID(safe(rs.getString("NID")));
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
                list.add(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }
    // ==============================
    // 🔹 TOTAL COUNT
    // ==============================
    public int countEmployees() {
        String sql = "SELECT COUNT(*) FROM employees";
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }
    // ==============================
    // 🔹 SEARCH BY EMPLOYEE CODE (INDEXED)
    // ==============================
    public Employee getEmployeeByCode(String empCode) {
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DESIGNATION,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    WHERE EMPLOYEE_CODE = ?
                    LIMIT 1
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, empCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Employee e = new Employee();
                e.setID(rs.getInt("ID"));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setNID(safe(rs.getString("NID")));
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
                return e;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
