package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.EmployeeStorageStatusBanner;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.GenericRecordTablePanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.AppWindowStateHelper;
import com.kgm.ui.styling.AppTabsHelper;
import com.kgm.ui.styling.HomeViewHelper;
import com.kgm.ui.styling.ScreenHeaderStyleHelper;
import com.kgm.util.DateDisplayFormatter;
import com.kgm.util.EmployeeFieldDefinitionCache;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class MissingDataView extends JFrame {
    private static final int EMPLOYEE_CODE_COLUMN = 0;
    private static final int MISSING_COLUMN = 2;
    private static final int ACTION_COLUMN = 9;
    private static final Color CONTROL_BORDER = new Color(209, 213, 219);
    private static final Color CONTROL_TEXT = new Color(35, 43, 54);
    private static final Color CONTROL_MUTED = new Color(99, 115, 129);
    private static final Color CONTROL_HOVER = new Color(248, 250, 252);
    private static final Color CONTROL_SELECTED = new Color(241, 245, 249);
    private static final Color CONTROL_PRIMARY = new Color(37, 99, 235);
    private static final int SEARCH_RADIUS = 3;
    private static final int DROPDOWN_RADIUS = 8;

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

    private enum MissingDataType {
        FIELDS("Fields", "field", "fields"),
        DOCUMENTS("Documents", "document", "documents");

        private final String title;
        private final String singular;
        private final String plural;

        MissingDataType(String title, String singular, String plural) {
            this.title = title;
            this.singular = singular;
            this.plural = plural;
        }
    }

    private final GenericRecordTablePanel<EmployeeRecordDao.MissingEmployeeRow> tablePanel =
            new GenericRecordTablePanel<>(
                    COLUMNS,
                    "No missing required employee data",
                    this::toRow
            );

    private final List<EmployeeRecordDao.MissingEmployeeRow> allRows = new ArrayList<>();
    private JTextField employeeCodeSearchField;
    private JButton clearSearchButton;
    private JButton missingItemFilterButton;
    private JLabel filterStatusLabel;
    private JTabbedPane missingTypeTabs;
    private EmployeeStorageStatusBanner storageStatusBanner;
    private MissingDataType activeMissingType = MissingDataType.FIELDS;
    private final Set<String> selectedMissingItems = new LinkedHashSet<>();
    private final Map<EmployeeRecordDao.MissingEmployeeRow, Set<String>> missingFieldLabelsByRow =
            new IdentityHashMap<>();
    private final Map<EmployeeRecordDao.MissingEmployeeRow, Set<String>> missingDocumentLabelsByRow =
            new IdentityHashMap<>();
    private List<String> availableMissingItems = new ArrayList<>();
    private String pendingInitialMissingItem;
    private SwingWorker<List<EmployeeRecordDao.MissingEmployeeRow>, Void> loadWorker;
    private LoadingOverlay.Handle loadOverlay;
    private boolean returningHomeAfterStop;

    public MissingDataView() {
        this(false);
    }

    public MissingDataView(boolean showDocumentsTab) {
        this(showDocumentsTab, null);
    }

    public MissingDataView(boolean showDocumentsTab, String initialMissingItem) {
        activeMissingType = showDocumentsTab ? MissingDataType.DOCUMENTS : MissingDataType.FIELDS;
        pendingInitialMissingItem = cleanInitialMissingItem(initialMissingItem);
        setTitle("Missing Required Data");
        AppWindowStateHelper.lockFullSize(this);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        storageStatusBanner = new EmployeeStorageStatusBanner(this);

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        configureTable();

        showLoading("Preparing missing required data...");
        setVisible(true);

        SwingUtilities.invokeLater(this::reloadAsync);
    }

    @Override
    public void dispose() {
        if (storageStatusBanner != null) {
            storageStatusBanner.dispose();
            storageStatusBanner = null;
        }
        cancelActiveLoad();
        super.dispose();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.add(new HeaderPanel("Missing Required Data"), BorderLayout.NORTH);
        header.add(EmployeeStorageStatusBanner.stickyRow(storageStatusBanner), BorderLayout.SOUTH);
        return header;
    }

    private JComponent createMainContent() {
        JPanel main = new PageContentPanel();
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(createTitleRow(), BorderLayout.NORTH);
        topSection.add(createControlsStack(), BorderLayout.CENTER);

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
        JPanel row = ScreenHeaderStyleHelper.screenHeader(
                "Employees Missing Required Data",
                "Review employees with missing required fields or required documents.",
                () -> {
                    new HomeView();
                    dispose();
                }
        );
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        return row;
    }

    private JPanel createControlsStack() {
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(Color.WHITE);
        stack.add(createMissingTypeTabsRow());
        stack.add(createSearchRow());
        return stack;
    }

    private JPanel createMissingTypeTabsRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        missingTypeTabs = new JTabbedPane();
        missingTypeTabs.addTab(MissingDataType.FIELDS.title, createFilterTabContent());
        missingTypeTabs.addTab(MissingDataType.DOCUMENTS.title, createFilterTabContent());
        missingTypeTabs.setSelectedIndex(activeMissingType == MissingDataType.DOCUMENTS ? 1 : 0);
        AppTabsHelper.styleTabs(
                missingTypeTabs,
                new Insets(6, 0, 10, 0),
                new Insets(8, 0, 0, 0),
                new Insets(12, 14, 11, 14),
                8
        );
        missingTypeTabs.addChangeListener(event -> {
            MissingDataType nextType = missingTypeTabs.getSelectedIndex() == 1
                    ? MissingDataType.DOCUMENTS
                    : MissingDataType.FIELDS;
            if (activeMissingType != nextType) {
                activeMissingType = nextType;
                selectedMissingItems.clear();
                rebuildAvailableMissingItems();
                updateMissingItemFilterButton();
                applyEmployeeCodeFilter();
            }
        });
        missingTypeTabs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent event) {
                int tabIndex = missingTypeTabs.indexAtLocation(event.getX(), event.getY());
                if (tabIndex >= 0 && missingTypeTabs.isEnabledAt(tabIndex)) {
                    missingTypeTabs.setSelectedIndex(tabIndex);
                    missingTypeTabs.revalidate();
                    missingTypeTabs.repaint();
                }
            }
        });

        row.add(missingTypeTabs, BorderLayout.CENTER);
        return row;
    }

    private JPanel createFilterTabContent() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(0, 1));
        return panel;
    }

    private JPanel createSearchRow() {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setBackground(Color.WHITE);
        searchBox.setPreferredSize(new Dimension(430, 36));
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(200, 200, 200), SEARCH_RADIUS),
                BorderFactory.createEmptyBorder(0, 10, 0, 4)
        ));

        employeeCodeSearchField = HomeViewHelper.createSearchField("Search Employee ID");
        employeeCodeSearchField.setBorder(null);
        employeeCodeSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        employeeCodeSearchField.setForeground(CONTROL_TEXT);
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

        missingItemFilterButton = new JButton("Missing fields");
        styleMissingItemFilterButton();
        missingItemFilterButton.addActionListener(event -> showMissingItemFilterMenu());

        JPanel filterWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filterWrapper.setOpaque(false);
        filterWrapper.add(missingItemFilterButton);

        JPanel topRow = new JPanel(new BorderLayout(14, 0));
        topRow.setOpaque(false);
        topRow.add(controlsWrapper, BorderLayout.WEST);
        topRow.add(filterWrapper, BorderLayout.EAST);

        filterStatusLabel = new JLabel(" ");
        filterStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterStatusLabel.setForeground(CONTROL_MUTED);
        filterStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        row.add(topRow, BorderLayout.NORTH);
        row.add(filterStatusLabel, BorderLayout.CENTER);
        return row;
    }

    private void styleMissingItemFilterButton() {
        if (missingItemFilterButton == null) {
            return;
        }
        missingItemFilterButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        missingItemFilterButton.setForeground(CONTROL_TEXT);
        missingItemFilterButton.setBackground(Color.WHITE);
        missingItemFilterButton.setFocusPainted(false);
        missingItemFilterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        missingItemFilterButton.setPreferredSize(new Dimension(240, 36));
        missingItemFilterButton.setHorizontalAlignment(SwingConstants.LEADING);
        missingItemFilterButton.setLayout(new BorderLayout());
        missingItemFilterButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(CONTROL_BORDER, SEARCH_RADIUS),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        missingItemFilterButton.add(new JLabel(new ChevronDownIcon()), BorderLayout.EAST);
    }

    private void showMissingItemFilterMenu() {
        if (missingItemFilterButton == null || !missingItemFilterButton.isEnabled()) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(new RoundedLineBorder(CONTROL_BORDER, DROPDOWN_RADIUS));
        menu.setLayout(new BorderLayout());
        menu.setBackground(Color.WHITE);

        if (availableMissingItems.isEmpty()) {
            JMenuItem empty = new JMenuItem("No missing " + activeMissingType.plural);
            empty.setEnabled(false);
            menu.add(empty, BorderLayout.CENTER);
        } else {
            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.setBackground(Color.WHITE);
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JTextField search = HomeViewHelper.createSearchField(" Search missing " + activeMissingType.plural);
            search.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedLineBorder(CONTROL_BORDER, SEARCH_RADIUS),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)
            ));
            search.setPreferredSize(new Dimension(300, 34));
            search.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            JPanel optionList = new JPanel();
            optionList.setLayout(new BoxLayout(optionList, BoxLayout.Y_AXIS));
            optionList.setBackground(Color.WHITE);

            JScrollPane optionsScroll = new JScrollPane(optionList);
            optionsScroll.setBorder(BorderFactory.createEmptyBorder());
            optionsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            optionsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            optionsScroll.getVerticalScrollBar().setUnitIncrement(14);
            optionsScroll.setPreferredSize(new Dimension(320, 240));
            optionsScroll.getViewport().setBackground(Color.WHITE);

            JButton clear = new JButton("Clear selection");
            HomeViewHelper.styleClearButton(clear);
            clear.addActionListener(event -> {
                selectedMissingItems.clear();
                updateMissingItemFilterButton();
                applyEmployeeCodeFilter();
                rebuildMissingItemOptions(optionList, search.getText());
            });

            search.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    rebuildMissingItemOptions(optionList, search.getText());
                }

                public void removeUpdate(DocumentEvent event) {
                    rebuildMissingItemOptions(optionList, search.getText());
                }

                public void changedUpdate(DocumentEvent event) {
                    rebuildMissingItemOptions(optionList, search.getText());
                }
            });

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            footer.setOpaque(false);
            footer.add(clear);

            content.add(search, BorderLayout.NORTH);
            content.add(optionsScroll, BorderLayout.CENTER);
            content.add(footer, BorderLayout.SOUTH);
            menu.add(content, BorderLayout.CENTER);
            rebuildMissingItemOptions(optionList, "");
        }

        menu.show(missingItemFilterButton, 0, missingItemFilterButton.getHeight() + 4);
    }

    private void rebuildMissingItemOptions(JPanel optionList, String query) {
        optionList.removeAll();
        String normalizedQuery = normalized(query);
        int matches = 0;
        for (String itemLabel : availableMissingItems) {
            if (!normalizedQuery.isBlank() && !normalized(itemLabel).contains(normalizedQuery)) {
                continue;
            }
            optionList.add(new DropdownCheckRow(itemLabel));
            matches++;
        }
        if (matches == 0) {
            JLabel empty = new JLabel("No " + activeMissingType.plural + " found");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(CONTROL_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));
            optionList.add(empty);
        }
        optionList.revalidate();
        optionList.repaint();
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

        tablePanel.setPaginationEnabled(true);
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
        selectedMissingItems.clear();
        missingFieldLabelsByRow.clear();
        missingDocumentLabelsByRow.clear();
        availableMissingItems = new ArrayList<>();
        updateMissingItemFilterButton();
        refreshTable();
    }

    private void reloadAsync() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        showLoading("Loading employees with missing required data...");
        loadOverlay = LoadingOverlay.show(
                this,
                "Fetching Missing Data",
                "Fetching employees with missing required data...",
                this::stopLoadingAndReturnHome
        );

        SwingWorker<List<EmployeeRecordDao.MissingEmployeeRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EmployeeRecordDao.MissingEmployeeRow> doInBackground() {
                try (EmployeeRecordDao repo = new EmployeeRecordDao()) {
                    return repo.missingRequiredDataRows();
                }
            }

            @Override
            protected void done() {
                closeLoadOverlay();
                if (isCancelled()) {
                    return;
                }

                try {
                    allRows.clear();
                    allRows.addAll(get());
                    rebuildAvailableMissingItems();
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

    private void stopLoadingAndReturnHome() {
        returningHomeAfterStop = true;
        cancelActiveLoad();
        SwingUtilities.invokeLater(() -> {
            new HomeView();
            dispose();
        });
    }

    private void cancelActiveLoad() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }
        closeLoadOverlay();
    }

    private void closeLoadOverlay() {
        if (loadOverlay != null) {
            loadOverlay.close();
            loadOverlay = null;
        }
    }

    private void showLoadFailed(String message) {
        if (returningHomeAfterStop) {
            return;
        }
        allRows.clear();
        tablePanel.setEmptyText(message == null || message.isBlank()
                ? "Missing required data could not be loaded."
                : message.trim());

        tablePanel.clearRows();
        updateSearchStatus(0, 0, "");
        selectedMissingItems.clear();
        missingFieldLabelsByRow.clear();
        missingDocumentLabelsByRow.clear();
        availableMissingItems = new ArrayList<>();
        updateMissingItemFilterButton();
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
        int totalRowsForType = 0;
        for (EmployeeRecordDao.MissingEmployeeRow row : allRows) {
            if (!matchesActiveMissingType(row)) {
                continue;
            }
            if (!matchesSelectedMissingItems(row)) {
                continue;
            }
            totalRowsForType++;
            if (query.isBlank() || normalized(row.employeeCode()).contains(query)) {
                visibleRows.add(row);
            }
        }

        tablePanel.setEmptyText(emptyTableText(query));
        tablePanel.setRows(visibleRows);
        updateSearchStatus(visibleRows.size(), totalRowsForType, query);
        refreshTable();
    }

    private boolean matchesActiveMissingType(EmployeeRecordDao.MissingEmployeeRow row) {
        if (row == null) {
            return false;
        }
        return activeMissingType == MissingDataType.DOCUMENTS
                ? row.hasMissingDocuments()
                : row.hasMissingFields();
    }

    private boolean matchesSelectedMissingItems(EmployeeRecordDao.MissingEmployeeRow row) {
        if (selectedMissingItems.isEmpty()) {
            return true;
        }

        Set<String> rowItems = activeMissingType == MissingDataType.DOCUMENTS
                ? missingDocumentLabelsByRow.getOrDefault(row, Set.of())
                : missingFieldLabelsByRow.getOrDefault(row, Set.of());
        for (String selected : selectedMissingItems) {
            String normalizedSelected = normalized(selected);
            for (String rowItem : rowItems) {
                if (normalized(rowItem).equals(normalizedSelected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void applyPendingInitialSelection() {
        if (pendingInitialMissingItem == null || pendingInitialMissingItem.isBlank()) {
            return;
        }

        String initial = pendingInitialMissingItem;
        pendingInitialMissingItem = null;
        for (String item : availableMissingItems) {
            if (normalized(item).equals(normalized(initial))) {
                selectedMissingItems.add(item);
                return;
            }
        }
        selectedMissingItems.add(initial);
    }

    private void rebuildAvailableMissingItems() {
        missingFieldLabelsByRow.clear();
        missingDocumentLabelsByRow.clear();
        for (EmployeeRecordDao.MissingEmployeeRow row : allRows) {
            missingFieldLabelsByRow.put(row, missingFieldSet(row));
            missingDocumentLabelsByRow.put(row, missingDocumentSet(row));
        }

        availableMissingItems = requiredManagementLabels(activeMissingType == MissingDataType.DOCUMENTS);
        selectedMissingItems.retainAll(new LinkedHashSet<>(availableMissingItems));
        applyPendingInitialSelection();
        updateMissingItemFilterButton();
    }

    private String cleanInitialMissingItem(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> requiredManagementLabels(boolean documentField) {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>(EmployeeFieldDefinitionCache.fields());
        definitions.sort((left, right) -> {
            int heading = compareText(left.heading(), right.heading());
            if (heading != 0) {
                return heading;
            }
            int sort = Integer.compare(left.sortOrder(), right.sortOrder());
            if (sort != 0) {
                return sort;
            }
            return compareText(left.label(), right.label());
        });

        List<String> labels = new ArrayList<>();
        for (EmployeeFieldDefinition definition : definitions) {
            if (definition.documentField() != documentField
                    || !definition.requiredField()
                    || "ID".equalsIgnoreCase(definition.columnName())) {
                continue;
            }
            String label = definition.label() == null ? "" : definition.label().trim();
            if (!label.isBlank() && labels.stream().noneMatch(existing -> existing.equalsIgnoreCase(label))) {
                labels.add(label);
            }
        }
        return labels;
    }

    private int compareText(String left, String right) {
        String cleanLeft = left == null ? "" : left;
        String cleanRight = right == null ? "" : right;
        return String.CASE_INSENSITIVE_ORDER.compare(cleanLeft, cleanRight);
    }

    private Set<String> missingFieldSet(EmployeeRecordDao.MissingEmployeeRow row) {
        return missingItemSet(row == null ? null : row.missingFieldItems());
    }

    private Set<String> missingDocumentSet(EmployeeRecordDao.MissingEmployeeRow row) {
        return missingItemSet(row == null ? null : row.missingDocumentItems());
    }

    private Set<String> missingItemSet(String missingItems) {
        Set<String> items = new LinkedHashSet<>();
        if (missingItems == null || missingItems.isBlank()) {
            return items;
        }

        for (String part : missingItems.split(",")) {
            String label = part == null ? "" : part.trim();
            if (!label.isBlank()) {
                items.add(label);
            }
        }
        return items;
    }

    private void updateMissingItemFilterButton() {
        if (missingItemFilterButton == null) {
            return;
        }

        missingItemFilterButton.setEnabled(!availableMissingItems.isEmpty());
        missingItemFilterButton.setVisible(true);
        String text;
        if (selectedMissingItems.isEmpty()) {
            text = "Missing " + activeMissingType.plural;
        } else if (selectedMissingItems.size() == 1) {
            text = firstSelectedMissingItem();
        } else {
            text = selectedMissingItems.size() + " " + activeMissingType.plural + " selected";
        }
        missingItemFilterButton.setText(compactButtonText(text));
        missingItemFilterButton.setToolTipText(selectedMissingItems.isEmpty()
                ? "Filter by missing " + activeMissingType.singular
                : String.join(", ", selectedMissingItems));
    }

    private String firstSelectedMissingItem() {
        for (String selected : selectedMissingItems) {
            return selected;
        }
        return "Missing " + activeMissingType.plural;
    }

    private String searchQuery() {
        return normalized(employeeCodeSearchField == null ? "" : employeeCodeSearchField.getText());
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String emptyTableText(String query) {
        String typeLabel = activeMissingType == null ? "data" : activeMissingType.plural;
        if (!selectedMissingItems.isEmpty()) {
            return query.isBlank()
                    ? "No employees match the selected missing " + activeMissingType.plural
                    : "No employees match this Employee ID and selected missing " + activeMissingType.plural;
        }
        return query.isBlank()
                ? "No employees with missing required " + typeLabel
                : "No missing required " + typeLabel + " match this Employee ID";
    }

    private String compactButtonText(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() <= 22) {
            return text;
        }
        return text.substring(0, 19) + "...";
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
            filterStatusLabel.setText("Showing " + totalRows + " missing "
                    + activeMissingType.singular + " record" + plural(totalRows));
            return;
        }
        filterStatusLabel.setText("Showing " + visibleRows + " of " + totalRows + " matching "
                + activeMissingType.singular + " record" + plural(totalRows));
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
                missingItemsForActiveType(row),
                row.designation(),
                row.grade(),
                formatDepartment(row),
                DateDisplayFormatter.format(row.joiningDate()),
                DateDisplayFormatter.format(row.resignationDate()),
                row.phoneNumber(),
                "View"
        };
    }

    private String missingItemsForActiveType(EmployeeRecordDao.MissingEmployeeRow row) {
        if (row == null) {
            return "";
        }
        String missingItems = activeMissingType == MissingDataType.DOCUMENTS
                ? row.missingDocumentItems()
                : row.missingFieldItems();
        return missingItems == null || missingItems.isBlank() ? row.missingItems() : missingItems;
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

    private class DropdownCheckRow extends JPanel {
        private final String labelText;
        private final JCheckBox checkbox = new JCheckBox();
        private final JLabel label = new JLabel();

        private DropdownCheckRow(String labelText) {
            super(new BorderLayout(10, 0));
            this.labelText = labelText;
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));

            checkbox.setOpaque(false);
            checkbox.setFocusPainted(false);
            checkbox.setPreferredSize(new Dimension(20, 20));
            checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            label.setText(labelText);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setForeground(CONTROL_TEXT);

            add(checkbox, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
            refresh(false);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent event) {
                    refresh(true);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent event) {
                    refresh(false);
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    toggle();
                }
            });
            checkbox.addActionListener(event -> toggle());
        }

        private void toggle() {
            if (selectedMissingItems.contains(labelText)) {
                selectedMissingItems.remove(labelText);
            } else {
                selectedMissingItems.add(labelText);
            }
            refresh(true);
            updateMissingItemFilterButton();
            applyEmployeeCodeFilter();
        }

        private void refresh(boolean hover) {
            boolean selected = selectedMissingItems.contains(labelText);
            setBackground(selected ? CONTROL_SELECTED : hover ? CONTROL_HOVER : Color.WHITE);
            checkbox.setSelected(selected);
            label.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 13));
        }
    }

    private static class ChevronDownIcon implements Icon {
        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CONTROL_MUTED);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int midY = y + 6;
            g2.drawLine(x + 2, midY - 2, x + 6, midY + 2);
            g2.drawLine(x + 6, midY + 2, x + 10, midY - 2);
            g2.dispose();
        }
    }

    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        private RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }
}
