package com.kgm.ui.panel;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.component.UniversalTextArea;
import com.kgm.ui.styling.EmployeeAdditionalDetailsPanelHelper;
import com.kgm.ui.styling.EmployeeRegistrationFormPanelHelper;
import com.kgm.util.DateDisplayFormatter;
import com.kgm.util.EmployeeBasicFieldUtil;
import com.kgm.util.EmployeeFieldDefinitionCache;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployeeAdditionalDetailsPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final int DROPDOWN_SEARCH_DEBOUNCE_MS = 350;
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color MISSING_BORDER = new Color(220, 38, 38);

    private final Employee data;
    private final List<EmployeeFieldDefinition> definitions;
    private final Map<String, JTextField> textFieldMap = new LinkedHashMap<>();
    private final Map<String, UniversalTextArea> textAreaMap = new LinkedHashMap<>();
    private final Map<String, JComboBox<String>> dropdownFieldMap = new LinkedHashMap<>();
    private final Map<String, UniversalDatePicker> dateFieldMap = new LinkedHashMap<>();
    private final Map<String, Boolean> dateDirtyMap = new LinkedHashMap<>();
    private final Map<String, Boolean> editableColumns = new LinkedHashMap<>();
    private final Map<String, Boolean> dirtyColumns = new LinkedHashMap<>();
    private final List<SectionView> sectionViews = new ArrayList<>();
    private final List<JButton> breadcrumbButtons = new ArrayList<>();
    private JTextField searchField;
    private JButton clearSearchButton;
    private JButton missingFieldsChip;
    private JLabel searchStatusLabel;
    private JPanel sectionsContainer;
    private JLabel emptySearchLabel;
    private JScrollPane sectionNavScroller;
    private Component sectionNavTopGap;
    private Component sectionNavGap;
    private Boolean lastSingleColumnLayout;
    private JComponent topAnchor;
    private Runnable pendingChangesListener;
    private boolean loadingValues;

    public EmployeeAdditionalDetailsPanel() {
        this(null);
    }

    public EmployeeAdditionalDetailsPanel(Employee data) {
        this(data, loadDefaultDefinitions());
    }

    public EmployeeAdditionalDetailsPanel(Employee data, List<EmployeeFieldDefinition> definitions) {
        this.data = data;
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
        EmployeeAdditionalDetailsPanelHelper.stylePanel(this);
        add(EmployeeAdditionalDetailsPanelHelper.createContent(buildUI()), BorderLayout.NORTH);
    }

    private JPanel buildUI() {
        JPanel root = EmployeeAdditionalDetailsPanelHelper.createRootPanel();
        Map<String, List<EmployeeFieldDefinition>> grouped = groupByHeading(definitions);

        sectionViews.clear();
        topAnchor = createSearchHeader(definitions.size());
        root.add(topAnchor);

        List<String> headings = new ArrayList<>(grouped.keySet());
        List<JComponent> sectionRefs = new ArrayList<>();
        JPanel sectionNav = null;
        if (!headings.isEmpty()) {
            sectionNav = EmployeeAdditionalDetailsPanelHelper.createBreadcrumbPanel();
            sectionNavScroller = EmployeeAdditionalDetailsPanelHelper.createBreadcrumbScroller(sectionNav);
            sectionNavTopGap = Box.createVerticalStrut(16);
            sectionNavGap = Box.createVerticalStrut(20);
            root.add(sectionNavTopGap);
            root.add(sectionNavScroller);
            root.add(sectionNavGap);
        }
        sectionsContainer = EmployeeAdditionalDetailsPanelHelper.createSectionsContainer();
        root.add(sectionsContainer);

        for (int index = 0; index < headings.size(); index++) {
            String heading = headings.get(index);
            JComponent section = createSection(heading, grouped.get(heading));
            sectionRefs.add(section);
        }

        if (headings.isEmpty()) {
            JLabel empty = EmployeeAdditionalDetailsPanelHelper.createSectionHeader("No additional fields available");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            sectionsContainer.add(empty);
        } else {
            emptySearchLabel = EmployeeAdditionalDetailsPanelHelper.createEmptyStateLabel("No fields match this search.");
            rebuildVisibleSections("");
            root.add(EmployeeAdditionalDetailsPanelHelper.createReturnToTopPanel(() -> scrollToComponent(topAnchor)));
            installBreadcrumbLinks(sectionNav, headings, sectionRefs);
            filterToMissingFieldsOnly();
        }

        installResponsiveLayoutRefresh(root);
        updateSearchStatus(definitions.size(), definitions.size(), "");
        return root;
    }

    private JComponent createSearchHeader(int totalFields) {
        searchStatusLabel = EmployeeAdditionalDetailsPanelHelper.createSearchStatusLabel(statusText(totalFields, missingFieldCount()));
        searchField = new PlaceholderTextField("Search Other Fields");
        searchField.setToolTipText("Search field label, section, value, or DB column");
        clearSearchButton = new JButton("Clear");

        EmployeeAdditionalDetailsPanelHelper.styleSearchField(searchField);
        EmployeeAdditionalDetailsPanelHelper.styleClearButton(clearSearchButton);

        clearSearchButton.addActionListener(event -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                applyFieldFilter();
            }

            public void removeUpdate(DocumentEvent event) {
                applyFieldFilter();
            }

            public void changedUpdate(DocumentEvent event) {
                applyFieldFilter();
            }
        });

        return EmployeeAdditionalDetailsPanelHelper.createSearchHeader(
                searchStatusLabel,
                EmployeeAdditionalDetailsPanelHelper.createSearchPanel(searchField, clearSearchButton, null)
        );
    }

    private JButton createMissingFieldsChip() {
        missingFieldsChip = EmployeeAdditionalDetailsPanelHelper.createMissingFieldsCta(buildMissingFieldsChipLabel());
        missingFieldsChip.addActionListener(event -> filterToMissingFieldsOnly());
        return missingFieldsChip;
    }

    private String buildMissingFieldsChipLabel() {
        return "Missing Fields (" + missingFieldCount() + ")";
    }

    private void updateMissingFieldsChipDisplay() {
        if (missingFieldsChip != null) {
            missingFieldsChip.setText(buildMissingFieldsChipLabel());
            EmployeeAdditionalDetailsPanelHelper.refreshChipSize(missingFieldsChip);
            missingFieldsChip.revalidate();
            missingFieldsChip.repaint();
        }
    }

    private void filterToMissingFieldsOnly() {
        searchField.setText("");
        setActiveBreadcrumb(missingFieldsChip);
        FilterSummary summary = rebuildVisibleSectionsForMissingOnly();
        updateSearchStatus(summary.visibleFields(), summary.totalFields(), "");
        revalidate();
        repaint();
    }

    private FilterSummary rebuildVisibleSectionsForMissingOnly() {
        int visibleFields = 0;
        int totalFields = 0;
        boolean hasVisibleSection = false;
        if (sectionsContainer == null) {
            return new FilterSummary(0, 0);
        }

        sectionsContainer.removeAll();
        if (sectionNavScroller != null) {
            sectionNavScroller.setVisible(true);
        }
        if (sectionNavTopGap != null) {
            sectionNavTopGap.setVisible(true);
        }
        if (sectionNavGap != null) {
            sectionNavGap.setVisible(true);
        }

        for (SectionView section : sectionViews) {
            List<FieldView> missingInSection = new ArrayList<>();

            for (FieldView field : section.fields()) {
                totalFields++;
                String column = field.definition().columnName();
                boolean isMissing = isEditableColumn(column) && isEmpty(currentFieldValueByColumn(column));
                if (isMissing) {
                    missingInSection.add(field);
                    visibleFields++;
                }
            }

            if (!missingInSection.isEmpty()) {
                if (hasVisibleSection) {
                    sectionsContainer.add(Box.createVerticalStrut(14));
                }
                rebuildSectionGrid(section, missingInSection);
                sectionsContainer.add(section.panel());
                hasVisibleSection = true;
            }
        }

        if (!hasVisibleSection && emptySearchLabel != null) {
            sectionsContainer.add(emptySearchLabel);
        }

        sectionsContainer.revalidate();
        sectionsContainer.repaint();
        return new FilterSummary(visibleFields, totalFields);
    }

    private static List<EmployeeFieldDefinition> loadDefaultDefinitions() {
        try {
            return EmployeeFieldDefinitionCache.detailFields();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return List.of();
        }
    }

    private Map<String, List<EmployeeFieldDefinition>> groupByHeading(List<EmployeeFieldDefinition> definitions) {
        Map<String, List<EmployeeFieldDefinition>> grouped = new LinkedHashMap<>();
        for (EmployeeFieldDefinition definition : definitions) {
            grouped.computeIfAbsent(definition.heading(), ignored -> new ArrayList<>()).add(definition);
        }
        return grouped;
    }

    private void installBreadcrumbLinks(JPanel breadcrumb, List<String> labels, List<JComponent> targets) {
        if (breadcrumb == null) {
            return;
        }
        breadcrumbButtons.clear();
        missingFieldsChip = createMissingFieldsChip();
        breadcrumbButtons.add(missingFieldsChip);
        breadcrumb.add(missingFieldsChip);
        for (int index = 0; index < labels.size(); index++) {
            JButton link = EmployeeAdditionalDetailsPanelHelper.createBreadcrumbLink(labels.get(index));
            JComponent target = targets.get(index);
            link.addActionListener(event -> {
                setActiveBreadcrumb(link);
                scrollToComponent(target);
            });
            breadcrumbButtons.add(link);
            breadcrumb.add(link);
        }
    }

    private void setActiveBreadcrumb(JButton activeButton) {
        for (JButton button : breadcrumbButtons) {
            EmployeeAdditionalDetailsPanelHelper.setBreadcrumbActive(button, button == activeButton);
        }
    }

    private void scrollToComponent(JComponent component) {
        if (component == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Container parent = component.getParent();
            if (parent instanceof JComponent parentComponent) {
                Rectangle bounds = component.getBounds();
                bounds.y = Math.max(0, bounds.y - 12);
                bounds.height = component.getHeight() + 24;
                parentComponent.scrollRectToVisible(bounds);
                return;
            }
            component.scrollRectToVisible(new Rectangle(0, 0, component.getWidth(), component.getHeight()));
        });
    }

    private JPanel createSection(String title, List<EmployeeFieldDefinition> definitions) {
        JPanel section = EmployeeAdditionalDetailsPanelHelper.createSectionPanel();
        JLabel header = EmployeeAdditionalDetailsPanelHelper.createSectionHeader(title);
        List<FieldView> fieldViews = new ArrayList<>();
        section.add(header, BorderLayout.NORTH);
        JPanel grid = buildGrid(definitions, fieldViews);
        section.add(grid, BorderLayout.CENTER);
        sectionViews.add(new SectionView(title, section, grid, fieldViews));
        return section;
    }

    private JPanel buildGrid(List<EmployeeFieldDefinition> definitions, List<FieldView> fieldViews) {
        JPanel grid = EmployeeAdditionalDetailsPanelHelper.createGridPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (int index = 0; index < definitions.size(); index += 2) {
            EmployeeFieldDefinition first = definitions.get(index);
            EmployeeFieldDefinition second = index + 1 < definitions.size() ? definitions.get(index + 1) : null;
            if (EmployeeBasicFieldUtil.isMultilineField(first)
                    && (second == null || !EmployeeBasicFieldUtil.isMultilineField(second))) {
                addFullWidthField(grid, gbc, row++, first, fieldViews);
                if (second != null) {
                    addSingleField(grid, gbc, row++, second, fieldViews);
                }
                continue;
            }
            if (second != null
                    && EmployeeBasicFieldUtil.isMultilineField(second)
                    && !EmployeeBasicFieldUtil.isMultilineField(first)) {
                addSingleField(grid, gbc, row++, first, fieldViews);
                addFullWidthField(grid, gbc, row++, second, fieldViews);
                continue;
            }

            addPairFields(grid, gbc, row++, first, second, fieldViews);
        }
        return grid;
    }

    private void addSingleField(
            JPanel grid,
            GridBagConstraints gbc,
            int row,
            EmployeeFieldDefinition definition,
            List<FieldView> fieldViews
    ) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        grid.add(createTrackedField(definition, fieldViews), gbc);

        gbc.gridx = 1;
        grid.add(EmployeeAdditionalDetailsPanelHelper.createGridFiller(), gbc);
    }

    private void addFullWidthField(
            JPanel grid,
            GridBagConstraints gbc,
            int row,
            EmployeeFieldDefinition definition,
            List<FieldView> fieldViews
    ) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        grid.add(createTrackedField(definition, fieldViews), gbc);
        gbc.gridwidth = 1;
    }

    private void addPairFields(
            JPanel grid,
            GridBagConstraints gbc,
            int row,
            EmployeeFieldDefinition first,
            EmployeeFieldDefinition second,
            List<FieldView> fieldViews
    ) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        grid.add(createTrackedField(first, fieldViews), gbc);

        gbc.gridx = 1;
        if (second == null) {
            grid.add(EmployeeAdditionalDetailsPanelHelper.createGridFiller(), gbc);
        } else {
            grid.add(createTrackedField(second, fieldViews), gbc);
        }
    }

    private JPanel createTrackedField(EmployeeFieldDefinition definition, List<FieldView> fieldViews) {
        JPanel field = createField(definition);
        fieldViews.add(new FieldView(definition, field));
        return field;
    }

    private JPanel createField(EmployeeFieldDefinition definition) {
        JPanel panel = EmployeeAdditionalDetailsPanelHelper.createFieldPanel();
        JLabel label = EmployeeAdditionalDetailsPanelHelper.createFieldLabel(definition.label());
        String value = valueFor(definition.columnName());
        boolean editable = isEmpty(value);
        editableColumns.put(definition.columnName(), editable);
        dirtyColumns.put(definition.columnName(), false);
        JComponent field;
        if (EmployeeBasicFieldUtil.isDateField(definition)) {
            UniversalDatePicker datePicker = EmployeeAdditionalDetailsPanelHelper.createDateField(parseDate(value));
            datePicker.addDateChangeListener(() -> {
                if (isEditableColumn(definition.columnName())) {
                    dateDirtyMap.put(definition.columnName(), true);
                    markDirty(definition.columnName());
                }
            });
            dateFieldMap.put(definition.columnName(), datePicker);
            dateDirtyMap.put(definition.columnName(), false);
            field = datePicker;
        } else if (EmployeeBasicFieldUtil.isDropdownField(definition)) {
            JComboBox<String> combo = EmployeeAdditionalDetailsPanelHelper.createDropdownField(
                    EmployeeBasicFieldUtil.dropdownOptions(definition, true),
                    isEmpty(value) ? "" : value,
                    definition.variableOptionField()
            );
            installDynamicDropdownSearch(definition, combo);
            combo.addItemListener(event -> markDirty(definition.columnName()));
            Component editor = combo.getEditor() == null ? null : combo.getEditor().getEditorComponent();
            if (editor instanceof JTextField editorField) {
                installDocumentListener(definition.columnName(), editorField);
            }
            dropdownFieldMap.put(definition.columnName(), combo);
            field = combo;
        } else if (EmployeeBasicFieldUtil.isMultilineField(definition)) {
            UniversalTextArea textArea = new UniversalTextArea(editable ? "" : displayValue(value));
            installDocumentListener(definition.columnName(), textArea.textArea());
            textAreaMap.put(definition.columnName(), textArea);
            field = textArea;
        } else {
            JTextField textField = EmployeeAdditionalDetailsPanelHelper.createField(editable ? "" : displayValue(value));
            textField.setEditable(editable);
            installDocumentListener(definition.columnName(), textField);
            textFieldMap.put(definition.columnName(), textField);
            field = textField;
        }

        applyFieldEditability(field, editable);
        updateFieldBorder(definition.columnName());
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void installDynamicDropdownSearch(EmployeeFieldDefinition definition, JComboBox<String> combo) {
        if (!definition.variableOptionField()) {
            return;
        }
        DropdownFieldSupport.installAsyncSearch(
                combo,
                query -> new EmployeeFieldDefinitionDao().searchDistinctEmployeeValues(
                        definition.columnName(),
                        query,
                        25
                ),
                DROPDOWN_SEARCH_DEBOUNCE_MS
        );
    }

    private void installResponsiveLayoutRefresh(JComponent root) {
        root.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                boolean singleColumn = useSingleColumnLayout();
                if (lastSingleColumnLayout != null && lastSingleColumnLayout == singleColumn) {
                    return;
                }
                lastSingleColumnLayout = singleColumn;
                SwingUtilities.invokeLater(() -> {
                    FilterSummary summary = rebuildVisibleSections(currentQuery());
                    updateSearchStatus(summary.visibleFields(), summary.totalFields(), currentQuery());
                });
            }
        });
        SwingUtilities.invokeLater(() -> {
            lastSingleColumnLayout = useSingleColumnLayout();
            rebuildVisibleSections(currentQuery());
        });
    }

    private void applyFieldFilter() {
        String query = currentQuery();
        FilterSummary summary = rebuildVisibleSections(query);
        updateSearchStatus(summary.visibleFields(), summary.totalFields(), query);
        revalidate();
        repaint();
    }

    private FilterSummary rebuildVisibleSections(String query) {
        int visibleFields = 0;
        int totalFields = 0;
        boolean hasVisibleSection = false;
        if (sectionsContainer == null) {
            return new FilterSummary(0, 0);
        }

        sectionsContainer.removeAll();
        boolean searching = !query.isBlank();
        if (sectionNavScroller != null) {
            sectionNavScroller.setVisible(!searching);
        }
        if (sectionNavTopGap != null) {
            sectionNavTopGap.setVisible(!searching);
        }
        if (sectionNavGap != null) {
            sectionNavGap.setVisible(!searching);
        }

        for (SectionView section : sectionViews) {
            boolean headingMatches = !query.isEmpty() && matchesText(section.heading(), query);
            List<FieldView> visibleInSection = new ArrayList<>();

            for (FieldView field : section.fields()) {
                totalFields++;
                boolean fieldVisible = query.isEmpty()
                        || headingMatches
                        || matchesField(field.definition(), query);
                if (fieldVisible) {
                    visibleInSection.add(field);
                    visibleFields++;
                }
            }

            if (!visibleInSection.isEmpty()) {
                if (hasVisibleSection) {
                    sectionsContainer.add(Box.createVerticalStrut(14));
                }
                rebuildSectionGrid(section, visibleInSection);
                sectionsContainer.add(section.panel());
                hasVisibleSection = true;
            }
        }

        if (!hasVisibleSection && !query.isBlank() && emptySearchLabel != null) {
            sectionsContainer.add(emptySearchLabel);
        }

        sectionsContainer.revalidate();
        sectionsContainer.repaint();
        return new FilterSummary(visibleFields, totalFields);
    }

    private void rebuildSectionGrid(SectionView section, List<FieldView> visibleFields) {
        section.grid().removeAll();
        if (useSingleColumnLayout()) {
            rebuildSingleColumnGrid(section.grid(), visibleFields);
        } else {
            rebuildTwoColumnGrid(section.grid(), visibleFields);
        }
        section.grid().revalidate();
        section.grid().repaint();
    }

    private void rebuildSingleColumnGrid(JPanel grid, List<FieldView> visibleFields) {
        for (int row = 0; row < visibleFields.size(); row++) {
            addVisibleField(grid, visibleFields.get(row), row, 0, 1, 1.0);
        }
    }

    private void rebuildTwoColumnGrid(JPanel grid, List<FieldView> visibleFields) {
        int row = 0;
        for (int index = 0; index < visibleFields.size(); index++) {
            FieldView first = visibleFields.get(index);
            if (EmployeeBasicFieldUtil.isMultilineField(first.definition())) {
                addVisibleField(grid, first, row++, 0, 2, 1.0);
                continue;
            }

            FieldView second = index + 1 < visibleFields.size() ? visibleFields.get(index + 1) : null;
            if (second != null && !EmployeeBasicFieldUtil.isMultilineField(second.definition())) {
                addVisibleField(grid, first, row, 0, 1, 0.5);
                addVisibleField(grid, second, row++, 1, 1, 0.5);
                index++;
            } else {
                addVisibleField(grid, first, row, 0, 1, 0.5);
                grid.add(EmployeeAdditionalDetailsPanelHelper.createGridFiller(), fillerConstraints(row));
                row++;
            }
        }
    }

    private void addVisibleField(
            JPanel grid,
            FieldView field,
            int row,
            int column,
            int width,
            double weight
    ) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.gridx = column;
        gbc.gridwidth = width;
        gbc.weightx = weight;
        gbc.insets = new Insets(7, 12, 7, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        field.panel().setVisible(true);
        grid.add(field.panel(), gbc);
    }

    private GridBagConstraints fillerConstraints(int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(7, 12, 7, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private boolean useSingleColumnLayout() {
        int width = sectionsContainer != null && sectionsContainer.getWidth() > 0
                ? sectionsContainer.getWidth()
                : getWidth();
        return width > 0 && width < 760;
    }

    private String currentQuery() {
        return normalized(searchField == null ? "" : searchField.getText());
    }

    private boolean matchesField(EmployeeFieldDefinition definition, String query) {
        return matchesText(definition.label(), query)
                || matchesText(definition.columnName(), query)
                || matchesText(definition.heading(), query)
                || matchesText(currentFieldValue(definition), query);
    }

    private String currentFieldValue(EmployeeFieldDefinition definition) {
        String column = definition.columnName();
        JTextField textField = textFieldMap.get(column);
        if (textField != null) {
            return textField.getText();
        }

        UniversalTextArea textArea = textAreaMap.get(column);
        if (textArea != null) {
            return textArea.getText();
        }

        JComboBox<String> combo = dropdownFieldMap.get(column);
        if (combo != null) {
            return DropdownFieldSupport.value(combo);
        }

        UniversalDatePicker datePicker = dateFieldMap.get(column);
        if (datePicker != null && datePicker.getDate() != null) {
            return DB_DATE_FORMAT.format(datePicker.getDate());
        }

        return valueFor(column);
    }

    private void updateSearchStatus(int visibleFields, int totalFields, String query) {
        if (clearSearchButton != null) {
            EmployeeAdditionalDetailsPanelHelper.updateClearButtonState(clearSearchButton, !query.isBlank());
        }
        if (searchStatusLabel == null) {
            return;
        }

        if (query.isBlank()) {
            searchStatusLabel.setText(statusText(totalFields, missingFieldCount()));
        } else {
            searchStatusLabel.setText("Showing " + visibleFields + " of " + totalFields
                    + " fields. Missing fields use a red outline.");
        }
        updateMissingFieldsChipDisplay();
    }

    private String statusText(int totalFields, int missingFields) {
        return "Total fields: " + totalFields + ". Missing data fields: " + missingFields
                + ". Red outline marks missing fields.";
    }

    private String normalized(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^A-Za-z0-9]+", " ")
                        .trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);
    }

    private boolean matchesText(String value, String query) {
        String text = normalized(value);
        if (query.isBlank()) {
            return true;
        }
        if (text.contains(query)) {
            return true;
        }

        String[] tokens = query.split("\\s+");
        boolean hasToken = false;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            hasToken = true;
            if (!text.contains(token)) {
                return false;
            }
        }
        return hasToken;
    }

    public Employee getUpdatedOtherDetails() {
        Employee employee = new Employee();
        textFieldMap.forEach((column, field) -> {
            if (!isDirtyEditableColumn(column) || !field.isEditable()) {
                return;
            }

            String value = field.getText();
            if (isEmpty(value)) {
                return;
            }

            writeValue(employee, column, value.trim());
        });
        textAreaMap.forEach((column, area) -> {
            if (!isDirtyEditableColumn(column) || !area.isEditable()) {
                return;
            }

            String value = area.getText();
            if (isEmpty(value)) {
                return;
            }

            writeValue(employee, column, value.trim());
        });
        dropdownFieldMap.forEach((column, combo) -> {
            if (!isDirtyEditableColumn(column)) {
                return;
            }

            String value = DropdownFieldSupport.value(combo);
            if (isEmpty(value)) {
                return;
            }

            writeValue(employee, column, value.trim());
        });
        dateFieldMap.forEach((column, picker) -> {
            if (!isDirtyEditableColumn(column) || !Boolean.TRUE.equals(dateDirtyMap.get(column))) {
                return;
            }

            Date value = picker.getDate();
            if (value == null) {
                return;
            }

            writeValue(employee, column, DB_DATE_FORMAT.format(value));
        });
        return employee;
    }

    public void setPendingChangesListener(Runnable pendingChangesListener) {
        this.pendingChangesListener = pendingChangesListener;
    }

    public boolean hasPendingChanges() {
        for (Map.Entry<String, JTextField> entry : textFieldMap.entrySet()) {
            if (isDirtyEditableColumn(entry.getKey()) && !isEmpty(entry.getValue().getText())) {
                return true;
            }
        }
        for (Map.Entry<String, UniversalTextArea> entry : textAreaMap.entrySet()) {
            if (isDirtyEditableColumn(entry.getKey()) && !isEmpty(entry.getValue().getText())) {
                return true;
            }
        }
        for (Map.Entry<String, JComboBox<String>> entry : dropdownFieldMap.entrySet()) {
            if (isDirtyEditableColumn(entry.getKey()) && !isEmpty(DropdownFieldSupport.value(entry.getValue()))) {
                return true;
            }
        }
        for (Map.Entry<String, UniversalDatePicker> entry : dateFieldMap.entrySet()) {
            if (isDirtyEditableColumn(entry.getKey())
                    && Boolean.TRUE.equals(dateDirtyMap.get(entry.getKey()))
                    && entry.getValue().getDate() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isEditableColumn(String column) {
        return Boolean.TRUE.equals(editableColumns.get(column));
    }

    private boolean isDirtyEditableColumn(String column) {
        return isEditableColumn(column) && Boolean.TRUE.equals(dirtyColumns.get(column));
    }

    private void installDocumentListener(String column, JTextComponent textComponent) {
        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                markDirty(column);
            }

            public void removeUpdate(DocumentEvent event) {
                markDirty(column);
            }

            public void changedUpdate(DocumentEvent event) {
                markDirty(column);
            }
        });
    }

    private void markDirty(String column) {
        if (!loadingValues && isEditableColumn(column)) {
            dirtyColumns.put(column, true);
            updateFieldBorder(column);
        }
        notifyPendingChanges();
    }

    private void notifyPendingChanges() {
        if (loadingValues || pendingChangesListener == null) {
            return;
        }
        pendingChangesListener.run();
    }

    private void applyFieldEditability(JComponent input, boolean editable) {
        input.setCursor(Cursor.getPredefinedCursor(editable ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (input instanceof JTextField textField) {
            textField.setEditable(editable);
            styleFieldBorder(textField, false);
            return;
        }
        if (input instanceof UniversalTextArea area) {
            area.setEditable(editable);
            area.textArea().setCursor(Cursor.getPredefinedCursor(editable ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
            styleAreaBorder(area, false);
            return;
        }
        if (input instanceof JComboBox<?> combo) {
            combo.setEnabled(editable);
            combo.setFocusable(editable);
            combo.setRequestFocusEnabled(editable);
            styleFieldBorder(combo, false);
            installReadableDisabledRenderer(combo);
            return;
        }
        if (input instanceof UniversalDatePicker picker) {
            picker.setEnabled(editable);
            styleFieldBorder(picker, false);
        }
    }

    private void updateFieldBorder(String column) {
        boolean missing = isEditableColumn(column) && isEmpty(currentFieldValueByColumn(column));
        JComponent input = inputForColumn(column);
        if (input instanceof UniversalTextArea area) {
            styleAreaBorder(area, missing);
        } else if (input != null) {
            styleFieldBorder(input, missing);
        }
    }

    private JComponent inputForColumn(String column) {
        if (textFieldMap.containsKey(column)) {
            return textFieldMap.get(column);
        }
        if (textAreaMap.containsKey(column)) {
            return textAreaMap.get(column);
        }
        if (dropdownFieldMap.containsKey(column)) {
            return dropdownFieldMap.get(column);
        }
        return dateFieldMap.get(column);
    }

    private String currentFieldValueByColumn(String column) {
        JTextField textField = textFieldMap.get(column);
        if (textField != null) {
            return textField.getText();
        }
        UniversalTextArea textArea = textAreaMap.get(column);
        if (textArea != null) {
            return textArea.getText();
        }
        JComboBox<String> combo = dropdownFieldMap.get(column);
        if (combo != null) {
            return DropdownFieldSupport.value(combo);
        }
        UniversalDatePicker picker = dateFieldMap.get(column);
        if (picker != null && picker.getDate() != null) {
            return DB_DATE_FORMAT.format(picker.getDate());
        }
        return "";
    }

    private void styleFieldBorder(JComponent component, boolean missing) {
        component.setBorder(EmployeeRegistrationFormPanelHelper.inputBorder(missing ? MISSING_BORDER : FIELD_BORDER));
    }

    private void styleAreaBorder(UniversalTextArea area, boolean missing) {
        area.setBorder(EmployeeRegistrationFormPanelHelper.inputBorder(missing ? MISSING_BORDER : FIELD_BORDER));
    }

    private void installReadableDisabledRenderer(JComboBox<?> combo) {
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : String.valueOf(value));
            label.setOpaque(true);
            label.setFont(combo.getFont());
            label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            label.setBackground(Color.WHITE);
            label.setForeground(new Color(35, 43, 54));
            return label;
        });
    }

    private int missingFieldCount() {
        int count = 0;
        for (Boolean editable : editableColumns.values()) {
            if (Boolean.TRUE.equals(editable)) {
                count++;
            }
        }
        return count;
    }

    private String valueFor(String column) {
        if (data == null || column == null || column.isBlank()) {
            return "";
        }

        try {
            Field field = Employee.class.getDeclaredField(column);
            field.setAccessible(true);
            Object value = field.get(data);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException exception) {
            String value = data.getDynamicField(column);
            return value == null ? "" : value;
        }
    }

    private void writeValue(Employee employee, String column, String value) {
        try {
            Field field = Employee.class.getDeclaredField(column);
            field.setAccessible(true);
            field.set(employee, value);
        } catch (ReflectiveOperationException exception) {
            employee.setDynamicField(column, value);
        }
    }

    private String displayValue(String value) {
        return isEmpty(value) ? "N/A" : value;
    }

    private Date parseDate(String value) {
        if (isEmpty(value) || "Choose Date".equalsIgnoreCase(value.trim())) {
            return null;
        }

        return DateDisplayFormatter.parse(value);
    }

    private boolean isEmpty(String value) {
        if (value == null) {
            return true;
        }

        String text = value.trim();
        return text.isEmpty()
                || text.equalsIgnoreCase("N/A")
                || text.equalsIgnoreCase("NA")
                || text.equalsIgnoreCase("NULL")
                || text.equalsIgnoreCase("EMPTY")
                || text.equals("-");
    }

    private record SectionView(String heading, JPanel panel, JPanel grid, List<FieldView> fields) {
    }

    private record FieldView(EmployeeFieldDefinition definition, JPanel panel) {
    }

    private record FilterSummary(int visibleFields, int totalFields) {
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(130, 140, 150));
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, 0, y);
            g2.dispose();
        }
    }
}
