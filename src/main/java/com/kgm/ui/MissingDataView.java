package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.GenericRecordTablePanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.util.DateDisplayFormatter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private final List<EmployeeRecordDao.MissingEmployeeRow> allRows = new ArrayList<>();
    private JTextField employeeCodeSearchField;
    private JButton clearSearchButton;
    private JLabel filterStatusLabel;
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

    private JComponent createMainContent() {
        JPanel main = new PageContentPanel();
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(createTitleRow(), BorderLayout.NORTH);
        topSection.add(createSearchRow(), BorderLayout.CENTER);

        main.add(topSection, BorderLayout.NORTH);
        main.add(createTableSection(), BorderLayout.CENTER);

        JScrollPane pageScroll = new JScrollPane(main);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);
        pageScroll.getViewport().setBackground(Color.WHITE);
        return pageScroll;
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

    private JPanel createSearchRow() {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setBackground(Color.WHITE);
        searchBox.setPreferredSize(new Dimension(430, 36));
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(0, 10, 0, 4)
        ));

        employeeCodeSearchField = HomeViewHelper.createSearchField("Search Employee ID");
        employeeCodeSearchField.setBorder(null);
        employeeCodeSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        employeeCodeSearchField.setForeground(new Color(35, 43, 54));
        employeeCodeSearchField.setBackground(Color.WHITE);
        employeeCodeSearchField.addActionListener(event -> applyEmployeeCodeFilter());

        clearSearchButton = new JButton("Clear");
        HomeViewHelper.styleClearButton(clearSearchButton);
        HomeViewHelper.setTextButtonEnabled(clearSearchButton, false);
        clearSearchButton.addActionListener(event -> {
            employeeCodeSearchField.setText("");
            employeeCodeSearchField.requestFocusInWindow();
        });

        employeeCodeSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                searchChanged();
            }

            public void removeUpdate(DocumentEvent event) {
                searchChanged();
            }

            public void changedUpdate(DocumentEvent event) {
                searchChanged();
            }
        });

        searchBox.add(employeeCodeSearchField, BorderLayout.CENTER);
        searchBox.add(clearSearchButton, BorderLayout.EAST);

        JButton searchButton = new JButton("Search");
        HomeViewHelper.styleSearchButton(searchButton);
        searchButton.addActionListener(event -> applyEmployeeCodeFilter());

        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setOpaque(false);
        controls.add(searchBox, BorderLayout.CENTER);
        controls.add(searchButton, BorderLayout.EAST);

        JPanel controlsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controlsWrapper.setOpaque(false);
        controlsWrapper.add(controls);

        filterStatusLabel = new JLabel(" ");
        filterStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterStatusLabel.setForeground(new Color(99, 115, 129));

        row.add(controlsWrapper, BorderLayout.WEST);
        row.add(filterStatusLabel, BorderLayout.CENTER);
        return row;
    }

    private JPanel createTableSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.add(tablePanel, BorderLayout.NORTH);
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

        tablePanel.setPaginationEnabled(false);
        tablePanel.setHugRows(true);
        tablePanel.setMinimumViewportRows(0);
        tablePanel.setPaginationBottomGap(18);
    }

    private void showLoading(String message) {
        allRows.clear();
        tablePanel.setEmptyText(message == null || message.isBlank()
                ? "Loading missing required data..."
                : message.trim());

        tablePanel.clearRows();
        updateSearchStatus(0, 0, "");
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
                    allRows.clear();
                    allRows.addAll(get());
                    applyEmployeeCodeFilter();
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
        allRows.clear();
        tablePanel.setEmptyText(message == null || message.isBlank()
                ? "Missing required data could not be loaded."
                : message.trim());

        tablePanel.clearRows();
        updateSearchStatus(0, 0, "");
        refreshTable();
    }

    private void searchChanged() {
        String query = searchQuery();
        HomeViewHelper.setTextButtonEnabled(clearSearchButton, !query.isBlank());
        applyEmployeeCodeFilter();
    }

    private void applyEmployeeCodeFilter() {
        String query = searchQuery();
        List<EmployeeRecordDao.MissingEmployeeRow> visibleRows = new ArrayList<>();
        if (query.isBlank()) {
            visibleRows.addAll(allRows);
        } else {
            for (EmployeeRecordDao.MissingEmployeeRow row : allRows) {
                if (normalized(row.employeeCode()).contains(query)) {
                    visibleRows.add(row);
                }
            }
        }

        tablePanel.setEmptyText(emptyTableText(query));
        tablePanel.setRows(visibleRows);
        updateSearchStatus(visibleRows.size(), allRows.size(), query);
        refreshTable();
    }

    private String searchQuery() {
        return normalized(employeeCodeSearchField == null ? "" : employeeCodeSearchField.getText());
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String emptyTableText(String query) {
        return query.isBlank()
                ? "No missing required employee data"
                : "No missing required employee data matches this Employee ID";
    }

    private void updateSearchStatus(int visibleRows, int totalRows, String query) {
        if (filterStatusLabel == null) {
            return;
        }
        if (totalRows <= 0) {
            filterStatusLabel.setText(" ");
            return;
        }
        if (query == null || query.isBlank()) {
            filterStatusLabel.setText("Showing " + totalRows + " missing record" + plural(totalRows));
            return;
        }
        filterStatusLabel.setText("Showing " + visibleRows + " of " + totalRows + " matching Employee ID");
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
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

    private static class PageContentPanel extends JPanel implements Scrollable {
        private PageContentPanel() {
            super(new BorderLayout());
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(90, visibleRect.height - 90);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
