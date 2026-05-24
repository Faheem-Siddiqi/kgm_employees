package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.component.UniversalTextArea;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeBasicDetailsPanelHelper;
import com.kgm.util.CnicFormatter;
import com.kgm.util.DateDisplayFormatter;
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
import java.util.Map;

public class EmployeeBasicDetailsPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private final List<EmployeeFieldDefinition> definitions = EmployeeBasicFieldUtil.loadBasicDefinitions();
    private final Map<String, JComponent> inputsByColumn = new LinkedHashMap<>();

    private JLabel photoPreview;
    private FileUploadCard photoUploadCard;
    private JLabel infoLabel;
    private File selectedImage;
    private Employee employee;

    public EmployeeBasicDetailsPanel() {
        EmployeeBasicDetailsPanelHelper.stylePanel(this);
        add(EmployeeBasicDetailsPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
    }

    public EmployeeBasicDetailsPanel(Employee employee) {
        this();
        this.employee = employee;
        loadEmployeeData();
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

        photoUploadCard = new FileUploadCard("Employee Photo", "JPEG only - Max 400KB", "Choose");
        photoUploadCard.addActionListener(event -> {
            if (selectedImageCanChange()) {
                chooseImage(photoPreview);
            }
        });

        JPanel bottom = EmployeeBasicDetailsPanelHelper.createPhotoInfoPanel();
        infoLabel = EmployeeBasicDetailsPanelHelper.createPhotoInfoLabel("");
        infoLabel.setVisible(false);
        bottom.add(photoUploadCard);

        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);
        return left;
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
            panel.add(Box.createHorizontalStrut(1), gbc);
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
        panel.add(Box.createHorizontalStrut(1), gbc);
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

    private void loadEmployeeData() {
        if (employee == null) {
            return;
        }

        for (EmployeeFieldDefinition definition : definitions) {
            setValue(definition.columnName(), EmployeeBasicFieldUtil.valueFor(employee, definition.columnName()));
        }
        loadProfileImage();
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

            BufferedImage img = ImageIO.read(imgFile);
            if (img == null) {
                lockProfileImageUpload();
                return;
            }

            Image scaled = img.getScaledInstance(
                    EmployeeBasicDetailsPanelHelper.PHOTO_SIZE,
                    EmployeeBasicDetailsPanelHelper.PHOTO_SIZE,
                    Image.SCALE_SMOOTH);
            photoPreview.setIcon(new ImageIcon(scaled));
            photoPreview.setText("");
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
        File imageFile = new File(imagePath);
        if (imageFile.isAbsolute()) {
            return imageFile;
        }
        return new File(System.getProperty("user.dir"), imagePath);
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
            Image scaled = img.getScaledInstance(
                    EmployeeBasicDetailsPanelHelper.PHOTO_SIZE,
                    EmployeeBasicDetailsPanelHelper.PHOTO_SIZE,
                    Image.SCALE_SMOOTH);
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
        Employee updated = new Employee();
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            if ("EMPLOYEE_CODE".equalsIgnoreCase(column)) {
                continue;
            }
            String value = valueFor(column);
            if (isEmpty(value)) {
                continue;
            }
            if ("NID".equalsIgnoreCase(column)) {
                value = CnicFormatter.format(value);
            }
            EmployeeBasicFieldUtil.writeValue(updated, column, value);
        }
        return updated;
    }

    public String validationMessage() {
        String cnic = valueFor("NID");
        if (!cnic.isBlank() && !CnicFormatter.isValid(cnic)) {
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
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty()
                || trimmed.equalsIgnoreCase("N/A")
                || trimmed.equalsIgnoreCase("NA")
                || trimmed.equalsIgnoreCase("NULL")
                || trimmed.equals("-");
    }

    private String displayValue(String value) {
        return isEmpty(value) ? "" : value;
    }

    public File getSelectedImage() {
        return selectedImage;
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
