package com.kgm.ui.panel;

import javax.swing.*;
import com.kgm.ui.EmployeeDetailView;
import com.kgm.ui.styling.UniversalTablePagination;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import com.kgm.dao.EmployeeRepositoryDao;
import com.kgm.model.Employee;

public class EmployeeTablePanel extends JPanel {
    private static final int EMPLOYEE_CODE_COLUMN = 0;
    private static final int ACTION_COLUMN = 12;
    private static final String ACTION_LABEL = "View";

    private JTable table;
    private DefaultTableModel model;
    private JPanel paginationPanel;
    private final List<Employee> allData = new ArrayList<>();
    // pagination editable - max 12 entries, no vertical scrolling
    private final int rowsPerPage = 12;
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
                "Action",
        });
        table = UniversalTablePagination.createEmployeeTable(model);
        JScrollPane scrollPane = createNoScrollScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (row >= 0 && column == ACTION_COLUMN) {
                    openEmployeeDetail(row);
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent event) {
                int column = table.columnAtPoint(event.getPoint());
                table.setCursor(Cursor.getPredefinedCursor(
                        column == ACTION_COLUMN ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
                ));
            }
        });

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
                e.getRESIGN_DATE(),
                ACTION_LABEL
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
                    e.getRESIGN_DATE(),
                    ACTION_LABEL
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

    private void openEmployeeDetail(int row) {
        String empCode = table.getValueAt(row, EMPLOYEE_CODE_COLUMN).toString();
        java.awt.Window window = SwingUtilities.getWindowAncestor(table);
        if (window != null) {
            window.dispose();
        }
        new EmployeeDetailView(empCode);
    }

    /**
     * Creates a scroll pane with no vertical scrolling that hugs the table content.
     * This ensures the table displays exactly the rows without scrolling, limited to 12 entries.
     */
    private JScrollPane createNoScrollScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new UniversalTablePagination.RoundedTableBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getViewport().setBorder(null);
        // Horizontal scroll as needed, no vertical scroll
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(96);
        // Set preferred size to hug the table content (12 rows max)
        scrollPane.setPreferredSize(table.getPreferredScrollableViewportSize());
        return scrollPane;
    }
}
