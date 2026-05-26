package com.kgm.ui.panel;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.component.UniversalTextArea;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationFormPanelHelper;
import com.kgm.util.CnicFormatter;
import com.kgm.util.EmployeeBasicFieldUtil;
import com.kgm.util.EmployeeDocumentUtil;
import com.kgm.util.PhoneFormatter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class EmployeeRegistrationFormPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final int DROPDOWN_SEARCH_DEBOUNCE_MS = 350;

    private List<EmployeeFieldDefinition> definitions;
    private final Map<String, JComponent> inputsByColumn = new LinkedHashMap<>();
    private boolean profileImageRequired;

    private JLabel photoPreview;
    private FileUploadCard photoUploadCard;
    private File selectedImage;

    public EmployeeRegistrationFormPanel() {
        this(EmployeeBasicFieldUtil.loadBasicDefinitions(), EmployeeDocumentUtil.isProfileImageRequired());
    }

    public EmployeeRegistrationFormPanel(
            List<EmployeeFieldDefinition> definitions,
            boolean profileImageRequired
    ) {
        this.definitions = definitions == null || definitions.isEmpty()
                ? EmployeeBasicFieldUtil.fallbackDefinitions()
                : List.copyOf(definitions);
        this.profileImageRequired = profileImageRequired;
        EmployeeRegistrationFormPanelHelper.stylePanel(this);
        add(EmployeeRegistrationFormPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
    }

    public void reloadFields() {
        definitions = EmployeeBasicFieldUtil.loadBasicDefinitions();
        profileImageRequired = EmployeeDocumentUtil.isProfileImageRequired();
        inputsByColumn.clear();
        selectedImage = null;
        removeAll();
        add(EmployeeRegistrationFormPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    private JPanel buildForm() {
        JPanel root = EmployeeRegistrationFormPanelHelper.createFormRoot();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        root.add(buildLeftPanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(buildRightForm(), gbc);

        return root;
    }

    private JPanel buildLeftPanel() {
        JPanel left = EmployeeRegistrationFormPanelHelper.createPhotoPanel();

        photoPreview = EmployeeRegistrationFormPanelHelper.createPhotoPreview(photoPreviewText());
        photoPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });

        photoUploadCard = new FileUploadCard(requiredLabel("Employee Photo"), "JPEG only - Max 400KB", "Choose");
        photoUploadCard.addActionListener(event -> chooseImage(photoPreview));

        JPanel bottom = EmployeeRegistrationFormPanelHelper.createPhotoInfoPanel();
        bottom.add(photoUploadCard);

        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);
        return left;
    }

    private JPanel buildRightForm() {
        JPanel panel = EmployeeRegistrationFormPanelHelper.createRightFormPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        for (int index = 0; index < definitions.size(); index += 2) {
            EmployeeFieldDefinition first = definitions.get(index);
            EmployeeFieldDefinition second = index + 1 < definitions.size() ? definitions.get(index + 1) : null;
            if (EmployeeBasicFieldUtil.isMultilineField(first)
                    && (second == null || !EmployeeBasicFieldUtil.isMultilineField(second))) {
                addFullWidthField(panel, gbc, row++, first);
                if (second != null) {
                    addSingleField(panel, gbc, row++, second);
                }
                continue;
            }
            if (second != null
                    && EmployeeBasicFieldUtil.isMultilineField(second)
                    && !EmployeeBasicFieldUtil.isMultilineField(first)) {
                addSingleField(panel, gbc, row++, first);
                addFullWidthField(panel, gbc, row++, second);
                continue;
            }
            addRow(panel, gbc, row++, first, second);
        }
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y,
                        EmployeeFieldDefinition first,
                        EmployeeFieldDefinition second) {
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new FormField(first, inputFor(first)), gbc);

        gbc.gridx = 1;
        if (second == null) {
            panel.add(Box.createHorizontalStrut(1), gbc);
        } else {
            panel.add(new FormField(second, inputFor(second)), gbc);
        }
    }

    private void addSingleField(JPanel panel, GridBagConstraints gbc, int y, EmployeeFieldDefinition definition) {
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new FormField(definition, inputFor(definition)), gbc);
        gbc.gridx = 1;
        panel.add(Box.createHorizontalStrut(1), gbc);
    }

    private void addFullWidthField(JPanel panel, GridBagConstraints gbc, int y, EmployeeFieldDefinition definition) {
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new FormField(definition, inputFor(definition)), gbc);
        gbc.gridwidth = 1;
    }

    private JComponent inputFor(EmployeeFieldDefinition definition) {
        String column = definition.columnName();
        JComponent input;
        if (EmployeeBasicFieldUtil.isDateField(definition)) {
            input = new UniversalDatePicker();
        } else if (EmployeeBasicFieldUtil.isDropdownField(definition)) {
            JComboBox<String> combo = new JComboBox<>(EmployeeBasicFieldUtil.dropdownOptions(definition, true));
            DropdownFieldSupport.configure(combo, definition.variableOptionField());
            // Set placeholder text for value() to treat as empty
            String placeholder = EmployeeBasicFieldUtil.dropdownPlaceholder(definition.variableOptionField());
            DropdownFieldSupport.setPlaceholder(combo, placeholder);
            installDynamicDropdownSearch(definition, combo);
            input = combo;
        } else if (EmployeeBasicFieldUtil.isMultilineField(definition)) {
            input = new UniversalTextArea();
        } else {
            input = new JTextField();
            if ("NID".equalsIgnoreCase(column)) {
                CnicFormatter.installFormatter((JTextField) input);
            }
            if ("EMP_CONTNO".equalsIgnoreCase(column)) {
                PhoneFormatter.installFormatter((JTextField) input);
            }
            if ("EMPLOYEE_CODE".equalsIgnoreCase(column)) {
                installDigitsOnlyFilter((JTextField) input);
                input.setToolTipText("Employee ID must contain numbers only.");
            }
        }

        EmployeeRegistrationFormPanelHelper.styleInput(input);
        applyDefaultValue(column, input);
        inputsByColumn.put(column, input);
        return input;
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

    private void applyDefaultValue(String column, JComponent input) {
        String value = defaultValue(column);
        if (value.isBlank()) {
            return;
        }
        if (input instanceof UniversalTextArea area) {
            area.setText(value);
        } else if (input instanceof JTextField textField) {
            textField.setText(value);
        }
    }

    private String defaultValue(String column) {
        if ("DESCR".equalsIgnoreCase(column)) {
            return "KGM";
        }
        return "";
    }

    private void chooseImage(JLabel target) {
        File file = FileUploadCard.chooseFile(
                this,
                "Upload Employee Photo",
                FileUploadCard.jpegImages()
        );
        if (file == null) {
            return;
        }

        String validationMessage = EmployeeDocumentUtil.validateImageFile(file);
        if (validationMessage != null) {
            DialogHelper.warning(this, "Invalid Image", validationMessage);
            return;
        }

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                DialogHelper.warning(this, "Invalid Image", "Please select a valid JPEG image.");
                return;
            }

            selectedImage = file;
            int padding = 4;
            Image scaled = img.getScaledInstance(
                    EmployeeRegistrationFormPanelHelper.PHOTO_SIZE - padding,
                    EmployeeRegistrationFormPanelHelper.PHOTO_SIZE - padding,
                    Image.SCALE_SMOOTH
            );
            target.setIcon(new ImageIcon(scaled));
            target.setText("");
            if (photoUploadCard != null) {
                photoUploadCard.setStatus(file.getName());
            }
        } catch (Exception e) {
            DialogHelper.warning(this, "Invalid Image", "Please select a valid JPEG image.");
        }
    }

    public Employee getEmployeeFromForm() {
        Employee employee = new Employee();
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            String value = valueFor(column);
            if ("NID".equalsIgnoreCase(column)) {
                value = CnicFormatter.format(value);
            }
            if ("GRADE".equalsIgnoreCase(column)) {
                value = value.toUpperCase(Locale.ROOT);
            }
            EmployeeBasicFieldUtil.writeValue(employee, column, value);
        }
        return employee;
    }

    public String validationMessage() {
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            if (isRequiredForRegistration(definition) && isEmpty(valueFor(column))) {
                return definition.label() + " is required.";
            }
        }

        String employeeId = valueFor("EMPLOYEE_CODE");
        if (!employeeId.isBlank() && !employeeId.matches("\\d+")) {
            return "Employee ID must contain numbers only.";
        }

        String email = valueFor("PERSONAL_EMAIL");
        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            return "Enter a valid email address.";
        }

        String cnic = valueFor("NID");
        if (!CnicFormatter.isValid(cnic)) {
            return "CNIC must use format " + CnicFormatter.FORMAT_EXAMPLE + ".";
        }

        Date joining = dateFor("JOINING_DATE");
        Date resignation = dateFor("RESIGN_DATE");
        if (joining != null && resignation != null && !joining.before(resignation)) {
            return "Date of Joining must be before Date of Resignation.";
        }
        return null;
    }

    private String valueFor(String column) {
        JComponent input = inputsByColumn.get(column);
        if (input instanceof UniversalDatePicker picker) {
            Date date = picker.getDate();
            return date == null ? "" : DB_DATE_FORMAT.format(date);
        }
        if (input instanceof JComboBox<?> combo) {
            return DropdownFieldSupport.value(combo);
        }
        if (input instanceof UniversalTextArea area) {
            return area.getText().trim();
        }
        if (input instanceof JScrollPane scrollPane
                && scrollPane.getViewport().getView() instanceof JTextArea area) {
            return area.getText().trim();
        }
        if (input instanceof JTextField textField) {
            return textField.getText().trim();
        }
        return "";
    }

    private Date dateFor(String column) {
        JComponent input = inputsByColumn.get(column);
        return input instanceof UniversalDatePicker picker ? picker.getDate() : null;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isRequiredForRegistration(EmployeeFieldDefinition definition) {
        return definition != null
                && definition.requiredField()
                && EmployeeBasicFieldUtil.isFundamentalsHeading(definition.heading());
    }

    public File getSelectedImage() {
        return selectedImage;
    }

    public boolean isRequiredProfileImageMissing() {
        return profileImageRequired && selectedImage == null;
    }

    public void clearForm() {
        for (JComponent input : inputsByColumn.values()) {
            if (input instanceof UniversalDatePicker picker) {
                picker.setDate(null);
            } else if (input instanceof JComboBox<?> combo) {
                if (combo.getItemCount() > 0) {
                    combo.setSelectedIndex(0);
                }
                if (combo.isEditable()) {
                    combo.getEditor().setItem(combo.getSelectedItem());
                }
            } else if (input instanceof JScrollPane scrollPane
                    && scrollPane.getViewport().getView() instanceof JTextArea area) {
                area.setText("");
            } else if (input instanceof UniversalTextArea area) {
                area.setText("");
            } else if (input instanceof JTextField textField) {
                textField.setText("");
            }
        }
        applyDefaultValues();
        selectedImage = null;
        if (photoUploadCard != null) {
            photoUploadCard.setStatus("");
        }
        photoPreview.setIcon(null);
        photoPreview.setText(photoPreviewText());
        requestInitialFocus();
        revalidate();
        repaint();
    }

    private void applyDefaultValues() {
        for (Map.Entry<String, JComponent> entry : inputsByColumn.entrySet()) {
            applyDefaultValue(entry.getKey(), entry.getValue());
        }
    }

    private void requestInitialFocus() {
        JComponent employeeId = inputsByColumn.get("EMPLOYEE_CODE");
        if (employeeId != null) {
            employeeId.requestFocusInWindow();
        }
    }

    private String photoPreviewText() {
        return profileImageRequired ? requiredLabel("Photo") : "Photo";
    }

    private String requiredLabel(String text) {
        return profileImageRequired
                ? "<html>" + text + " <font color='#d93025'>*</font></html>"
                : text;
    }

    private void installDigitsOnlyFilter(JTextField field) {
        if (field == null || !(field.getDocument() instanceof AbstractDocument document)) {
            return;
        }
        document.setDocumentFilter(new DigitsOnlyDocumentFilter());
    }

    private static class DigitsOnlyDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String digits = text == null ? "" : text.replaceAll("\\D", "");
            fb.replace(offset, length, digits, attrs);
        }
    }

    private String labelText(EmployeeFieldDefinition definition) {
        String label = definition == null ? "" : definition.label();
        if (!isRequiredForRegistration(definition)) {
            return label;
        }
        return "<html>" + htmlEscape(label) + " <font color='#d93025'>*</font></html>";
    }

    private String htmlEscape(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
    }

    private class FormField extends JPanel {
        FormField(EmployeeFieldDefinition definition, JComponent input) {
            EmployeeRegistrationFormPanelHelper.styleFormField(this);
            add(EmployeeRegistrationFormPanelHelper.createFieldLabel(labelText(definition)), BorderLayout.NORTH);
            add(input, BorderLayout.CENTER);
        }
    }
}
