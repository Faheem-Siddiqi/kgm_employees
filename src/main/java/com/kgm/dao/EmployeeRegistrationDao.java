package com.kgm.dao;

import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.util.EmployeeDocumentUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class EmployeeRegistrationDao {
    @FunctionalInterface
    public interface BatchProgressListener {
        void onProgress(String message, int completedRows, int totalRows);
    }

    private static final List<String> BASE_INSERT_COLUMNS = List.of(
            "NID",
            "EMP_NAME",
            "FATHER_NAME",
            "DEPARTMENT",
            "DESIGNATION",
            "PERSONAL_EMAIL",
            "DOB",
            "JOINING_DATE",
            "RESIGN_DATE",
            "EMP_CONTNO",
            "PERMANENT_ADR",
            "EMPLOYEE_CODE",
            "GENDER",
            "RESIGN_REASON",
            "SECTION",
            "GRADE",
            "SHIFT"
    );

    private final Connection conn;

    public EmployeeRegistrationDao(Connection conn) {
        this.conn = conn;
    }

    public void insertEmployee(Employee employee) {
        insertEmployee(employee, new ArrayList<>(employee.getDynamicFields().keySet()));
    }

    public void insertEmployee(Employee employee, List<String> extraColumns) {
        List<String> columns = insertColumns();
        for (String column : extraColumns) {
            addColumn(columns, column);
        }
        String sql = "INSERT INTO employees (" + quotedColumns(columns) + ") VALUES (" + placeholders(columns.size()) + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int index = 0; index < columns.size(); index++) {
                ps.setString(index + 1, safe(readField(employee, columns.get(index))));
            }
            ps.executeUpdate();
            new EmployeeFieldDefinitionDao(conn).applyCustomFieldDefaultsForEmployee(employee.getEMPLOYEE_CODE());
        } catch (SQLException ex) {
            throw new RuntimeException("Employee insert failed: " + ex.getMessage(), ex);
        }
    }

    public int insertEmployees(List<Employee> employees, List<List<String>> extraColumnsByEmployee) {
        return insertEmployees(employees, extraColumnsByEmployee, null);
    }

    public int insertEmployees(
            List<Employee> employees,
            List<List<String>> extraColumnsByEmployee,
            BatchProgressListener progressListener
    ) {
        if (employees == null || employees.isEmpty()) {
            return 0;
        }

        List<String> columns = insertColumns();
        if (extraColumnsByEmployee != null) {
            for (List<String> extraColumns : extraColumnsByEmployee) {
                if (extraColumns == null) {
                    continue;
                }
                for (String column : extraColumns) {
                    addColumn(columns, column);
                }
            }
        }

        String sql = "INSERT INTO employees (" + quotedColumns(columns) + ") VALUES (" + placeholders(columns.size()) + ")";
        boolean originalAutoCommit;
        try {
            originalAutoCommit = conn.getAutoCommit();
        } catch (SQLException exception) {
            throw new RuntimeException("Employee batch insert failed: " + exception.getMessage(), exception);
        }

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                List<String> employeeCodes = new ArrayList<>();
                notifyProgress(progressListener, "Preparing rows for database insert...", 0, employees.size());
                for (int employeeIndex = 0; employeeIndex < employees.size(); employeeIndex++) {
                    Employee employee = employees.get(employeeIndex);
                    employeeCodes.add(safe(readField(employee, "EMPLOYEE_CODE")));
                    for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                        ps.setString(columnIndex + 1, safe(readField(employee, columns.get(columnIndex))));
                    }
                    ps.addBatch();
                    notifyProgress(progressListener, "Preparing rows for database insert...", employeeIndex + 1, employees.size());
                    if ((employeeIndex + 1) % 500 == 0) {
                        notifyProgress(progressListener, "Writing employee batch to database...", employeeIndex + 1, employees.size());
                        ps.executeBatch();
                    }
                }
                notifyProgress(progressListener, "Writing employee batch to database...", employees.size(), employees.size());
                ps.executeBatch();
                notifyProgress(progressListener, "Applying field defaults...", employees.size(), employees.size());
                new EmployeeFieldDefinitionDao(conn).applyCustomFieldDefaultsForEmployees(employeeCodes);
                notifyProgress(progressListener, "Finalizing import transaction...", employees.size(), employees.size());
                conn.commit();
                return employees.size();
            }
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new RuntimeException("Employee batch insert failed: " + exception.getMessage(), exception);
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    private void notifyProgress(
            BatchProgressListener progressListener,
            String message,
            int completedRows,
            int totalRows
    ) {
        if (progressListener == null) {
            return;
        }
        try {
            progressListener.onProgress(message, completedRows, totalRows);
        } catch (RuntimeException ignored) {
        }
    }

    private List<String> insertColumns() {
        List<String> columns = new ArrayList<>();
        for (String column : BASE_INSERT_COLUMNS) {
            addColumn(columns, column);
        }
        for (EmployeeFieldDefinition definition : new EmployeeFieldDefinitionDao(conn).listFundamentalsFields()) {
            addColumn(columns, definition.columnName());
        }
        for (EmployeeDocumentUtil.DocumentType documentType : EmployeeDocumentUtil.documentTypes()) {
            addColumn(columns, documentType.employeeFieldName());
        }
        addColumn(columns, "EMP_IMG");
        return columns;
    }

    private void addColumn(List<String> columns, String column) {
        if (column == null || column.isBlank() || containsColumn(columns, column)) {
            return;
        }
        columns.add(column.trim().toUpperCase());
    }

    private boolean containsColumn(List<String> columns, String column) {
        for (String existing : columns) {
            if (existing.equalsIgnoreCase(column)) {
                return true;
            }
        }
        return false;
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

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }
}
