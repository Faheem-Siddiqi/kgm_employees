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

    private final GenericRecordTablePanel<EmployeeRecordDao.MissingEmployeeRow> tablePanel =
            new GenericRecordTablePanel<>(
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

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        configureTable();

        showLoading("Preparing missing required data...");
        setVisible(true);

        SwingUtilities.invokeLater(this::reloadAsync);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.add(new HeaderPanel("Missing Required Data"), BorderLayout.NORTH);
        return header;
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        main.add(createTitleRow(), BorderLayout.NORTH);
        main.add(createTableSection(), BorderLayout.CENTER);

        return main;
    }

    private JPanel createTitleRow() {
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel textBlock = new JPanel();
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        textBlock.setBackground(Color.WHITE);

        JLabel title = new JLabel("Employees Missing Required Data");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(17, 24, 39));

        JLabel subtitle = new JLabel("Review employees with missing required fields or required documents.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(99, 115, 129));

        textBlock.add(title);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(subtitle);

        JButton dashboard = new JButton("Dashboard");
        EmployeeRegistrationViewHelper.styleBackButton(dashboard);
        dashboard.addActionListener(event -> {
            new HomeView();
            dispose();
        });

        row.add(textBlock, BorderLayout.WEST);
        row.add(dashboard, BorderLayout.EAST);

        return row;
    }

    private JPanel createTableSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);

        /*
         * Main screen has no scroll.
         * Only this table area can scroll horizontally/vertically if table content needs it.
         */
        JScrollPane tableScroll = new JScrollPane(tablePanel);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getViewport().setBackground(Color.WHITE);

        section.add(tableScroll, BorderLayout.CENTER);

        return section;
    }

    private void configureTable() {
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

        tablePanel.setPreferredColumnWidthLimit(MISSING_COLUMN, 560);
        tablePanel.setPreferredColumnWidthLimit(5, 240);

        tablePanel.setPaginationBottomGap(18);

        /*
         * Important:
         * No fixed height.
         * Width is allowed to be wider than screen so table scroll can show horizontal scrollbar.
         */
        tablePanel.setPreferredSize(new Dimension(1250, tablePanel.getPreferredSize().height));
    }

    private void showLoading(String message) {
        tablePanel.setEmptyText(message == null || message.isBlank()
                ? "Loading missing required data..."
                : message.trim());

        tablePanel.clearRows();
        refreshTable();
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
                    refreshTable();
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
        refreshTable();
    }

    private void refreshTable() {
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

        new EmployeeDetailView(row.employeeCode());
        SwingUtilities.invokeLater(this::dispose);
    }
}   