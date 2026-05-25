package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.GenericRecordTablePanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.util.DateDisplayFormatter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

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

    private final GenericRecordTablePanel<EmployeeRecordDao.MissingEmployeeRow> tablePanel = new GenericRecordTablePanel<>(
            COLUMNS,
            "No missing required employee data",
            this::toRow
    );
    private SwingWorker<List<EmployeeRecordDao.MissingEmployeeRow>, Void> loadWorker;

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
        centerWrapper.add(createTablePanel(), pageConstraints(1, 16));

        JScrollPane pageScroll = EmployeeRegistrationViewHelper.createPageScrollPane(centerWrapper);

        // Allows horizontal scrolling when table content becomes wider than screen.
        pageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pageScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        EmployeeRegistrationViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);

        add(pageScroll, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        showLoading("Preparing missing required data...");
        setVisible(true);

        SwingUtilities.invokeLater(this::reloadAsync);
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

        // Extra bottom spacing prevents the first/only row from looking cut.
        panel.setBorder(BorderFactory.createEmptyBorder(0, 28, 28, 28));

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

        // Wider important columns so long missing-field text is readable.
        tablePanel.setPreferredColumnWidthLimit(MISSING_COLUMN, 560);
        tablePanel.setPreferredColumnWidthLimit(5, 240);

        // More bottom gap under pagination/table area.
        tablePanel.setPaginationBottomGap(18);

        // Minimum/preferred size gives the table stable height and allows page-level horizontal scroll.
        tablePanel.setMinimumSize(new Dimension(1250, 180));
        tablePanel.setPreferredSize(new Dimension(1250, 240));

        // Keep loader and loaded data in same stable area.
        panel.add(tablePanel, BorderLayout.CENTER);

        return panel;
    }

    private void showLoading(String message) {
        tablePanel.setEmptyText(message == null || message.isBlank()
                ? "Loading missing required data..."
                : message.trim());

        tablePanel.clearRows();
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private void reloadAsync() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        showLoading("Loading employees with missing required data...");

        SwingWorker<List<EmployeeRecordDao.MissingEmployeeRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EmployeeRecordDao.MissingEmployeeRow> doInBackground() {
                try (EmployeeRecordDao repo = new EmployeeRecordDao()) {
                    return repo.missingRequiredDataRows();
                }
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    tablePanel.setEmptyText("No missing required employee data");
                    tablePanel.setRows(get());

                    tablePanel.revalidate();
                    tablePanel.repaint();
                } catch (CancellationException ignored) {
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadFailed("Missing required data loading was interrupted.");
                } catch (ExecutionException exception) {
                    exception.printStackTrace();
                    showLoadFailed("Missing required data could not be loaded.");
                } finally {
                    if (loadWorker == this) {
                        loadWorker = null;
                    }
                }
            }
        };

        loadWorker = worker;
        worker.execute();
    }

    private void showLoadFailed(String message) {
        tablePanel.setEmptyText(message == null || message.isBlank()
                ? "Missing required data could not be loaded."
                : message.trim());

        tablePanel.clearRows();
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private Object[] toRow(EmployeeRecordDao.MissingEmployeeRow row) {
        return new Object[]{
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
        };
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

    private void openEmployeeDetail(EmployeeRecordDao.MissingEmployeeRow row) {
        if (row == null) {
            return;
        }

        String employeeCode = row.employeeCode();

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

        // Keeps extra vertical space below the table instead of centering loaded data.
        gbc.weighty = y == 1 ? 1.0 : 0.0;

        return gbc;
    }
}
