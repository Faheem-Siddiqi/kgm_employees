package com.kgm.ui;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.service.AuthService;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.UniversalTablePanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.util.EmployeeDocumentUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FieldManagementView extends JFrame {
    private static final int FIELD_COLUMN = 0;
    private static final int FIELD_LABEL = 1;
    private static final int FIELD_HEADING = 2;
    private static final int FIELD_CATEGORY = 3;
    private static final int FIELD_DATE = 4;
    private static final int FIELD_TEXT_AREA = 5;
    private static final int FIELD_DROPDOWN = 6;
    private static final int FIELD_ORIGIN = 8;
    private static final int FIELD_ACTION = 9;

    private static final int CATEGORY_SELECT = 0;
    private static final int CATEGORY_FIELDS = 2;
    private static final int CATEGORY_COUNT = 3;
    private static final int CATEGORY_ACTION = 4;

    private static final int REQUIRED_LABEL = 0;
    private static final int REQUIRED_COLUMN = 1;
    private static final int REQUIRED_HEADING = 2;
    private static final int REQUIRED_CATEGORY = 3;
    private static final int REQUIRED_STATUS = 4;
    private static final int REQUIRED_ACTION = 5;

    private static final String[] FIELD_COLUMNS = {
            "DB Column", "Label", "Heading", "Category", "Date", "Text Area", "Dropdown", "Locked", "Origin", "Action"
    };
    private static final String[] CATEGORY_COLUMNS = {
            "Select", "Category", "Fields", "Count", "Action"
    };
    private static final String[] REQUIRED_COLUMNS = {
            "Field", "DB Column", "Heading", "Category", "Required", "Action"
    };

    private final EmployeeFieldDefinitionDao dao = new EmployeeFieldDefinitionDao();
    private final Map<String, EmployeeFieldDefinition> definitionsByColumn = new LinkedHashMap<>();
    private final List<EmployeeFieldDefinition> allFields = new ArrayList<>();
    private final List<EmployeeFieldDefinition> displayedFields = new ArrayList<>();
    private final List<EmployeeFieldDefinition> requiredRows = new ArrayList<>();
    private final List<CategoryRow> categoryRows = new ArrayList<>();

    private UniversalTablePanel fieldTable;
    private UniversalTablePanel categoryTable;
    private UniversalTablePanel requiredTable;
    private JTextField searchField;
    private JComboBox<String> originFilter;
    private String selectedCategoryHeading;

    public FieldManagementView() {
        setTitle("Field Management");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(new HeaderPanel("Field Management"), BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeRegistrationViewHelper.createCenterWrapper();
        centerWrapper.add(createTitleRow(), pageConstraints(0, 0));

        JTabbedPane tabs = createTabs();
        centerWrapper.add(createTabsArea(tabs), pageConstraints(1, 8));

        JScrollPane pageScroll = EmployeeRegistrationViewHelper.createPageScrollPane(centerWrapper);
        tabs.addChangeListener(event -> SwingUtilities.invokeLater(() -> {
            centerWrapper.revalidate();
            centerWrapper.repaint();
            pageScroll.getVerticalScrollBar().setValue(0);
        }));
        EmployeeRegistrationViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);
        add(pageScroll, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        loadData();
        setVisible(true);
    }

    private JPanel createTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(25, 28, 0, 28));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(Color.WHITE);

        JLabel title = new JLabel("Database Field Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Manage DB fields, date behavior, document fields, and detail categories");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(99, 115, 129));

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        // Spacing
        titleBlock.add(Box.createVerticalStrut(16));

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

    private JTabbedPane createTabs() {
        fieldTable = createFieldTable();
        categoryTable = createCategoryTable();
        requiredTable = createRequiredTable();

        JTabbedPane tabs = new HugHeightTabbedPane();
        tabs.addTab("Fields", createFieldTab());
        tabs.addTab("Categories", createCategoryTab());
        tabs.addTab("Required Fields", createRequiredTab());
        EmployeeRegistrationViewHelper.styleTabs(
                tabs,
                new Insets(0, 28, 2, 28),
                new Insets(4, 0, 4, 0),
                new Insets(4, 12, 4, 12),
                3
        );
        tabs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent event) {
                int tabIndex = tabs.indexAtLocation(event.getX(), event.getY());
                if (tabIndex >= 0 && tabs.isEnabledAt(tabIndex)) {
                    tabs.setSelectedIndex(tabIndex);
                    tabs.revalidate();
                    tabs.repaint();
                }
            }
        });
        return tabs;
    }

    private JComponent createTabsArea(JTabbedPane tabs) {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(Color.WHITE);
        area.add(tabs, BorderLayout.CENTER);
        return area;
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

    private UniversalTablePanel createFieldTable() {
        UniversalTablePanel tablePanel = new UniversalTablePanel(FIELD_COLUMNS, "No fields match the current search");
        tablePanel.setMinimumViewportRows(12);
        tablePanel.setEmptyStateEnabled(false);
        tablePanel.setCheckboxColumn(FIELD_DATE, row -> row >= 0
                && row < displayedFields.size()
                && displayedFields.get(row).dateField());
        tablePanel.setCheckboxColumn(FIELD_TEXT_AREA, row -> row >= 0
                && row < displayedFields.size()
                && displayedFields.get(row).textAreaField());
        tablePanel.setStatusColumn(FIELD_ORIGIN, this::deleteFieldAtRow, row -> row >= 0
                && row < displayedFields.size()
                && displayedFields.get(row).customField());
        tablePanel.setActionColumn(FIELD_ACTION, "Edit", this::editFieldAtRow);
        tablePanel.setColumnAlignment(FIELD_DATE, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(FIELD_TEXT_AREA, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(FIELD_DROPDOWN, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(FIELD_CATEGORY, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(FIELD_HEADING, SwingConstants.LEFT);
        tablePanel.setColumnAlignment(FIELD_LABEL, SwingConstants.LEFT);
        tablePanel.setPreferredColumnWidthLimit(FIELD_COLUMN, 190);
        tablePanel.setPreferredColumnWidthLimit(FIELD_LABEL, 190);
        tablePanel.setPreferredColumnWidthLimit(FIELD_HEADING, 210);
        return tablePanel;
    }

    private UniversalTablePanel createCategoryTable() {
        UniversalTablePanel tablePanel = new UniversalTablePanel(CATEGORY_COLUMNS, "No detail categories available");
        tablePanel.setPaginationEnabled(false);
        tablePanel.setCheckboxColumn(CATEGORY_SELECT,
                row -> row >= 0 && row < categoryRows.size() && categoryRows.get(row).selected(),
                this::selectCategoryAtRow);
        tablePanel.setWrappedTextColumn(CATEGORY_FIELDS);
        tablePanel.setColumnAlignment(CATEGORY_COUNT, SwingConstants.CENTER);
        tablePanel.setActionColumn(CATEGORY_ACTION, "Edit Name", this::renameCategoryAtRow);
        tablePanel.setPreferredColumnWidthLimit(CATEGORY_SELECT, 86);
        tablePanel.setPreferredColumnWidthLimit(CATEGORY_FIELDS, 620);
        tablePanel.setPreferredColumnWidthLimit(CATEGORY_COUNT, 86);
        return tablePanel;
    }

    private UniversalTablePanel createRequiredTable() {
        UniversalTablePanel tablePanel = new UniversalTablePanel(REQUIRED_COLUMNS, "No fields available");
        tablePanel.setMinimumViewportRows(12);
        tablePanel.setActionColumn(REQUIRED_ACTION, "Edit", this::editRequiredAtRow);
        tablePanel.setColumnAlignment(REQUIRED_CATEGORY, SwingConstants.CENTER);
        tablePanel.setColumnAlignment(REQUIRED_STATUS, SwingConstants.CENTER);
        tablePanel.setPreferredColumnWidthLimit(REQUIRED_LABEL, 240);
        tablePanel.setPreferredColumnWidthLimit(REQUIRED_COLUMN, 190);
        tablePanel.setPreferredColumnWidthLimit(REQUIRED_HEADING, 230);
        tablePanel.setPreferredColumnWidthLimit(REQUIRED_STATUS, 96);
        return tablePanel;
    }

    private JPanel createFieldTab() {
        JPanel tab = createTabPanel();
        tab.add(createSectionHeader(
                "Fields",
                "Search by DB column or label, add custom fields, edit labels/headings, and mark date fields."
        ), BorderLayout.NORTH);
        tab.add(createFieldsBody(), BorderLayout.CENTER);
        return tab;
    }

    private JPanel createFieldsBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Color.WHITE);
        body.add(createFieldActionsRow(), BorderLayout.NORTH);
        body.add(fieldTable, BorderLayout.CENTER);
        return body;
    }

    private JPanel createFieldActionsRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);

        JButton add = new JButton("Add Field");
        JButton refresh = new JButton("Refresh");
        EmployeeRegistrationViewHelper.stylePrimaryButton(add);
        EmployeeRegistrationViewHelper.styleSecondaryButton(refresh);
        add.addActionListener(event -> addField());
        refresh.addActionListener(event -> loadData());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);
        actions.add(add);
        actions.add(refresh);

        row.add(createFieldFilters(), BorderLayout.WEST);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JPanel createFieldFilters() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setBackground(Color.WHITE);
        filters.add(createSearchBox());
        filters.add(createOriginFilter());
        return filters;
    }

    private JPanel createCategoryTab() {
        JPanel tab = createTabPanel();
        tab.add(createSectionHeader(
                "Categories",
                "Every category lists the fields it contains. Select any category or use Edit Name to update it everywhere."
        ), BorderLayout.NORTH);
        tab.add(createCategoryBody(), BorderLayout.CENTER);
        return tab;
    }

    private JPanel createCategoryBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        JComponent actions = createCategoryActionsRow();
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(actions);
        body.add(Box.createVerticalStrut(12));
        body.add(categoryTable);
        return body;
    }

    private JPanel createCategoryActionsRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);

        JButton rename = new JButton("Edit Selected");
        JButton delete = new JButton("Delete Selected");
        JButton refresh = new JButton("Refresh");
        EmployeeRegistrationViewHelper.stylePrimaryButton(rename);
        EmployeeRegistrationViewHelper.styleSecondaryButton(delete);
        EmployeeRegistrationViewHelper.styleSecondaryButton(refresh);
        rename.addActionListener(event -> renameSelectedCategory());
        delete.addActionListener(event -> deleteSelectedCategory());
        refresh.addActionListener(event -> loadData());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);
        actions.add(rename);
        actions.add(delete);
        actions.add(refresh);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JPanel createRequiredTab() {
        JPanel tab = createTabPanel();
        tab.add(createSectionHeader(
                "Required Fields",
                "Use Edit to set any employee field or document as required. The Home dashboard compliance chart uses these settings."
        ), BorderLayout.NORTH);
        tab.add(createRequiredBody(), BorderLayout.CENTER);
        return tab;
    }

    private JPanel createRequiredBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        JComponent actions = createRequiredActionsRow();
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        requiredTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(actions);
        body.add(Box.createVerticalStrut(12));
        body.add(requiredTable);
        return body;
    }

    private JPanel createRequiredActionsRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);

        JButton refresh = new JButton("Refresh");
        EmployeeRegistrationViewHelper.styleSecondaryButton(refresh);
        refresh.addActionListener(event -> loadData());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);
        actions.add(refresh);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JPanel createTabPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 14));
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(0, 28, 8, 28));
        return tab;
    }

    private JPanel createSectionHeader(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        // Spacing
        panel.setBorder(new EmptyBorder(8, 0, 12, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(35, 43, 54));
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(99, 115, 129));

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitleLabel);
        return panel;
    }

    private JPanel createSearchBox() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(Color.WHITE);

        searchField = new PlaceholderTextField("Search", 28);
        searchField.setToolTipText("Search by DB column or field label");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(null);

        JButton clear = new JButton("X");
        clear.setToolTipText("Clear search");
        clear.setFocusPainted(false);
        clear.setBorderPainted(false);
        clear.setContentAreaFilled(false);
        clear.setMargin(new Insets(0, 0, 0, 0));
        clear.setFont(new Font("Segoe UI", Font.BOLD, 11));
        clear.setForeground(new Color(99, 115, 129));
        clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clear.setPreferredSize(new Dimension(28, 28));
        setClearButtonActive(clear, false);
        clear.addActionListener(event -> {
            if (!searchField.getText().isEmpty()) {
                searchField.setText("");
                searchField.requestFocusInWindow();
            }
        });

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(Color.WHITE);
        searchBox.setPreferredSize(new Dimension(320, 34));
        searchBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        searchBox.add(searchField, BorderLayout.CENTER);
        searchBox.add(clear, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateSearch(clear);
            }

            public void removeUpdate(DocumentEvent event) {
                updateSearch(clear);
            }

            public void changedUpdate(DocumentEvent event) {
                updateSearch(clear);
            }
        });

        panel.add(searchBox);
        return panel;
    }

    private JComponent createOriginFilter() {
        originFilter = new JComboBox<>(new String[]{"All Fields", "Built-in", "Custom"});
        originFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        originFilter.setPreferredSize(new Dimension(130, 34));
        originFilter.setBackground(Color.WHITE);
        originFilter.setFocusable(false);
        originFilter.addActionListener(event -> applyFieldSearch());
        return originFilter;
    }

    private void updateSearch(JButton clear) {
        setClearButtonActive(clear, searchField != null && !searchField.getText().isEmpty());
        applyFieldSearch();
    }

    private void setClearButtonActive(JButton clear, boolean active) {
        clear.setText(active ? "X" : "");
        clear.setEnabled(active);
        clear.setForeground(active ? new Color(99, 115, 129) : new Color(255, 255, 255, 0));
        clear.setCursor(Cursor.getPredefinedCursor(active ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadData() {
        try {
            allFields.clear();
            definitionsByColumn.clear();
            allFields.addAll(dao.listFields());
            for (EmployeeFieldDefinition definition : allFields) {
                definitionsByColumn.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
            }
            applyFieldSearch();
            refreshCategoryTable();
            refreshRequiredTable();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            DialogHelper.error(this, "Field Management Load Failed", rootMessage(exception));
        }
    }

    private void applyFieldSearch() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        displayedFields.clear();
        for (EmployeeFieldDefinition definition : allFields) {
            if (matchesOriginFilter(definition) && (query.isEmpty() || matchesFieldSearch(definition, query))) {
                displayedFields.add(definition);
            }
        }
        fieldTable.setRows(toFieldRows(displayedFields));
    }

    private boolean matchesOriginFilter(EmployeeFieldDefinition definition) {
        String selected = originFilter == null || originFilter.getSelectedItem() == null
                ? "All Fields"
                : originFilter.getSelectedItem().toString();
        if ("Built-in".equals(selected)) {
            return !definition.customField();
        }
        if ("Custom".equals(selected)) {
            return definition.customField();
        }
        return true;
    }

    private boolean matchesFieldSearch(EmployeeFieldDefinition definition, String query) {
        return contains(definition.columnName(), query) || contains(definition.label(), query);
    }

    private List<Object[]> toFieldRows(List<EmployeeFieldDefinition> definitions) {
        List<Object[]> rows = new ArrayList<>();
        for (EmployeeFieldDefinition definition : definitions) {
            rows.add(new Object[]{
                    definition.columnName(),
                    definition.label(),
                    definition.heading(),
                    definition.usageLabel(),
                    definition.dateField(),
                    definition.textAreaField(),
                    dropdownLabel(definition),
                    definition.protectedField() ? "Yes" : "No",
                    definition.sourceLabel(),
                    "Edit"
            });
        }
        return rows;
    }

    private String dropdownLabel(EmployeeFieldDefinition definition) {
        if (!definition.dropdownField()) {
            return "No";
        }
        return definition.variableOptionField() ? "Variable" : "Fixed";
    }

    private void refreshCategoryTable() {
        Map<String, List<EmployeeFieldDefinition>> grouped = new LinkedHashMap<>();
        for (EmployeeFieldDefinition definition : allFields) {
            grouped.computeIfAbsent(definition.heading(), ignored -> new ArrayList<>()).add(definition);
        }

        if (selectedCategoryHeading != null && !grouped.containsKey(selectedCategoryHeading)) {
            selectedCategoryHeading = null;
        }

        categoryRows.clear();
        for (Map.Entry<String, List<EmployeeFieldDefinition>> entry : grouped.entrySet()) {
            categoryRows.add(new CategoryRow(
                    entry.getKey(),
                    entry.getValue(),
                    entry.getKey().equals(selectedCategoryHeading)
            ));
        }
        categoryTable.setRows(toCategoryRows(categoryRows));
    }

    private void refreshRequiredTable() {
        if (requiredTable == null) {
            return;
        }
        requiredTable.setRows(toRequiredRows(allFields));
    }

    private List<Object[]> toRequiredRows(List<EmployeeFieldDefinition> definitions) {
        List<Object[]> rows = new ArrayList<>();
        requiredRows.clear();
        for (EmployeeFieldDefinition definition : definitions) {
            if ("ID".equalsIgnoreCase(definition.columnName())) {
                continue;
            }
            requiredRows.add(definition);
            rows.add(new Object[]{
                    definition.label(),
                    definition.columnName(),
                    definition.heading(),
                    definition.documentField() ? "Documents" : "Fields",
                    definition.requiredField() ? "True" : "False",
                    "Edit"
            });
        }
        return rows;
    }

    private void editRequiredAtRow(int row) {
        if (row < 0 || row >= requiredRows.size()) {
            return;
        }
        EmployeeFieldDefinition selected = requiredRows.get(row);
        if ("ID".equalsIgnoreCase(selected.columnName())) {
            DialogHelper.warning(this, "System Field", "ID cannot be marked as required.");
            return;
        }
        if (!confirmPassword("Enter admin password to edit required status.")) {
            return;
        }
        Boolean required = showRequiredStatusDialog(selected);
        if (required == null || required == selected.requiredField()) {
            return;
        }
        try {
            dao.updateRequiredField(selected.columnName(), required);
            loadData();
            refreshOpenEmployeeForms();
            DialogHelper.success(this, "Required status updated.\nField: "
                    + selected.label()
                    + "\nRequired: "
                    + (required ? "True" : "False"));
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Required Field Update Failed", rootMessage(exception));
        }
    }

    private Boolean showRequiredStatusDialog(EmployeeFieldDefinition definition) {
        JComboBox<String> status = new JComboBox<>(new String[]{"True", "False"});
        status.setSelectedItem(definition.requiredField() ? "True" : "False");
        status.setPreferredSize(new Dimension(180, 34));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = formConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Field"), gbc);
        gbc.gridx = 1;
        form.add(new JLabel(definition.label() + " (" + definition.columnName() + ")"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Required"), gbc);
        gbc.gridx = 1;
        form.add(status, gbc);

        int result = DialogHelper.formOption(
                this,
                "Edit Required Status",
                form,
                "Save",
                "Cancel"
        );
        if (result != 0) {
            return null;
        }
        return "True".equals(status.getSelectedItem());
    }

    private List<Object[]> toCategoryRows(List<CategoryRow> categories) {
        List<Object[]> rows = new ArrayList<>();
        for (CategoryRow category : categories) {
            rows.add(new Object[]{
                    category.selected(),
                    category.heading(),
                    fieldLabelList(category.fields()),
                    category.fields().size(),
                    "Edit Name"
            });
        }
        return rows;
    }

    private String fieldLabelList(List<EmployeeFieldDefinition> fields) {
        StringBuilder labels = new StringBuilder();
        for (EmployeeFieldDefinition field : fields) {
            if (labels.length() > 0) {
                labels.append(", ");
            }
            labels.append(field.label());
        }
        return labels.toString();
    }

    private void selectCategoryAtRow(int row) {
        if (row < 0 || row >= categoryRows.size()) {
            return;
        }
        CategoryRow category = categoryRows.get(row);
        selectedCategoryHeading = category.heading().equals(selectedCategoryHeading) ? null : category.heading();
        refreshCategoryTable();
    }

    private void renameSelectedCategory() {
        if (selectedCategoryHeading == null) {
            DialogHelper.warning(this, "Select Category", "Select a category checkbox first.");
            return;
        }
        for (int row = 0; row < categoryRows.size(); row++) {
            if (categoryRows.get(row).heading().equals(selectedCategoryHeading)) {
                renameCategoryAtRow(row);
                return;
            }
        }
        DialogHelper.warning(this, "Select Category", "Selected category is no longer available.");
    }

    private void deleteSelectedCategory() {
        CategoryRow category = selectedCategory();
        if (category == null) {
            DialogHelper.warning(this, "Select Category", "Select a category checkbox first.");
            return;
        }
        if (!categoryCanBeDeleted(category)) {
            DialogHelper.warning(
                    this,
                    "Built-in Category",
                    "Only categories that contain custom fields can be deleted."
            );
            return;
        }
        if (!confirmPassword("Enter admin password to delete this category.")) {
            return;
        }

        int choice = DialogHelper.option(
                this,
                "Confirm Category Delete",
                "This will delete the category and all its fields:\n"
                        + category.heading()
                        + "\n\nFields: " + category.fields().size()
                        + "\nAll saved values in those DB columns will be deleted.",
                "Delete Category",
                "Cancel"
        );
        if (choice != 0) {
            return;
        }

        try {
            int deleted = dao.deleteHeading(category.heading());
            selectedCategoryHeading = null;
            EmployeeDocumentUtil.refreshDocumentTypes();
            loadData();
            refreshOpenEmployeeForms();
            DialogHelper.success(this, "Category deleted.\nFields deleted: " + deleted);
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Delete Category Failed", rootMessage(exception));
        }
    }

    private CategoryRow selectedCategory() {
        if (selectedCategoryHeading == null) {
            return null;
        }
        for (CategoryRow category : categoryRows) {
            if (category.heading().equals(selectedCategoryHeading)) {
                return category;
            }
        }
        return null;
    }

    private boolean categoryCanBeDeleted(CategoryRow category) {
        if (category == null || category.fields().isEmpty()) {
            return false;
        }
        for (EmployeeFieldDefinition field : category.fields()) {
            if (!field.customField() || field.protectedField()) {
                return false;
            }
        }
        return true;
    }

    private void renameCategoryAtRow(int row) {
        if (row < 0 || row >= categoryRows.size()) {
            return;
        }

        CategoryRow category = categoryRows.get(row);
        String renamed = showCategoryDialog(category);
        if (renamed == null) {
            return;
        }

        try {
            int updated = dao.renameHeading(category.heading(), renamed);
            selectedCategoryHeading = renamed;
            loadData();
            refreshOpenEmployeeForms();
            DialogHelper.success(this, "Category name updated.\nFields updated: " + updated);
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Edit Category Failed", rootMessage(exception));
        }
    }

    private String showCategoryDialog(CategoryRow category) {
        JTextField name = new JTextField(category.heading(), 26);
        JTextArea fields = new JTextArea(fieldLabelList(category.fields()), 4, 26);
        fields.setLineWrap(true);
        fields.setWrapStyleWord(true);
        fields.setEditable(false);
        fields.setFocusable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = formConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Category"), gbc);
        gbc.gridx = 1;
        form.add(name, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Fields"), gbc);
        gbc.gridx = 1;
        form.add(new JScrollPane(fields), gbc);

        while (true) {
            int result = DialogHelper.formOption(
                    this,
                    "Edit Category Name",
                    form,
                    "Save",
                    "Cancel"
            );
            if (result != 0) {
                return null;
            }

            String cleanName = name.getText().trim();
            if (cleanName.isEmpty()) {
                DialogHelper.warning(this, "Name Required", "Enter a category name.");
                continue;
            }
            if (categoryNameExists(cleanName, category.heading())) {
                DialogHelper.warning(this, "Category Exists", "Use a unique category name.");
                continue;
            }
            return cleanName;
        }
    }

    private boolean categoryNameExists(String candidate, String current) {
        for (CategoryRow category : categoryRows) {
            if (!category.heading().equalsIgnoreCase(current)
                    && category.heading().equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void addField() {
        FieldFormData data = showFieldDialog("Add Employee Field", null);
        if (data == null) {
            return;
        }

        try {
            EmployeeFieldDefinition added = dao.addField(
                    data.label(),
                    data.heading(),
                    data.documentField(),
                    data.dateField(),
                    data.dropdownField(),
                    data.variableOptionField(),
                    data.textAreaField(),
                    data.dropdownOptions()
            );
            EmployeeDocumentUtil.refreshDocumentTypes();
            loadData();
            refreshOpenEmployeeForms();
            DialogHelper.success(this, "Field added.\nColumn: " + added.columnName());
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Add Field Failed", rootMessage(exception));
        }
    }

    private void editFieldAtRow(int row) {
        EmployeeFieldDefinition selected = fieldAtRow(row);
        if (selected == null) {
            return;
        }
        if (!selected.customField()
                && !confirmPassword("Enter admin password to edit this built-in field.")) {
            return;
        }

        FieldFormData data = showFieldDialog("Edit Employee Field", selected);
        if (data == null) {
            return;
        }

        try {
            EmployeeFieldDefinition updated = dao.updateFieldSettings(
                    selected.columnName(),
                    data.label(),
                    data.heading(),
                    data.dateField(),
                    data.dropdownField(),
                    data.variableOptionField(),
                    data.textAreaField(),
                    data.dropdownOptions()
            );
            EmployeeDocumentUtil.refreshDocumentTypes();
            loadData();
            refreshOpenEmployeeForms();
            DialogHelper.success(this, "Field updated.\nColumn: " + updated.columnName());
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Edit Field Failed", rootMessage(exception));
        }
    }

    private void deleteFieldAtRow(int row) {
        EmployeeFieldDefinition selected = fieldAtRow(row);
        if (selected == null) {
            return;
        }
        if (!selected.customField()) {
            DialogHelper.warning(this, "Built-in Field", "Only custom fields can be deleted.");
            return;
        }
        if (!confirmPassword("Enter admin password to delete this field.")) {
            return;
        }

        int choice = DialogHelper.option(
                this,
                "Confirm Delete",
                "This will delete the DB column and all saved values for:\n"
                        + selected.label() + " (" + selected.columnName() + ")",
                "Delete Field",
                "Cancel"
        );
        if (choice != 0) {
            return;
        }

        try {
            dao.deleteField(selected.columnName());
            EmployeeDocumentUtil.refreshDocumentTypes();
            loadData();
            refreshOpenEmployeeForms();
            DialogHelper.success(this, "Field deleted.");
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Delete Field Failed", rootMessage(exception));
        }
    }

    private EmployeeFieldDefinition fieldAtRow(int row) {
        if (row < 0 || row >= displayedFields.size()) {
            return null;
        }
        EmployeeFieldDefinition selected = displayedFields.get(row);
        return definitionsByColumn.getOrDefault(
                selected.columnName().toUpperCase(Locale.ROOT),
                selected
        );
    }

    private FieldFormData showFieldDialog(String title, EmployeeFieldDefinition current) {
        JTextField column = new JTextField(current == null ? "" : current.columnName(), 24);
        column.setEditable(false);
        JTextField label = new JTextField(current == null ? "" : current.label(), 24);
        List<String> headings = dao.listDetailHeadings();
        JComboBox<String> heading = new JComboBox<>(headings.toArray(new String[0]));
        heading.setEditable(true);
        if (current != null) {
            heading.setSelectedItem(current.heading());
        }

        JComboBox<String> category = new JComboBox<>(new String[]{"Details", "Documents"});
        category.setSelectedItem(current != null && current.documentField() ? "Documents" : "Details");
        category.setEnabled(current == null);
        JCheckBox dateField = new JCheckBox("Use calendar for this field");
        dateField.setSelected(current != null && current.dateField());
        dateField.setOpaque(false);
        JCheckBox textAreaField = new JCheckBox("Use larger text area");
        textAreaField.setSelected(current != null && current.textAreaField());
        textAreaField.setOpaque(false);
        JCheckBox dropdownField = new JCheckBox("Use dropdown for this field");
        dropdownField.setSelected(current != null && current.dropdownField());
        dropdownField.setOpaque(false);
        JCheckBox variableOption = new JCheckBox("Allow custom value");
        variableOption.setSelected(current != null && current.variableOptionField());
        variableOption.setOpaque(false);
        OptionEditorPanel optionEditor = new OptionEditorPanel(
                current == null ? List.of() : current.dropdownOptionList()
        );
        JLabel variableLabel = new JLabel("Variable option");
        JLabel optionsLabel = new JLabel("Options");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        heading.setEnabled(!"Documents".equals(category.getSelectedItem()));
        dateField.setEnabled(!"Documents".equals(category.getSelectedItem()));
        dropdownField.setEnabled(!"Documents".equals(category.getSelectedItem()));
        dateField.addActionListener(event -> {
            if (dateField.isSelected()) {
                textAreaField.setSelected(false);
                dropdownField.setSelected(false);
                variableOption.setSelected(false);
            }
            refreshDropdownControls(form, category, dateField, textAreaField, dropdownField, variableOption, optionEditor, variableLabel, optionsLabel);
        });
        textAreaField.addActionListener(event -> {
            if (textAreaField.isSelected() && !"Documents".equals(category.getSelectedItem())) {
                dateField.setSelected(false);
                dropdownField.setSelected(false);
                variableOption.setSelected(false);
            }
            refreshDropdownControls(form, category, dateField, textAreaField, dropdownField, variableOption, optionEditor, variableLabel, optionsLabel);
        });
        dropdownField.addActionListener(event -> {
            if (dropdownField.isSelected() && !"Documents".equals(category.getSelectedItem())) {
                dateField.setSelected(false);
                textAreaField.setSelected(false);
            }
            refreshDropdownControls(form, category, dateField, textAreaField, dropdownField, variableOption, optionEditor, variableLabel, optionsLabel);
        });
        category.addActionListener(event -> {
            boolean documents = "Documents".equals(category.getSelectedItem());
            heading.setEnabled(!documents);
            dateField.setEnabled(!documents);
            textAreaField.setEnabled(!documents);
            dropdownField.setEnabled(!documents);
            if (documents) {
                heading.setSelectedItem("Documents");
                dateField.setSelected(false);
                textAreaField.setSelected(false);
                dropdownField.setSelected(false);
                variableOption.setSelected(false);
            }
            refreshDropdownControls(form, category, dateField, textAreaField, dropdownField, variableOption, optionEditor, variableLabel, optionsLabel);
        });

        GridBagConstraints gbc = formConstraints();

        int row = 0;
        if (current != null) {
            gbc.gridx = 0;
            gbc.gridy = row++;
            form.add(new JLabel("DB column"), gbc);
            gbc.gridx = 1;
            form.add(column, gbc);
        }

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Field label"), gbc);
        gbc.gridx = 1;
        form.add(label, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Type"), gbc);
        gbc.gridx = 1;
        form.add(category, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Category"), gbc);
        gbc.gridx = 1;
        form.add(heading, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Date"), gbc);
        gbc.gridx = 1;
        form.add(dateField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Text Area"), gbc);
        gbc.gridx = 1;
        form.add(textAreaField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Dropdown"), gbc);
        gbc.gridx = 1;
        form.add(dropdownField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(variableLabel, gbc);
        gbc.gridx = 1;
        form.add(variableOption, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(optionsLabel, gbc);
        gbc.gridx = 1;
        form.add(optionEditor, gbc);
        refreshDropdownControls(form, category, dateField, textAreaField, dropdownField, variableOption, optionEditor, variableLabel, optionsLabel);

        while (true) {
            int result = DialogHelper.formOption(
                    this,
                    title,
                    form,
                    "Save",
                    "Cancel"
            );
            if (result != 0) {
                return null;
            }

            String labelText = label.getText().trim();
            boolean documentField = "Documents".equals(category.getSelectedItem());
            Object headingValue = heading.getEditor().getItem();
            String headingText = documentField
                    ? current == null ? "Documents" : current.heading()
                    : headingValue == null ? "" : headingValue.toString().trim();
            boolean useDatePicker = !documentField && dateField.isSelected();
            boolean useTextArea = !documentField && !useDatePicker && textAreaField.isSelected();
            boolean useDropdown = !documentField && dropdownField.isSelected();
            List<String> options = useDropdown ? optionEditor.values() : List.of();
            String optionsText = String.join("\n", options);
            if (labelText.isEmpty()) {
                DialogHelper.warning(this, "Label Required", "Enter a field label.");
                continue;
            }
            if (!documentField && headingText.isEmpty()) {
                DialogHelper.warning(this, "Category Required", "Choose or type a category for this field.");
                continue;
            }
            if (useDropdown && optionsText.isEmpty()) {
                DialogHelper.warning(this, "Options Required", "Add at least one dropdown option.");
                continue;
            }
            return new FieldFormData(
                    labelText,
                    headingText,
                    documentField,
                    useDatePicker,
                    useDropdown,
                    useDropdown && variableOption.isSelected(),
                    useTextArea,
                    optionsText
            );
        }
    }

    private void refreshDropdownControls(
            JPanel form,
            JComboBox<String> category,
            JCheckBox dateField,
            JCheckBox textAreaField,
            JCheckBox dropdownField,
            JCheckBox variableOption,
            OptionEditorPanel optionEditor,
            JLabel variableLabel,
            JLabel optionsLabel
    ) {
        boolean documents = "Documents".equals(category.getSelectedItem());
        boolean dropdown = !documents && dropdownField.isSelected();
        dateField.setEnabled(!documents);
        textAreaField.setEnabled(!documents);
        dropdownField.setEnabled(!documents);
        if (documents || dateField.isSelected() || dropdown) {
            textAreaField.setSelected(false);
        }
        variableOption.setVisible(dropdown);
        variableOption.setEnabled(dropdown);
        variableLabel.setVisible(dropdown);
        optionsLabel.setVisible(dropdown);
        optionEditor.setVisible(dropdown);
        optionEditor.setOptionsEnabled(dropdown);
        if (!dropdown) {
            variableOption.setSelected(false);
        }

        form.revalidate();
        form.repaint();
        Window window = SwingUtilities.getWindowAncestor(form);
        if (window != null) {
            window.pack();
        }
    }

    private boolean confirmPassword() {
        return confirmPassword("Enter admin password to delete this field.");
    }

    private boolean confirmPassword(String message) {
        JPasswordField password = new JPasswordField(18);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.add(password, BorderLayout.CENTER);

        int result = DialogHelper.formOption(
                this,
                "Admin Password",
                panel,
                "Confirm",
                "Cancel"
        );
        if (result != 0) {
            return false;
        }

        boolean valid = AuthService.login("admin", new String(password.getPassword()));
        if (!valid) {
            DialogHelper.error(this, "Password Incorrect", "The admin password is incorrect.");
        }
        return valid;
    }

    private GridBagConstraints formConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        return gbc;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message == null || message.isBlank() ? throwable.getMessage() : message;
    }

    private void refreshOpenEmployeeForms() {
        for (Window window : Window.getWindows()) {
            if (window == this || !window.isDisplayable()) {
                continue;
            }
            if (window instanceof EmployeeRegistrationView registrationView) {
                registrationView.refreshDynamicFields();
            } else if (window instanceof EmployeeDetailView detailView) {
                detailView.refreshDynamicFields();
            }
        }
    }

    private record FieldFormData(
            String label,
            String heading,
            boolean documentField,
            boolean dateField,
            boolean dropdownField,
            boolean variableOptionField,
            boolean textAreaField,
            String dropdownOptions
    ) {
    }

    private record CategoryRow(String heading, List<EmployeeFieldDefinition> fields, boolean selected) {
    }

    private static class OptionEditorPanel extends JPanel {
        private final JPanel rows = new JPanel();
        private final JButton addOption = new JButton("Add Option");
        private final List<JTextField> fields = new ArrayList<>();

        OptionEditorPanel(List<String> options) {
            setLayout(new BorderLayout(0, 8));
            setOpaque(false);
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            rows.setOpaque(false);
            addOption.setFocusPainted(false);
            addOption.addActionListener(event -> addOptionRow(""));

            if (options == null || options.isEmpty()) {
                addOptionRow("");
            } else {
                for (String option : options) {
                    addOptionRow(option);
                }
            }

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            actions.setOpaque(false);
            actions.add(addOption);
            add(rows, BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
        }

        void addOptionRow(String value) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(0, 0, 6, 0));

            JTextField field = new JTextField(value == null ? "" : value.trim(), 22);
            JButton remove = new JButton("Remove");
            remove.setFocusPainted(false);
            remove.addActionListener(event -> {
                fields.remove(field);
                rows.remove(row);
                if (fields.isEmpty()) {
                    addOptionRow("");
                }
                revalidate();
                repaint();
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.pack();
                }
            });

            fields.add(field);
            row.add(field, BorderLayout.CENTER);
            row.add(remove, BorderLayout.EAST);
            rows.add(row);
            revalidate();
            repaint();
        }

        List<String> values() {
            List<String> values = new ArrayList<>();
            for (JTextField field : fields) {
                String value = field.getText().trim();
                if (value.isEmpty() || containsIgnoreCase(values, value)) {
                    continue;
                }
                values.add(value);
            }
            return values;
        }

        void setOptionsEnabled(boolean enabled) {
            addOption.setEnabled(enabled);
            for (Component row : rows.getComponents()) {
                row.setEnabled(enabled);
                if (row instanceof Container container) {
                    for (Component child : container.getComponents()) {
                        child.setEnabled(enabled);
                    }
                }
            }
        }

        private static boolean containsIgnoreCase(List<String> values, String candidate) {
            for (String value : values) {
                if (value.equalsIgnoreCase(candidate)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class HugHeightTabbedPane extends JTabbedPane {
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            Component selected = getSelectedComponent();
            if (selected == null) {
                return preferred;
            }

            int tallestTabContentHeight = 0;
            for (int index = 0; index < getTabCount(); index++) {
                Component component = getComponentAt(index);
                if (component != null) {
                    tallestTabContentHeight = Math.max(
                            tallestTabContentHeight,
                            component.getPreferredSize().height
                    );
                }
            }

            int tabChromeHeight = Math.max(0, preferred.height - tallestTabContentHeight);
            Dimension selectedSize = selected.getPreferredSize();
            return new Dimension(preferred.width, selectedSize.height + tabChromeHeight);
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setColor(new Color(130, 140, 150));
            copy.setFont(getFont());
            Insets insets = getInsets();
            FontMetrics metrics = copy.getFontMetrics();
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            copy.drawString(placeholder, insets.left, y);
            copy.dispose();
        }
    }
}
