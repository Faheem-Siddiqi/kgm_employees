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
    private static final int ACTION_COLUMN = 7;

    private static final String[] COLUMNS = {
            "Employee ID",
            "Name",
            "Designation",
            "Grade",
            "Department",
            "Phone",
            "CNIC",
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

        tablePanel = new UniversalTablePanel(COLUMNS, "No employee records yet");
        tablePanel.setLinkColumn(EMPLOYEE_CODE_COLUMN, this::openEmployeeDetail, true);
        tablePanel.setActionColumn(ACTION_COLUMN, "View", this::openEmployeeDetail);
        tablePanel.setColumnAlignment(1, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(2, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(3, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(4, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(5, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(6, SwingConstants.LEFT);
        tablePanel.setPreferredColumnWidthLimit(4, 200);
        tablePanel.setPreferredColumnWidthLimit(5, 140);
        tablePanel.setPreferredColumnWidthLimit(6, 150);
        // Pagination button gap for this table specifically.
        tablePanel.setPaginationBottomGap(5);

        add(tablePanel, BorderLayout.NORTH);
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

    /**
     * Formats the department display as "Department - Section" if section exists,
     * otherwise just shows the department name.
     */
    private String formatDepartment(Employee employee) {
        String department = employee.getDEPARTMENT();
        if (department == null || department.trim().isEmpty() || department.equalsIgnoreCase("N/A")) {
            return "";
        }

        String section = employee.getSECTION();
        if (section == null || section.trim().isEmpty() || section.equalsIgnoreCase("N/A")) {
            section = employee.getDynamicField("SECTION");
        }

        if (section != null && !section.trim().isEmpty() && !section.equalsIgnoreCase("N/A")) {
            return department + " - " + section;
        }

        return department;
    }

    private List<Object[]> toRows(List<Employee> employees) {
        List<Object[]> rows = new ArrayList<>();
        for (Employee employee : employees) {
            rows.add(new Object[]{
                    employee.getEMPLOYEE_CODE(),
                    employee.getEMP_NAME(),
                    employee.getDESIGNATION(),
                    employee.getGRADE() != null ? employee.getGRADE() : "",
                    formatDepartment(employee),
                    employee.getEMP_CONTNO(),
                    employee.getNID(),
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
