package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeBasicDetailsPanelHelper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EmployeeBasicDetailsPanel extends JPanel {
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private JLabel photoPreview;
    private JLabel uploadLabel;
    private JLabel infoLabel;
    private File selectedImage;
    private Employee employee;

    private JTextField empIdField;
    private JTextField nameField;
    private JTextField fatherNameField;
    private JTextField cnicField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField departmentField;
    private JTextField designationField;
    private JComboBox<String> genderCombo;
    private JComboBox<String> reasonCombo;
    private boolean genderChanged;
    private boolean reasonChanged;
    private UniversalDatePicker appointmentPicker;
    private UniversalDatePicker leavingPicker;
    private boolean appointmentDateChanged;
    private boolean leavingDateChanged;
    private JTextArea addressArea;
    private JTextArea currentAddressArea;

    public EmployeeBasicDetailsPanel() {
        EmployeeBasicDetailsPanelHelper.stylePanel(this);
        add(EmployeeBasicDetailsPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
    }

    public EmployeeBasicDetailsPanel(Employee employee) {
        this();
        this.employee = employee;
        loadEmployeeData();
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

    private void loadEmployeeData() {
        if (employee == null) {
            return;
        }

        empIdField.setText(employee.getEMPLOYEE_CODE());
        empIdField.setEditable(false);

        nameField.setText(employee.getEMP_NAME());
        fatherNameField.setText(employee.getFATHER_NAME());
        cnicField.setText(employee.getNID());
        phoneField.setText(employee.getEMP_CONTNO());
        emailField.setText(employee.getPERSONAL_EMAIL());
        departmentField.setText(employee.getDEPARTMENT());
        designationField.setText(employee.getDESIGNATION());

        if (!isEmpty(employee.getGENDER())) {
            genderCombo.setSelectedItem(employee.getGENDER());
        } else {
            genderCombo.setSelectedIndex(0);
        }
        if (!isEmpty(employee.getRESIGN_REASON())) {
            reasonCombo.setSelectedItem(employee.getRESIGN_REASON());
        } else {
            reasonCombo.setSelectedIndex(0);
        }
        genderChanged = false;
        reasonChanged = false;

        setDateIfPresent(appointmentPicker, employee.getJOINING_DATE());
        setDateIfPresent(leavingPicker, employee.getRESIGN_DATE());
        appointmentDateChanged = false;
        leavingDateChanged = false;

        addressArea.setText(employee.getPERMANENT_ADR());
        currentAddressArea.setText(employee.getCURRENT_ADR());

        loadProfileImage();
    }

    private void setDateIfPresent(UniversalDatePicker picker, String value) {
        Date parsed = parseDate(value);
        if (parsed != null) {
            picker.setDate(parsed);
        }
        picker.setEnabled(true);
    }

    private Date parseDate(String value) {
        if (isEmpty(value)) {
            return null;
        }

        String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy HH:mm", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(value.trim());
            } catch (ParseException ignored) {
            }
        }
        return null;
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
        uploadLabel.setVisible(false);
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

        uploadLabel = new JLabel("Upload");
        EmployeeBasicDetailsPanelHelper.styleUploadLabel(uploadLabel);
        uploadLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (selectedImageCanChange()) {
                    chooseImage(photoPreview);
                }
            }
        });

        JPanel bottom = EmployeeBasicDetailsPanelHelper.createPhotoInfoPanel();
        infoLabel = EmployeeBasicDetailsPanelHelper.createPhotoInfoLabel("JPEG only - Max 400KB");
        bottom.add(infoLabel);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(uploadLabel);
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
        int y = 0;

        empIdField = new JTextField();
        nameField = new JTextField();
        addRow(panel, gbc, y++, "Employee ID", empIdField, "Name", nameField);

        fatherNameField = new JTextField();
        cnicField = new JTextField();
        addRow(panel, gbc, y++, "Father Name", fatherNameField, "CNIC", cnicField);

        phoneField = new JTextField();
        emailField = new JTextField();
        addRow(panel, gbc, y++, "Phone", phoneField, "Email", emailField);

        departmentField = new JTextField();
        designationField = new JTextField();
        addRow(panel, gbc, y++, "Department", departmentField, "Designation", designationField);

        genderCombo = new JComboBox<>(new String[] {"", "Male", "Female", "Other"});
        reasonCombo = new JComboBox<>(new String[] {"", "Layoff", "Retirement", "Others"});
        genderCombo.addActionListener(e -> genderChanged = true);
        reasonCombo.addActionListener(e -> reasonChanged = true);
        addRow(panel, gbc, y++, "Gender", genderCombo, "Reason", reasonCombo);

        appointmentPicker = new UniversalDatePicker();
        leavingPicker = new UniversalDatePicker();
        appointmentPicker.addDateChangeListener(() -> appointmentDateChanged = true);
        leavingPicker.addDateChangeListener(() -> leavingDateChanged = true);
        addRow(panel, gbc, y++, "Appointment Date", appointmentPicker, "Leaving Date", leavingPicker);

        addressArea = new JTextArea(4, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        EmployeeBasicDetailsPanelHelper.styleTextArea(addressArea);
        JScrollPane permanentAddressScroll = EmployeeBasicDetailsPanelHelper.createTextAreaScrollPane(addressArea);

        currentAddressArea = new JTextArea(4, 20);
        currentAddressArea.setLineWrap(true);
        currentAddressArea.setWrapStyleWord(true);
        EmployeeBasicDetailsPanelHelper.styleTextArea(currentAddressArea);
        JScrollPane currentAddressScroll = EmployeeBasicDetailsPanelHelper.createTextAreaScrollPane(currentAddressArea);

        addRow(panel, gbc, y++, "Permanent Address", permanentAddressScroll, "Current Address", currentAddressScroll);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y,
                        String l1, JComponent c1,
                        String l2, JComponent c2) {
        gbc.gridy = y;
        gbc.gridx = 0;
        panel.add(new FormField(l1, c1), gbc);
        gbc.gridx = 1;
        panel.add(new FormField(l2, c2), gbc);
    }

    private void chooseImage(JLabel target) {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG Images (*.jpg, *.jpeg)", "jpg", "jpeg");
        fc.setFileFilter(filter);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
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
                Image scaled = img.getScaledInstance(
                        EmployeeBasicDetailsPanelHelper.PHOTO_SIZE,
                        EmployeeBasicDetailsPanelHelper.PHOTO_SIZE,
                        Image.SCALE_SMOOTH);
                target.setIcon(new ImageIcon(scaled));
                target.setText("");
                uploadLabel.setText("Replace before saving");
            } catch (Exception e) {
                DialogHelper.warning(this, "Invalid Image", "Please select a valid JPEG image.");
            }
        }
    }

    class FormField extends JPanel {
        JLabel label;
        JComponent input;

        public FormField(String text, JComponent comp) {
            EmployeeBasicDetailsPanelHelper.styleFormField(this);
            label = EmployeeBasicDetailsPanelHelper.createFieldLabel(text);
            input = comp;
            EmployeeBasicDetailsPanelHelper.styleInput(input);
            add(label, BorderLayout.NORTH);
            add(input, BorderLayout.CENTER);
        }
    }

    public Employee getEmployeeFromForm() {
        Employee e = new Employee();

        if (!isEmpty(nameField.getText())) {
            e.setEMP_NAME(nameField.getText());
        }
        if (!isEmpty(fatherNameField.getText())) {
            e.setFATHER_NAME(fatherNameField.getText());
        }
        if (!isEmpty(cnicField.getText())) {
            e.setNID(cnicField.getText());
        }
        if (!isEmpty(phoneField.getText())) {
            e.setEMP_CONTNO(phoneField.getText());
        }
        if (!isEmpty(emailField.getText())) {
            e.setPERSONAL_EMAIL(emailField.getText());
        }
        if (!isEmpty(departmentField.getText())) {
            e.setDEPARTMENT(departmentField.getText());
        }
        if (!isEmpty(designationField.getText())) {
            e.setDESIGNATION(designationField.getText());
        }
        if (genderChanged && genderCombo.getSelectedItem() != null
                && !isEmpty(genderCombo.getSelectedItem().toString())) {
            e.setGENDER(genderCombo.getSelectedItem().toString());
        }
        if (reasonChanged && reasonCombo.getSelectedItem() != null
                && !isEmpty(reasonCombo.getSelectedItem().toString())) {
            e.setRESIGN_REASON(reasonCombo.getSelectedItem().toString());
        }
        if (appointmentDateChanged && appointmentPicker.getDate() != null) {
            e.setJOINING_DATE(DB_DATE_FORMAT.format(appointmentPicker.getDate()));
        }
        if (leavingDateChanged && leavingPicker.getDate() != null) {
            e.setRESIGN_DATE(DB_DATE_FORMAT.format(leavingPicker.getDate()));
        }
        if (!isEmpty(addressArea.getText())) {
            e.setPERMANENT_ADR(addressArea.getText());
        }
        if (!isEmpty(currentAddressArea.getText())) {
            e.setCURRENT_ADR(currentAddressArea.getText());
        }
        return e;
    }

    public File getSelectedImage() {
        return selectedImage;
    }
}
