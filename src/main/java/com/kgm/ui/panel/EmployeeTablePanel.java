package com.kgm.ui.panel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.kgm.dao.EmployeeRepository;
import com.kgm.model.Employee;

public class EmployeeTablePanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    private JPanel paginationPanel;
    private JLabel pageInfoLabel;

    private final List<Employee> allData = new ArrayList<>();

    private final int rowsPerPage = 2500;
    private int currentPage = 1;

    private final EmployeeRepository repo;

    // ================= CONSTRUCTOR =================
    public EmployeeTablePanel(EmployeeRepository repo) {

        this.repo = repo;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        model = new DefaultTableModel();

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

        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);

        styleTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        add(scrollPane, BorderLayout.CENTER);

        // ===== PAGINATION PANEL =====
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        paginationPanel.setBackground(Color.WHITE);

        // ===== PAGE INFO LABEL =====
        pageInfoLabel = new JLabel();
        pageInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pageInfoLabel.setForeground(new Color(100, 100, 100));
        pageInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pageInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== BOTTOM CONTAINER (VERTICAL) =====
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // margin 12

        JPanel infoWrapper = new JPanel(new BorderLayout());
        infoWrapper.setBackground(Color.WHITE);
        infoWrapper.add(pageInfoLabel, BorderLayout.CENTER);

        paginationPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottom.add(infoWrapper);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(paginationPanel);

        add(bottom, BorderLayout.SOUTH);

        // INIT
        loadPage(1);
        buildPagination();
    }

    // ================= STYLE TABLE =================
    private void styleTable() {

        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(Color.WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(100, 45));
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(new Color(60, 60, 60));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                label.setOpaque(true);
                label.setBackground(isSelected ? new Color(232, 244, 255) : Color.WHITE);
                label.setForeground(new Color(50, 50, 50));

                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));

                return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    // ================= LOAD PAGE =================
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
                    e.getDESIGNATION(),
                    e.getDESIGNATION(),
                    e.getGENDER(),
                    e.getRESIGN_REASON(),
                    e.getJOINING_DATE(),
                    e.getRESIGN_DATE()
            });
        }

        SwingUtilities.invokeLater(this::autoResizeColumns);
    }

    // ================= AUTO RESIZE =================
    private void autoResizeColumns() {

        for (int col = 0; col < table.getColumnCount(); col++) {

            int width = 60;

            for (int row = 0; row < table.getRowCount(); row++) {
                Component comp = table.prepareRenderer(
                        table.getCellRenderer(row, col), row, col);
                width = Math.max(comp.getPreferredSize().width + 20, width);
            }

            JTableHeader header = table.getTableHeader();
            Component headerComp = header.getDefaultRenderer()
                    .getTableCellRendererComponent(
                            table,
                            table.getColumnName(col),
                            false, false, 0, col);

            width = Math.max(width, headerComp.getPreferredSize().width + 20);

            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }
    }

    // ================= PAGINATION =================
    private void buildPagination() {

        paginationPanel.removeAll();

        int totalRecords = repo.countEmployees();

        int totalPages = Math.max(1,
                (int) Math.ceil(totalRecords / (double) rowsPerPage));

        int showing = Math.min(rowsPerPage,
                totalRecords - ((currentPage - 1) * rowsPerPage));

        pageInfoLabel.setText("Showing " + showing + " / " + totalRecords);

        JButton prev = new JButton("<");
        prev.setEnabled(currentPage > 1);
        prev.addActionListener(e -> {
            currentPage--;
            loadPage(currentPage);
            buildPagination();
        });
        paginationPanel.add(prev);

        for (int i = 1; i <= totalPages; i++) {

            JButton btn = new JButton(String.valueOf(i));
            btn.setPreferredSize(new Dimension(38, 32));
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (i == currentPage) {
                btn.setBackground(Color.BLACK);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(new Color(240, 240, 240));
                btn.setForeground(new Color(120, 120, 120));
            }

            int page = i;

            btn.addActionListener(e -> {
                currentPage = page;
                loadPage(page);
                buildPagination();
            });

            paginationPanel.add(btn);
        }

        JButton next = new JButton(">");
        next.setEnabled(currentPage < totalPages);
        next.addActionListener(e -> {
            currentPage++;
            loadPage(currentPage);
            buildPagination();
        });
        paginationPanel.add(next);

        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    // ================= API =================
    public void clearTable() {
        model.setRowCount(0);
    }
}