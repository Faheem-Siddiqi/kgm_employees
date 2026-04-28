package com.kgm.ui.panel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeTablePanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    private JPanel paginationPanel;

    private final List<Object[]> allData = new ArrayList<>();

    private final int rowsPerPage = 15;
    private int currentPage = 1;

    public EmployeeTablePanel() {

        // FULL VIEW CONTAINER
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // ================= TABLE MODEL =================
        model = new DefaultTableModel();

        model.setColumnIdentifiers(new String[]{
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
                "Address"
        });

        table = new JTable(model);

        // IMPORTANT FOR FULL WIDTH SCROLLING
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);

        styleTable();

        // ================= SCROLL PANE =================
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        // ================= DUMMY DATA =================
        generateDummyData();

        // ================= PAGINATION =================
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        paginationPanel.setBackground(Color.WHITE);
        paginationPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(null);
        bottom.add(paginationPanel, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);

        // INIT
        refresh(1);
    }

    // ================= TABLE STYLE =================
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

                label.setBackground(isSelected
                        ? new Color(232, 244, 255)
                        : Color.WHITE);

                label.setForeground(new Color(50, 50, 50));

                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));

                return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    // ================= DUMMY DATA =================
    private void generateDummyData() {

        for (int i = 1; i <= 100; i++) {

            allData.add(new Object[]{
                    "EMP-" + String.format("%04d", i),
                    "Employee " + i,
                    "Father " + i,
                    "42101-12345" + i,
                    "0300-00000" + i,
                    "emp" + i + "@company.com",
                    (i % 3 == 0) ? "IT" : (i % 3 == 1) ? "HR" : "Finance",
                    (i % 4 == 0) ? "Developer" : "Officer",
                    (i % 2 == 0) ? "Male" : "Female",
                    "N/A",
                    "2025-01-" + String.format("%02d", (i % 28) + 1),
                    (i % 4 == 0) ? "2025-12-31" : "N/A",
                    (i % 5 == 0) ? "Lahore" : "Karachi"
            });
        }
    }

    // ================= LOAD PAGE =================
    private void loadPage(int page) {

        model.setRowCount(0);

        int start = (page - 1) * rowsPerPage;
        int end = Math.min(start + rowsPerPage, allData.size());

        for (int i = start; i < end; i++) {
            model.addRow(allData.get(i));
        }

        SwingUtilities.invokeLater(this::autoResizeColumns);
    }

    // ================= AUTO COLUMN WIDTH =================
    private void autoResizeColumns() {

        for (int col = 0; col < table.getColumnCount(); col++) {

            int width = 60;

            for (int row = 0; row < table.getRowCount(); row++) {

                Component comp = table.prepareRenderer(
                        table.getCellRenderer(row, col),
                        row, col
                );

                width = Math.max(comp.getPreferredSize().width + 20, width);
            }

            JTableHeader header = table.getTableHeader();
            Component headerComp = header.getDefaultRenderer()
                    .getTableCellRendererComponent(
                            table,
                            table.getColumnName(col),
                            false, false, 0, col
                    );

            width = Math.max(width, headerComp.getPreferredSize().width + 20);

            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }
    }

    // ================= PAGINATION =================
    private void buildPagination() {

        paginationPanel.removeAll();

        int totalPages = (int) Math.ceil(allData.size() / (double) rowsPerPage);

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
            btn.addActionListener(e -> refresh(page));

            paginationPanel.add(btn);
        }

        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    // ================= REFRESH =================
    private void refresh(int page) {

        currentPage = page;

        loadPage(page);
        buildPagination();
    }

    // ================= PUBLIC API =================
    public void addEmployee(Object[] row) {
        allData.add(row);
        refresh(currentPage);
    }

    public void clearTable() {
        allData.clear();
        model.setRowCount(0);
        refresh(1);
    }
}