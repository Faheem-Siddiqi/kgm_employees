package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationFormPanelHelper;
import com.kgm.util.CnicFormatter;
import com.kgm.util.EmployeeBasicFieldUtil;

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

public class EmployeeRegistrationFormPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private List<EmployeeFieldDefinition> definitions = EmployeeBasicFieldUtil.loadBasicDefinitions();
    private final Map<String, JComponent> inputsByColumn = new LinkedHashMap<>();

    private JLabel photoPreview;
    private File selectedImage;

    public EmployeeRegistrationFormPanel() {
        EmployeeRegistrationFormPanelHelper.stylePanel(this);
        add(EmployeeRegistrationFormPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
    }

    public void reloadFields() {
        definitions = EmployeeBasicFieldUtil.loadBasicDefinitions();
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

        photoPreview = EmployeeRegistrationFormPanelHelper.createPhotoPreview("Photo");
        photoPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });

        JLabel uploadLabel = new JLabel("Upload / Replace");
        EmployeeRegistrationFormPanelHelper.styleUploadLabel(uploadLabel);
        uploadLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });

        JPanel bottom = EmployeeRegistrationFormPanelHelper.createPhotoInfoPanel();
        bottom.add(EmployeeRegistrationFormPanelHelper.createPhotoInfoLabel("JPEG only - Max 400KB"));
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(uploadLabel);

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
            if (EmployeeBasicFieldUtil.isMultilineField(first.columnName())) {
                addFullWidthField(panel, gbc, row++, first);
                continue;
            }

            EmployeeFieldDefinition second = index + 1 < definitions.size() ? definitions.get(index + 1) : null;
            if (second != null && EmployeeBasicFieldUtil.isMultilineField(second.columnName())) {
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
            JComboBox<String> combo = new JComboBox<>(EmployeeBasicFieldUtil.dropdownOptions(definition, false));
            DropdownFieldSupport.configure(combo, definition.variableOptionField());
            input = combo;
        } else if (EmployeeBasicFieldUtil.isMultilineField(column)) {
            JTextArea area = new JTextArea(4, 20);
            EmployeeRegistrationFormPanelHelper.styleAddressArea(area);
            input = EmployeeRegistrationFormPanelHelper.createAddressScrollPane(area);
        } else {
            input = new JTextField();
            if ("NID".equalsIgnoreCase(column)) {
                CnicFormatter.installFormatter((JTextField) input);
            }
        }

        EmployeeRegistrationFormPanelHelper.styleInput(input);
        inputsByColumn.put(column, input);
        return input;
    }

    private void chooseImage(JLabel target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG Images (*.jpg, *.jpeg)", "jpg", "jpeg"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (file.length() > 400 * 1024) {
            DialogHelper.warning(this, "File Too Large", "Max 400KB allowed.");
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
            EmployeeBasicFieldUtil.writeValue(employee, column, value);
        }
        return employee;
    }

    public String validationMessage() {
        for (EmployeeFieldDefinition definition : definitions) {
            String column = definition.columnName();
            if (EmployeeBasicFieldUtil.isRequired(definition) && isEmpty(valueFor(column))) {
                return definition.label() + " is required.";
            }
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

    public File getSelectedImage() {
        return selectedImage;
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
            } else if (input instanceof JTextField textField) {
                textField.setText("");
            }
        }
        selectedImage = null;
        photoPreview.setIcon(null);
        photoPreview.setText("Photo");
        requestInitialFocus();
        revalidate();
        repaint();
    }

    private void requestInitialFocus() {
        JComponent employeeId = inputsByColumn.get("EMPLOYEE_CODE");
        if (employeeId != null) {
            employeeId.requestFocusInWindow();
        }
    }

    private class FormField extends JPanel {
        FormField(String text, JComponent input) {
            EmployeeRegistrationFormPanelHelper.styleFormField(this);
            add(EmployeeRegistrationFormPanelHelper.createFieldLabel(text), BorderLayout.NORTH);
            add(input, BorderLayout.CENTER);
        }
    }
}
