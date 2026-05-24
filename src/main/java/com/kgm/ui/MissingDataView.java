package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.UniversalTablePanel;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.util.DateDisplayFormatter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MissingDataView extends JFrame {
    private static final int EMPLOYEE_CODE_COLUMN = 0;
    private static final int MISSING_COLUMN = 2;
    private static final int ACTION_COLUMN = 9;

    private static final String[] COLUMNS = {
            "Employee ID",
            "Name",
            "Missing",
            "Designation",
            "Grade",
            "Department-Section",
            "Date of Joining",
            "Date of Resignation",
            "Phone Number",
            "Action"
    };

    private final UniversalTablePanel tablePanel = new UniversalTablePanel(
            COLUMNS,
            "No missing required employee data"
    );
    private final List<EmployeeRecordDao.MissingEmployeeRow> rows = new ArrayList<>();

    public MissingDataView() {
        setTitle("Missing Required Data");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(new HeaderPanel("Missing Required Data"), BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeRegistrationViewHelper.createCenterWrapper();
        centerWrapper.add(createTitleRow(), pageConstraints(0, 0));
        centerWrapper.add(createTablePanel(), pageConstraints(1, 8));
        add(EmployeeRegistrationViewHelper.createPageScrollPane(centerWrapper), BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        reload();
        setVisible(true);
    }

    private JPanel createTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(25, 28, 16, 28));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(Color.WHITE);

        JLabel title = new JLabel("Employees Missing Required Data");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Shows employees missing required Field Management values or required document uploads in Graph");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(99, 115, 129));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        JButton dashboard = new JButton("Dashboard");
        EmployeeRegistrationViewHelper.styleBackButton(dashboard);
        dashboard.addActionListener(event -> {
            new HomeView();
            dispose();
        });

        row.add(titleBlock, BorderLayout.WEST);
        row.add(dashboard, BorderLayout.EAST);
        return row;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 28, 8, 28));

        tablePanel.setLinkColumn(EMPLOYEE_CODE_COLUMN, this::openEmployeeDetail, true);
        tablePanel.setActionColumn(ACTION_COLUMN, "View", this::openEmployeeDetail);
        tablePanel.setWrappedTextColumn(MISSING_COLUMN);
        tablePanel.setColumnAlignment(1, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(2, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(3, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(4, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(5, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(6, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(7, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(8, SwingConstants.CENTER);
        tablePanel.setPreferredColumnWidthLimit(MISSING_COLUMN, 420);
        tablePanel.setPreferredColumnWidthLimit(5, 220);
        tablePanel.setPaginationBottomGap(5);

        panel.add(tablePanel, BorderLayout.NORTH);
        return panel;
    }

    private void reload() {
        rows.clear();
        try (EmployeeRecordDao repo = new EmployeeRecordDao()) {
            rows.addAll(repo.missingRequiredDataRows());
        }
        tablePanel.setRows(toRows(rows));
    }

    private List<Object[]> toRows(List<EmployeeRecordDao.MissingEmployeeRow> missingRows) {
        List<Object[]> tableRows = new ArrayList<>();
        for (EmployeeRecordDao.MissingEmployeeRow row : missingRows) {
            tableRows.add(new Object[]{
                    row.employeeCode(),
                    row.name(),
                    row.missingItems(),
                    row.designation(),
                    row.grade(),
                    formatDepartment(row),
                    DateDisplayFormatter.format(row.joiningDate()),
                    DateDisplayFormatter.format(row.resignationDate()),
                    row.phoneNumber(),
                    "View"
            });
        }
        return tableRows;
    }

    private String formatDepartment(EmployeeRecordDao.MissingEmployeeRow row) {
        String department = clean(row.department());
        String section = clean(row.section());
        if (department.isBlank()) {
            return "";
        }
        return section.isBlank() ? department : department + " - " + section;
    }

    private String clean(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("N/A")) {
            return "";
        }
        return value.trim();
    }

    private void openEmployeeDetail(int row) {
        if (row < 0 || row >= rows.size()) {
            return;
        }
        String employeeCode = rows.get(row).employeeCode();
        new EmployeeDetailView(employeeCode);
        SwingUtilities.invokeLater(this::dispose);
    }

    private GridBagConstraints pageConstraints(int y, int bottomGap) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, bottomGap, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1.0;
        return gbc;
    }
}
