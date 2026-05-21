package com.kgm.dao;

import com.kgm.model.Employee;
import com.kgm.util.EmployeeDocumentUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class EmployeeRegistrationDao {
    private static final List<String> BASE_INSERT_COLUMNS = List.of(
            "NID",
            "EMP_NAME",
            "DEPARTMENT",
            "DESIGNATION",
            "PERSONAL_EMAIL",
            "JOINING_DATE",
            "RESIGN_DATE",
            "EMP_CONTNO",
            "PERMANENT_ADR",
            "EMPLOYEE_CODE",
            "GENDER",
            "RESIGN_REASON"
    );

    private final Connection conn;

    public EmployeeRegistrationDao(Connection conn) {
        this.conn = conn;
    }

    public void insertEmployee(Employee employee) {
        List<String> columns = insertColumns();
        String sql = "INSERT INTO employees (" + quotedColumns(columns) + ") VALUES (" + placeholders(columns.size()) + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int index = 0; index < columns.size(); index++) {
                ps.setString(index + 1, safe(readField(employee, columns.get(index))));
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Employee insert failed: " + ex.getMessage(), ex);
        }
    }

    private List<String> insertColumns() {
        List<String> columns = new ArrayList<>(BASE_INSERT_COLUMNS);
        for (EmployeeDocumentUtil.DocumentType documentType : EmployeeDocumentUtil.documentTypes()) {
            columns.add(documentType.employeeFieldName());
        }
        columns.add("EMP_IMG");
        return columns;
    }

    private String quotedColumns(List<String> columns) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String column : columns) {
            joiner.add(quoteIdentifier(column));
        }
        return joiner.toString();
    }

    private String placeholders(int count) {
        StringJoiner joiner = new StringJoiner(",");
        for (int index = 0; index < count; index++) {
            joiner.add("?");
        }
        return joiner.toString();
    }

    private String readField(Employee employee, String fieldName) {
        try {
            Field field = Employee.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(employee);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException exception) {
            return employee.getDynamicField(fieldName);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
