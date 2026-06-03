package com.kgm.ui.panel;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.component.UniversalTextArea;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeBasicDetailsPanelHelper;
import com.kgm.ui.styling.EmployeeRegistrationFormPanelHelper;
import com.kgm.util.CnicFormatter;
import com.kgm.util.DateDisplayFormatter;
import com.kgm.util.EmployeeBasicFieldUtil;
import com.kgm.util.EmployeeDocumentUtil;
import com.kgm.util.PhoneFormatter;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class EmployeeBasicDetailsPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final int DROPDOWN_SEARCH_DEBOUNCE_MS = 350;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color MISSING_BORDER = new Color(220, 38, 38);

    private final List<EmployeeFieldDefinition> definitions;
    private final Map<String, JComponent> inputsByColumn = new LinkedHashMap<>();
    private final Map<String, Boolean> editableColumns = new LinkedHashMap<>();
    private final Map<String, Boolean> dirtyColumns = new LinkedHashMap<>();

    private JLabel photoPreview;
    private FileUploadCard photoUploadCard;
    private JLabel infoLabel;
    private File selectedImage;
    private Employee employee;
    private Runnable pendingChangesListener;
    private Consumer<File> selectedImageListener;
    private boolean loadingValues;

    public EmployeeBasicDetailsPanel() {
        this(null, EmployeeBasicFieldUtil.loadBasicDefinitions());
    }

    public EmployeeBasicDetailsPanel(Employee employee) {
        this(employee, EmployeeBasicFieldUtil.loadBasicDefinitions());
    }

    public EmployeeBasicDetailsPanel(Employee employee, List<EmployeeFieldDefinition> definitions) {
        this.definitions = definitions == null || definitions.isEmpty()
                ? EmployeeBasicFieldUtil.loadBasicDefinitions()
                : List.copyOf(definitions);
        this.employee = employee;
        EmployeeBasicDetailsPanelHelper.stylePanel(this);
        add(EmployeeBasicDetailsPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
        if (employee != null) {
            loadEmployeeData();
        }
    }

    private JPanel buildForm() {
        JPanel root = EmployeeBasicDetailsPanelHelper.createFormRoot();
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
        JPanel left = EmployeeBasicDetailsPanelHelper.createPhotoPanel();
        photoPreview = EmployeeBasicDetailsPanelHelper.createPhotoPreview("Photo");
        photoPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (selectedImageCanChange()) {
                    chooseImage(photoPreview);
                }
            }
        });

        photoUploadCard = new FileUploadCard("Employee Photo", "JPEG only - Max " + EmployeeDocumentUtil.maxUploadSizeLabel(), "Choose");
        photoUploadCard.addActionListener(event -> {
            if (selectedImageCanChange()) {
                chooseImage(photoPreview);
            }
        });

        JPanel bottom = EmployeeBasicDetailsPanelHelper.createPhotoInfoPanel();
        infoLabel = EmployeeBasicDetailsPanelHelper.createPhotoInfoLabel("");
        infoLabel.setVisible(false);
        photoUploadCard.setVisible(false);
        bottom.add(createEmployeeIdentityBlock());

        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);
        return left;
    }

    private JPanel createEmployeeIdentityBlock() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));

        JLabel name = new JLabel(displayOrFallback(employee == null ? null : employee.getEMP_NAME(), "Employee Name"));
        name.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        name.setForeground(new Color(20, 101, 192));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel code = identityLine("Code", employee == null ? null : employee.getEMPLOYEE_CODE());
        JLabel designation = identityLine("Designation", employee == null ? null : employee.getDESIGNATION());

        panel.add(name);
        panel.add(Box.createVerticalStrut(3));
        panel.add(code);
        panel.add(Box.createVerticalStrut(2));
        panel.add(designation);
        return panel;
    }

    private JLabel identityLine(String label, String value) {
        JLabel line = new JLabel(label + ": " + displayOrFallback(value, "N/A"));
        line.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        line.setForeground(new Color(99, 115, 129));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private String displayOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private JPanel buildRightForm() {
        JPanel panel = EmployeeBasicDetailsPanelHelper.createRightFormPanel();
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
        panel.add(new FormField(first.label(), inputFor(first)), gbc);

        gbc.gridx = 1;
        if (second == null) {
            panel.add(EmployeeBasicDetailsPanelHelper.createGridFiller(), gbc);
        } else {
            panel.add(new FormField(second.label(), inputFor(second)), gbc);
        }
    }

    private void addSingleField(JPanel panel, GridBagConstraints gbc, int y, EmployeeFieldDefinition definition) {
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new FormField(definition.label(), inputFor(definition)), gbc);
        gbc.gridx = 1;
        panel.add(EmployeeBasicDetailsPanelHelper.createGridFiller(), gbc);
    }

    private void addFullWidthField(JPanel panel, GridBagConstraints gbc, int y, EmployeeFieldDefinition definition) {
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new FormField(definition.label(), inputFor(definition)), gbc);
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
            DropdownFieldSupport.setPlaceholder(
                    combo,
                    EmployeeBasicFieldUtil.dropdownPlaceholder(definition.variableOptionField())
            );
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
        }

        if ("EMPLOYEE_CODE".equalsIgnoreCase(column)) {
            input.setEnabled(false);
            if (input instanceof JTextField textField) {
                textField.setEditable(false);
            }
        }
        EmployeeBasicDetailsPanelHelper.styleInput(input);
        inputsByColumn.put(column, input);
        return input;
    }

    private void installDynamicDropdownSearch(EmployeeFieldDefinition definition, JComboBox<String> combo) {
        if (!definition.variableOptionField() || "SECTION".equalsIgnoreCase(definition.columnName())) {
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

    private void loadEmployeeData() {
        if (employee == null) {
            return;
        }

        loadingValues = true;
        for (EmployeeFieldDefinition definition : definitions) {
            setValue(definition.columnName(), EmployeeBasicFieldUtil.valueFor(employee, definition.columnName()));
        }
        loadProfileImage();
        applyMissingOnlyEditability();
        installPendingChangeListeners();
        loadingValues = false;
    }

    private void setValue(String column, String value) {
        JComponent input = inputsByColumn.get(column);
        if (input instanceof UniversalDatePicker picker) {
            Date parsed = parseDate(value);
            if (parsed != null) {
                picker.setDate(parsed);
            }
        } else if (input instanceof JComboBox<?> combo) {
            if (value != null && !value.trim().isEmpty()) {
                setComboValue(combo, value);
            } else if (combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            }
        } else if (input instanceof UniversalTextArea area) {
            area.setText(displayValue(value));
        } else if (input instanceof JScrollPane scrollPane
                && scrollPane.getViewport().getView() instanceof JTextArea area) {
            area.setText(displayValue(value));
        } else if (input instanceof JTextField textField) {
            textField.setText(displayValue(value));
        }
    }

    private Date parseDate(String value) {
        if (isEmpty(value)) {
            return null;
        }

        return DateDisplayFormatter.parse(value);
    }

    private void loadProfileImage() {
        if (employee == null || isEmpty(employee.getEMP_IMG())) {
            return;
        }

        try {
            File imgFile = resolveEmployeeImageFile(employee.getEMP_IMG());
            if (!imgFile.exists()) {
                lockProfileImageUpload();
                return;
            }

            BufferedImage img = EmployeeDocumentUtil.readJpegImage(imgFile);
            if (img == null) {
                lockProfileImageUpload();
                return;
            }

            setPhotoPreviewImage(img);
            lockProfileImageUpload();
        } catch (Exception ex) {
            lockProfileImageUpload();
            System.out.println("Image load error: " + ex.getMessage());
        }
    }

    private void lockProfileImageUpload() {
        photoPreview.setCursor(Cursor.getDefaultCursor());
        if (photoUploadCard != null) {
            photoUploadCard.setVisible(false);
        }
        if (infoLabel != null) {
            infoLabel.setVisible(false);
        }
    }

    private boolean selectedImageCanChange() {
        return employee == null || isEmpty(employee.getEMP_IMG());
    }

    private File resolveEmployeeImageFile(String imagePath) {
        return EmployeeDocumentUtil.resolveStoredFile(imagePath);
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

        prepareSelectedImage(file, true);
    }

    public void setSelectedImageFromDocumentUpload(File file) {
        prepareSelectedImage(file, false);
    }

    public void setSelectedImageListener(Consumer<File> selectedImageListener) {
        this.selectedImageListener = selectedImageListener;
    }

    private void prepareSelectedImage(File file, boolean notifyListener) {
        if (file == null || !selectedImageCanChange()) {
            return;
        }

        if (!EmployeeDocumentUtil.shouldCompressBeforeUpload(file)) {
            applySelectedImage(EmployeeDocumentUtil.prepareImageForUpload(file), notifyListener);
            return;
        }
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Preparing Photo",
                "Compressing JPG/JPEG photo to fit the upload limit..."
        );
        SwingWorker<EmployeeDocumentUtil.PreparedUploadFile, Void> worker = new SwingWorker<>() {
            @Override
            protected EmployeeDocumentUtil.PreparedUploadFile doInBackground() {
                return EmployeeDocumentUtil.prepareImageForUpload(file);
            }

            @Override
            protected void done() {
                loader.close();
                try {
                    applySelectedImage(get(), notifyListener);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.warning(EmployeeBasicDetailsPanel.this, "Upload Stopped", "Photo preparation was interrupted.");
                } catch (ExecutionException exception) {
                    DialogHelper.warning(EmployeeBasicDetailsPanel.this, "Invalid Image", "The selected photo could not be prepared.");
                }
            }
        };
        worker.execute();
    }

    private void applySelectedImage(EmployeeDocumentUtil.PreparedUploadFile prepared, boolean notifyListener) {
        if (!prepared.ready()) {
            DialogHelper.warning(this, "Invalid Image", prepared.message());
            return;
        }
        try {
            File file = prepared.file();
            BufferedImage img = EmployeeDocumentUtil.readJpegImage(file);
            if (img == null) {
                DialogHelper.warning(this, "Invalid Image", "Please select a valid JPEG image.");
                return;
            }
            selectedImage = file;
            setPhotoPreviewImage(img);
            if (photoUploadCard != null) {
                photoUploadCard.setStatus(prepared.originalFile().getName());
            }
            if (notifyListener && selectedImageListener != null) {
                selectedImageListener.accept(file);
            }
            notifyPendingChanges();
            revalidate();
            repaint();
        } catch (Exception e) {
            DialogHelper.warning(this, "Invalid Image", "Please select a valid JPEG image.");
        }
    }

    private void setPhotoPreviewImage(BufferedImage image) {
        if (photoPreview instanceof EmployeeRegistrationFormPanelHelper.PhotoPreviewLabel preview) {
            preview.setPreviewImage(image);
            return;
        }
        photoPreview.setIcon(new ImageIcon(image));
        photoPreview.setText("");
    }

    public Employee getEmployeeFromForm() {
        Employee updated = new Employee();
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            if ("EMPLOYEE_CODE".equalsIgnoreCase(column) || !isDirtyEditableColumn(column)) {
                continue;
            }
            String value = valueFor(column);
            if (isEmpty(value)) {
                continue;
            }
            if ("NID".equalsIgnoreCase(column)) {
                value = CnicFormatter.format(value);
            }
            if ("EMP_CONTNO".equalsIgnoreCase(column)) {
                value = PhoneFormatter.format(value);
            }
            if ("GRADE".equalsIgnoreCase(column)) {
                value = value.toUpperCase(Locale.ROOT);
            }
            EmployeeBasicFieldUtil.writeValue(updated, column, value);
        }
        return updated;
    }

    public String validationMessage() {
        String phone = isEditableColumn("EMP_CONTNO") ? valueFor("EMP_CONTNO") : "";
        if (!phone.isBlank() && !PhoneFormatter.isValid(phone)) {
            return "Phone must use format " + PhoneFormatter.FORMAT_EXAMPLE + ".";
        }

        String email = isEditableColumn("PERSONAL_EMAIL") ? valueFor("PERSONAL_EMAIL") : "";
        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            return "Enter a valid email address.";
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
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty()
                || trimmed.equalsIgnoreCase("N/A")
                || trimmed.equalsIgnoreCase("NA")
                || trimmed.equalsIgnoreCase("NULL")
                || trimmed.equalsIgnoreCase("EMPTY")
                || trimmed.equals("-");
    }

    private String displayValue(String value) {
        return isEmpty(value) ? "" : value;
    }

    public File getSelectedImage() {
        return selectedImage;
    }

    public void setPendingChangesListener(Runnable pendingChangesListener) {
        this.pendingChangesListener = pendingChangesListener;
    }

    public boolean hasPendingChanges() {
        if (selectedImage != null) {
            return true;
        }
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            if (!isDirtyEditableColumn(column)) {
                continue;
            }
            if (!isEmpty(valueFor(column))) {
                return true;
            }
        }
        return false;
    }

    private void applyMissingOnlyEditability() {
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            boolean editable = !"EMPLOYEE_CODE".equalsIgnoreCase(column)
                    && isEmpty(EmployeeBasicFieldUtil.valueFor(employee, column));
            editableColumns.put(column, editable);
            dirtyColumns.put(column, false);
            applyFieldEditability(inputsByColumn.get(column), editable);
            updateFieldBorder(column);
        }

        if (!selectedImageCanChange()) {
            lockProfileImageUpload();
        }
    }

    private boolean isEditableColumn(String column) {
        return Boolean.TRUE.equals(editableColumns.get(column));
    }

    private boolean isDirtyEditableColumn(String column) {
        return isEditableColumn(column) && Boolean.TRUE.equals(dirtyColumns.get(column));
    }

    private void installPendingChangeListeners() {
        for (Map.Entry<String, JComponent> entry : inputsByColumn.entrySet()) {
            installPendingChangeListener(entry.getKey(), entry.getValue());
        }
    }

    private void installPendingChangeListener(String column, JComponent input) {
        if (input instanceof UniversalDatePicker picker) {
            picker.addDateChangeListener(() -> markDirty(column));
            return;
        }
        if (input instanceof JComboBox<?> combo) {
            combo.addItemListener(event -> markDirty(column));
            Component editor = combo.getEditor() == null ? null : combo.getEditor().getEditorComponent();
            if (editor instanceof JTextField textField) {
                installDocumentListener(column, textField);
            }
            return;
        }
        if (input instanceof UniversalTextArea area) {
            installDocumentListener(column, area.textArea());
            return;
        }
        if (input instanceof JScrollPane scrollPane
                && scrollPane.getViewport().getView() instanceof JTextArea area) {
            installDocumentListener(column, area);
            return;
        }
        if (input instanceof JTextField textField) {
            installDocumentListener(column, textField);
        }
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
        if (input == null) {
            return;
        }
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
        JComponent input = inputsByColumn.get(column);
        boolean missing = isEditableColumn(column) && isEmpty(valueFor(column));
        if (input instanceof UniversalTextArea area) {
            styleAreaBorder(area, missing);
        } else if (input != null) {
            styleFieldBorder(input, missing);
        }
    }

    private void styleFieldBorder(JComponent component, boolean missing) {
        component.setBorder(EmployeeRegistrationFormPanelHelper.inputBorder(missing ? MISSING_BORDER : FIELD_BORDER));
    }

    private void styleAreaBorder(UniversalTextArea area, boolean missing) {
        area.setBorder(EmployeeRegistrationFormPanelHelper.inputBorder(missing ? MISSING_BORDER : FIELD_BORDER));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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

    @SuppressWarnings("unchecked")
    private void setComboValue(JComboBox<?> combo, String value) {
        DropdownFieldSupport.setValue((JComboBox<String>) combo, value);
    }

    private class FormField extends JPanel {
        FormField(String text, JComponent input) {
            EmployeeBasicDetailsPanelHelper.styleFormField(this);
            add(EmployeeBasicDetailsPanelHelper.createFieldLabel(text), BorderLayout.NORTH);
            add(input, BorderLayout.CENTER);
        }
    }
}
