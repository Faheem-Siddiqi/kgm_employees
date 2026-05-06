package com.kgm.ui.panel;

import javax.swing.*;
import com.kgm.ui.EmployeeDetailView;
import com.kgm.ui.styling.UniversalTablePagination;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import com.kgm.dao.EmployeeRepositoryDao;
import com.kgm.model.Employee;

public class EmployeeTablePanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JPanel paginationPanel;
    private final List<Employee> allData = new ArrayList<>();
    // pagination editable
    private final int rowsPerPage = 25;
    private int currentPage = 1;
    private final EmployeeRepositoryDao repo;
    private JLabel showingLabel;

    public EmployeeTablePanel(EmployeeRepositoryDao repo) {
        this.repo = repo;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.setColumnIdentifiers(new String[] {
                "Employee ID",
                "Name",
                "Father Name",
                "CNIC",
                "Phone",
                "Email",
                "Department",
                "Designation",
                "Gender",
                "Reason",
                "Joining Date",
                "Leaving Date",
        });
        table = UniversalTablePagination.createEmployeeTable(model);
        JScrollPane scrollPane = UniversalTablePagination.createScrollPane(table, true);
        add(scrollPane, BorderLayout.CENTER);

        // event

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    // get Employee Code from first column
                    String empCode = table.getValueAt(row, 0).toString();
                    // close current window
                    java.awt.Window window = SwingUtilities.getWindowAncestor(table);
                    if (window != null) {
                        window.dispose();
                    }
                    // open detail page
                    new EmployeeDetailView(empCode);
                }
            }
        });
        // event
        paginationPanel = UniversalTablePagination.createPaginationButtonPanel();
        showingLabel = UniversalTablePagination.createShowingLabel();
        add(UniversalTablePagination.createPaginationContainer(showingLabel, paginationPanel), BorderLayout.SOUTH);
        loadPage(1);
        buildPagination();
    }

    // ================= SEARCH DISPLAY (NEW) =================
    public void showSingleEmployee(Employee e) {
        model.setRowCount(0);
        if (e == null)
            return;
        model.addRow(new Object[] {
                e.getEMPLOYEE_CODE(),
                e.getEMP_NAME(),
                e.getFATHER_NAME(),
                e.getNID(),
                e.getEMP_CONTNO(),
                e.getPERSONAL_EMAIL(),
                e.getDEPARTMENT(),
                e.getDESIGNATION(),
                e.getGENDER(),
                e.getRESIGN_REASON(),
                e.getJOINING_DATE(),
                e.getRESIGN_DATE()
        });
        UniversalTablePagination.showSingleRecord(showingLabel, paginationPanel);
    }

    private void loadPage(int page) {
        model.setRowCount(0);
        int offset = (page - 1) * rowsPerPage;
        List<Employee> list = repo.getEmployees(offset);
        allData.clear();
        allData.addAll(list);
        for (Employee e : list) {
            model.addRow(new Object[] {
                    e.getEMPLOYEE_CODE(),
                    e.getEMP_NAME(),
                    e.getFATHER_NAME(),
                    e.getNID(),
                    e.getEMP_CONTNO(),
                    e.getPERSONAL_EMAIL(),
                    e.getDEPARTMENT(),
                    e.getDESIGNATION(),
                    e.getGENDER(),
                    e.getRESIGN_REASON(),
                    e.getJOINING_DATE(),
                    e.getRESIGN_DATE()
            });
        }
        UniversalTablePagination.updateShowingLabel(showingLabel, currentPage, rowsPerPage, repo.countEmployees());
        SwingUtilities.invokeLater(() -> UniversalTablePagination.autoResizeColumns(table));
    }

    private void buildPagination() {
        UniversalTablePagination.buildPagination(
                paginationPanel,
                repo.countEmployees(),
                rowsPerPage,
                currentPage,
                page -> {
                    currentPage = page;
                    loadPage(page);
                    buildPagination();
                });
    }

    public void clearTable() {
        model.setRowCount(0);
    }

    public void reload() {
        currentPage = 1;
        loadPage(currentPage);
        buildPagination();
    }
}
