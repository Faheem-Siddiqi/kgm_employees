package com.kgm.ui.panel;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.Employee;
import com.kgm.ui.EmployeeDetailView;
import com.kgm.util.CnicFormatter;
import com.kgm.util.DateDisplayFormatter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class EmployeeTablePanel extends JPanel {
    private static final int EMPLOYEE_CODE_COLUMN = 0;
    private static final int ACTION_COLUMN = 9;

    private static final String[] COLUMNS = {
            "Employee ID",
            "Name",
            "Designation",
            "Grade",
            "Department-Section",
            "Date of Joining",
            "Date of Resignation",
            "Phone",
            "CNIC",
            "Action"
    };

    private EmployeeRecordDao repo;
    private final UniversalTablePanel tablePanel;
    private final List<Employee> loadedEmployees = new ArrayList<>();
    private final List<Employee> displayedEmployees = new ArrayList<>();
    private Consumer<String> onFilterChanged;
    private String activeFilterLabel;

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
        tablePanel.setColumnAlignment(5, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(6, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(7, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(8, SwingConstants.LEFT);
        tablePanel.setPreferredColumnWidthLimit(4, 200);
        tablePanel.setPreferredColumnWidthLimit(5, 140);
        tablePanel.setPreferredColumnWidthLimit(6, 140);
        tablePanel.setPreferredColumnWidthLimit(7, 140);
        tablePanel.setPreferredColumnWidthLimit(8, 150);
        // Pagination button gap for this table specifically.
        tablePanel.setPaginationBottomGap(5);

        add(tablePanel, BorderLayout.NORTH);
        tablePanel.clearRows();
    }

    public void setRepository(EmployeeRecordDao repo) {
        this.repo = repo;
    }

    public void showLoading(String message) {
        activeFilterLabel = null;
        displayedEmployees.clear();
        String text = message == null || message.isBlank()
                ? "Loading employee records..."
                : message.trim();
        tablePanel.setEmptyText(text);
        tablePanel.clearRows();
    }

    public void showLoadFailed(String message) {
        activeFilterLabel = null;
        displayedEmployees.clear();
        String text = message == null || message.isBlank()
                ? "Employee records could not be loaded."
                : message.trim();
        tablePanel.setEmptyText(text);
        tablePanel.clearRows();
    }

    public void setEmployees(List<Employee> employees) {
        setEmployees(employees, toRows(employees));
    }

    public void setEmployees(List<Employee> employees, List<Object[]> preparedRows) {
        tablePanel.setEmptyText("No employee records yet");
        loadedEmployees.clear();
        if (employees != null) {
            loadedEmployees.addAll(employees);
        }

        activeFilterLabel = null;
        displayedEmployees.clear();
        displayedEmployees.addAll(loadedEmployees);
        tablePanel.setPaginationEnabled(true);
        tablePanel.setRows(preparedRows == null ? toRows(displayedEmployees) : preparedRows);
        if (onFilterChanged != null) {
            onFilterChanged.accept(null);
        }
    }

    /**
     * Registers a callback that fires when a chart filter is applied or cleared.
     * The String parameter is the filter label (null when cleared).
     */
    public void setOnFilterChanged(Consumer<String> onFilterChanged) {
        this.onFilterChanged = onFilterChanged;
    }

    /**
     * Returns the active chart filter label, or null if no filter is active.
     */
    public String getActiveFilterLabel() {
        return activeFilterLabel;
    }

    /**
     * Filters the table to show only employees matching the given column and value.
     * Supported columns: DEPARTMENT, GRADE, DESIGNATION, RESIGN_REASON
     */
    public void filterByColumn(String columnName, String value, String filterLabel) {
        Map<String, String> criteria = new LinkedHashMap<>();
        criteria.put(columnName, value);
        filterByColumns(criteria, filterLabel);
    }

    public void filterByColumns(Map<String, String> criteria, String filterLabel) {
        this.activeFilterLabel = filterLabel;
        displayedEmployees.clear();
        for (Employee emp : loadedEmployees) {
            if (matchesFilters(emp, criteria)) {
                displayedEmployees.add(emp);
            }
        }

        tablePanel.setPaginationEnabled(true);
        tablePanel.setRows(toRows(displayedEmployees));
        if (onFilterChanged != null) {
            onFilterChanged.accept(filterLabel);
        }
    }

    private boolean matchesFilters(Employee emp, Map<String, String> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            if (!matchesFilter(emp, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clears any active chart filter and reloads all employees.
     */
    public void clearFilter() {
        this.activeFilterLabel = null;
        displayedEmployees.clear();
        displayedEmployees.addAll(loadedEmployees);
        tablePanel.setPaginationEnabled(true);
        tablePanel.setRows(toRows(displayedEmployees));
        if (onFilterChanged != null) {
            onFilterChanged.accept(null);
        }
    }

    private boolean matchesFilter(Employee emp, String columnName, String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String empValue;
        switch (columnName.toUpperCase(Locale.ROOT)) {
            case "DEPARTMENT":
                empValue = emp.getDEPARTMENT();
                break;
            case "SECTION":
                empValue = emp.getSECTION();
                if (empValue == null || empValue.trim().isEmpty() || empValue.equalsIgnoreCase("N/A")) {
                    empValue = emp.getDynamicField("SECTION");
                }
                break;
            case "GRADE":
                empValue = emp.getGRADE();
                break;
            case "DESIGNATION":
                empValue = emp.getDESIGNATION();
                break;
            case "RESIGN_REASON":
                empValue = emp.getRESIGN_REASON();
                break;
            default:
                return true;
        }

        if (isUnassignedFilterValue(value)) {
            return isMissingFilterValue(empValue);
        }

        if ("RESIGN_REASON".equalsIgnoreCase(columnName)) {
            return resignReasonMatches(empValue, value);
        }

        if (empValue == null || empValue.trim().isEmpty() || empValue.equalsIgnoreCase("N/A")) {
            return false;
        }

        return empValue.trim().equalsIgnoreCase(value.trim());
    }

    private boolean isUnassignedFilterValue(String value) {
        return value != null && value.trim().equalsIgnoreCase("Unassigned");
    }

    private boolean isMissingFilterValue(String value) {
        if (value == null) {
            return true;
        }
        String text = value.trim();
        return text.isEmpty()
                || text.equalsIgnoreCase("N/A")
                || text.equalsIgnoreCase("NA")
                || text.equalsIgnoreCase("NULL")
                || text.equals("-");
    }

    private boolean resignReasonMatches(String reason, String filterValue) {
        if (reason == null || filterValue == null) {
            return false;
        }

        String employeeReason = reason.trim();
        String filter = normalizedResignReasonFilter(filterValue);
        if ("Others".equalsIgnoreCase(filter)) {
            return !employeeReason.equalsIgnoreCase("Layoff")
                    && !employeeReason.equalsIgnoreCase("Resignation");
        }
        return employeeReason.equalsIgnoreCase(filter);
    }

    private String normalizedResignReasonFilter(String value) {
        String text = value == null ? "" : value.trim();
        if ("Layoffs".equalsIgnoreCase(text)) {
            return "Layoff";
        }
        if ("Resignations".equalsIgnoreCase(text)) {
            return "Resignation";
        }
        if ("Others".equalsIgnoreCase(text) || "Other Exits".equalsIgnoreCase(text)) {
            return "Others";
        }
        return text;
    }

    public void showSingleEmployee(Employee employee) {
        this.activeFilterLabel = null;
        tablePanel.setEmptyText("No employee records yet");
        displayedEmployees.clear();
        if (employee != null) {
            displayedEmployees.add(employee);
        }

        tablePanel.setPaginationEnabled(false);
        tablePanel.setRows(toRows(displayedEmployees));
    }

    public void clearTable() {
        this.activeFilterLabel = null;
        tablePanel.setEmptyText("No employee records yet");
        displayedEmployees.clear();
        tablePanel.clearRows();
    }

    public void reload() {
        if (repo == null) {
            clearTable();
            return;
        }

        setEmployees(repo.getEmployeeSummaries());
    }

    /**
     * Formats the department display as "Department - Section" if section exists,
     * otherwise just shows the department name.
     */
    private static String formatDepartment(Employee employee) {
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

    public static List<Object[]> toRows(List<Employee> employees) {
        List<Object[]> rows = new ArrayList<>();
        if (employees == null) {
            return rows;
        }
        for (Employee employee : employees) {
            rows.add(new Object[]{
                    employee.getEMPLOYEE_CODE(),
                    employee.getEMP_NAME(),
                    employee.getDESIGNATION(),
                    employee.getGRADE() != null ? employee.getGRADE() : "",
                    formatDepartment(employee),
                    DateDisplayFormatter.format(employee.getJOINING_DATE()),
                    DateDisplayFormatter.format(employee.getRESIGN_DATE()),
                    employee.getEMP_CONTNO(),
                    CnicFormatter.format(employee.getNID()),
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
        new EmployeeDetailView(employeeCode);
        if (window != null) {
            SwingUtilities.invokeLater(window::dispose);
        }
    }
}
