package com.kgm.ui;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.service.AuthService;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.ui.styling.TablePaginationHelper;
import com.kgm.util.EmployeeDocumentUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsView extends JFrame {
    private static final int DATE_COLUMN = 4;

    private final EmployeeFieldDefinitionDao dao = new EmployeeFieldDefinitionDao();
    private final Map<String, EmployeeFieldDefinition> definitionsByColumn = new LinkedHashMap<>();
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private boolean loadingFields;

    public SettingsView() {
        setTitle("Field Settings");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(new HeaderPanel("Field Settings"), BorderLayout.NORTH);
        add(top, BorderLayout.NORTH);

        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(Color.WHITE);
        page.setBorder(new EmptyBorder(26, 28, 24, 28));

        page.add(createTitleRow(), BorderLayout.NORTH);

        model = new DefaultTableModel(
                new String[]{"Column", "Label", "Heading", "Category", "Date", "Origin", "Locked"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == DATE_COLUMN ? Boolean.class : String.class;
            }
        };
        table = TablePaginationHelper.createDocumentTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        TablePaginationHelper.autoResizeColumns(table);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(Color.WHITE);
        center.add(createActionsRow(), BorderLayout.NORTH);
        center.add(TablePaginationHelper.createScrollPane(table, true), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);

        add(page, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);
        loadFields();
        setVisible(true);
    }

    private JPanel createTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(Color.WHITE);

        JLabel title = new JLabel("Database Field Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Manage custom detail and document fields for employee records");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(99, 115, 129));

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        JButton dashboard = new JButton("Dashboard");
        EmployeeRegistrationViewHelper.styleBackButton(dashboard);
        dashboard.addActionListener(event -> {
            dispose();
            new HomeView();
        });

        row.add(titleBlock, BorderLayout.WEST);
        row.add(dashboard, BorderLayout.EAST);
        return row;
    }

    private JPanel createActionsRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);

        JButton add = new JButton("Add Field");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton refresh = new JButton("Refresh");

        EmployeeRegistrationViewHelper.stylePrimaryButton(add);
        EmployeeRegistrationViewHelper.styleSecondaryButton(edit);
        EmployeeRegistrationViewHelper.styleSecondaryButton(delete);
        EmployeeRegistrationViewHelper.styleSecondaryButton(refresh);

        add.addActionListener(event -> addField());
        edit.addActionListener(event -> editField());
        delete.addActionListener(event -> deleteField());
        refresh.addActionListener(event -> loadFields());

        JPanel searchPanel = createSearchPanel();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(add);
        buttonPanel.add(edit);
        buttonPanel.add(delete);
        buttonPanel.add(refresh);

        row.add(searchPanel, BorderLayout.WEST);
        row.add(buttonPanel, BorderLayout.EAST);
        return row;
    }

    private JPanel createSearchPanel() {
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
        clear.setVisible(false);
        clear.addActionListener(event -> searchField.setText(""));

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
                clear.setVisible(!searchField.getText().isEmpty());
                applyFieldSearch();
            }

            public void removeUpdate(DocumentEvent event) {
                clear.setVisible(!searchField.getText().isEmpty());
                applyFieldSearch();
            }

            public void changedUpdate(DocumentEvent event) {
                clear.setVisible(!searchField.getText().isEmpty());
                applyFieldSearch();
            }
        });

        panel.add(searchBox);
        return panel;
    }

    private void applyFieldSearch() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    return cellContains(entry, 0, query) || cellContains(entry, 1, query);
                }
            });
        }
        TablePaginationHelper.autoResizeColumns(table);
    }

    private boolean cellContains(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry,
                                 int column,
                                 String query) {
        Object value = entry.getValue(column);
        return value != null && value.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void loadFields() {
        try {
            loadingFields = true;
            definitionsByColumn.clear();
            model.setRowCount(0);
            for (EmployeeFieldDefinition definition : dao.listFields()) {
                definitionsByColumn.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
                model.addRow(new Object[]{
                        definition.columnName(),
                        definition.label(),
                        definition.heading(),
                        definition.usageLabel(),
                        definition.dateField(),
                        definition.sourceLabel(),
                        definition.protectedField() ? "Yes" : "No"
                });
            }
            applyFieldSearch();
            TablePaginationHelper.autoResizeColumns(table);
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            DialogHelper.error(this, "Settings Load Failed", exception.getMessage());
        } finally {
            loadingFields = false;
        }
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
                    data.dateField()
            );
            EmployeeDocumentUtil.refreshDocumentTypes();
            loadFields();
            DialogHelper.success(this, "Field added.\nColumn: " + added.columnName());
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Add Field Failed", rootMessage(exception));
        }
    }

    private void editField() {
        EmployeeFieldDefinition selected = selectedDefinition();
        if (selected == null) {
            DialogHelper.warning(this, "Select Field", "Select a field to edit.");
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
                    data.dateField()
            );
            EmployeeDocumentUtil.refreshDocumentTypes();
            loadFields();
            DialogHelper.success(this, "Field updated.\nColumn: " + updated.columnName());
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Edit Field Failed", rootMessage(exception));
        }
    }

    private void deleteField() {
        EmployeeFieldDefinition selected = selectedDefinition();
        if (selected == null) {
            DialogHelper.warning(this, "Select Field", "Select a custom field to delete.");
            return;
        }
        if (!selected.customField()) {
            DialogHelper.warning(this, "Built-in Field", "Only custom fields can be deleted.");
            return;
        }
        if (!confirmPassword()) {
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
            loadFields();
            DialogHelper.success(this, "Field deleted.");
        } catch (RuntimeException exception) {
            DialogHelper.error(this, "Delete Field Failed", rootMessage(exception));
        }
    }

    private EmployeeFieldDefinition selectedDefinition() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Object column = model.getValueAt(modelRow, 0);
        if (column == null) {
            return null;
        }
        return definitionsByColumn.get(column.toString().toUpperCase(Locale.ROOT));
    }

    private FieldFormData showFieldDialog(String title, EmployeeFieldDefinition current) {
        JTextField column = new JTextField(current == null ? "" : current.columnName(), 24);
        column.setEditable(false);
        JTextField label = new JTextField(current == null ? "" : current.label(), 24);
        List<String> headings = dao.listDetailHeadings();
        JComboBox<String> heading = new JComboBox<>(headings.toArray(new String[0]));
        heading.setEditable(true);
        if (current != null && !current.documentField()) {
            heading.setSelectedItem(current.heading());
        } else if (current != null && current.documentField()) {
            heading.setSelectedItem("Documents");
        }

        JComboBox<String> category = new JComboBox<>(new String[]{"Details", "Documents"});
        category.setSelectedItem(current != null && current.documentField() ? "Documents" : "Details");
        category.setEnabled(current == null);
        JCheckBox dateField = new JCheckBox("Its Date Field. Use Calendar");
        dateField.setSelected(current != null && current.dateField());
        dateField.setOpaque(false);
        heading.setEnabled(!"Documents".equals(category.getSelectedItem()));
        dateField.setEnabled(!"Documents".equals(category.getSelectedItem()));
        category.addActionListener(event -> {
            boolean documents = "Documents".equals(category.getSelectedItem());
            heading.setEnabled(!documents);
            dateField.setEnabled(!documents);
            if (documents) {
                heading.setSelectedItem("Documents");
                dateField.setSelected(false);
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

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
        form.add(new JLabel("Category"), gbc);
        gbc.gridx = 1;
        form.add(category, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        form.add(new JLabel("Heading"), gbc);
        gbc.gridx = 1;
        form.add(heading, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Date"), gbc);
        gbc.gridx = 1;
        form.add(dateField, gbc);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    form,
                    title,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String labelText = label.getText().trim();
            boolean documentField = "Documents".equals(category.getSelectedItem());
            Object headingValue = heading.getEditor().getItem();
            String headingText = documentField ? "Documents" : headingValue == null ? "" : headingValue.toString().trim();
            boolean useDatePicker = !documentField && dateField.isSelected();
            if (labelText.isEmpty()) {
                DialogHelper.warning(this, "Label Required", "Enter a field label.");
                continue;
            }
            if (!documentField && headingText.isEmpty()) {
                DialogHelper.warning(this, "Heading Required", "Choose or type a heading for this field.");
                continue;
            }
            return new FieldFormData(labelText, headingText, documentField, useDatePicker);
        }
    }

    private boolean confirmPassword() {
        JPasswordField password = new JPasswordField(18);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Enter admin password to delete this field."), BorderLayout.NORTH);
        panel.add(password, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Admin Password",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        boolean valid = AuthService.login("admin", new String(password.getPassword()));
        if (!valid) {
            DialogHelper.error(this, "Password Incorrect", "The admin password is incorrect.");
        }
        return valid;
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message == null || message.isBlank() ? throwable.getMessage() : message;
    }

    private record FieldFormData(String label, String heading, boolean documentField, boolean dateField) {
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
