package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.Employee;
import com.kgm.ui.EmployeeDetailView;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeTablePanel extends JPanel {
    private static final int EMPLOYEE_CODE_COLUMN = 0;
    private static final int ACTION_COLUMN = 8;

    private static final String[] COLUMNS = {
            "Employee ID",
            "Name",
            "CNIC",
            "Phone",
            "Department",
            "Designation",
            "Joining Date",
            "Leaving Date",
            "Action"
    };

    private final EmployeeRecordDao repo;
    private final UniversalTablePanel tablePanel;
    private final List<Employee> displayedEmployees = new ArrayList<>();

    public EmployeeTablePanel(EmployeeRecordDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tablePanel = new UniversalTablePanel(COLUMNS, "No employee records found.");
        tablePanel.setLinkColumn(EMPLOYEE_CODE_COLUMN, this::openEmployeeDetail);
        tablePanel.setActionColumn(ACTION_COLUMN, "View", this::openEmployeeDetail);
        tablePanel.setColumnAlignment(6, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(7, SwingConstants.CENTER);
        tablePanel.setClippedTextColumn(1);
        tablePanel.setClippedTextColumn(4);
        tablePanel.setClippedTextColumn(5);
        tablePanel.setPreferredColumnWidthLimit(2, 150);
        tablePanel.setPreferredColumnWidthLimit(3, 140);
        tablePanel.setPreferredColumnWidthLimit(6, 130);
        tablePanel.setPreferredColumnWidthLimit(7, 130);
        // Pagination button gap for this table specifically.
        tablePanel.setPaginationBottomGap(5);

        add(tablePanel, BorderLayout.CENTER);
        reload();
    }

    public void showSingleEmployee(Employee employee) {
        displayedEmployees.clear();
        if (employee != null) {
            displayedEmployees.add(employee);
        }

        tablePanel.setPaginationEnabled(false);
        tablePanel.setRows(toRows(displayedEmployees));
    }

    public void clearTable() {
        displayedEmployees.clear();
        tablePanel.clearRows();
    }

    public void reload() {
        displayedEmployees.clear();
        int totalEmployees = repo.countEmployees();
        if (totalEmployees > 0) {
            displayedEmployees.addAll(repo.getEmployees(0, totalEmployees));
        }

        tablePanel.setPaginationEnabled(true);
        tablePanel.setRows(toRows(displayedEmployees));
    }

    private List<Object[]> toRows(List<Employee> employees) {
        List<Object[]> rows = new ArrayList<>();
        for (Employee employee : employees) {
            rows.add(new Object[]{
                    employee.getEMPLOYEE_CODE(),
                    employee.getEMP_NAME(),
                    employee.getNID(),
                    employee.getEMP_CONTNO(),
                    employee.getDEPARTMENT(),
                    employee.getDESIGNATION(),
                    employee.getJOINING_DATE(),
                    employee.getRESIGN_DATE(),
                    "View"
            });
        }
        return rows;
    }

    private void openEmployeeDetail(int row) {
        if (row < 0 || row >= displayedEmployees.size()) {
            return;
        }

        String employeeCode = displayedEmployees.get(row).getEMPLOYEE_CODE();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }

        new EmployeeDetailView(employeeCode);
    }
}
