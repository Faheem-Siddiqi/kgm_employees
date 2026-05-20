package com.kgm.ui.panel;
import java.text.SimpleDateFormat;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Date;
import com.kgm.model.Employee;
import com.kgm.ui.component.UniversalDatePicker;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationFormPanelHelper;

public class EmployeeRegistrationFormPanel extends JPanel {

    // ================= IMAGE =================
    private JLabel photoPreview;
    private JLabel uploadLabel;
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // ✔ ADDED (ONLY CHANGE)
    private File selectedImage;

    // ================= ALL DB FIELDS (EXPLICIT) =================
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
    private UniversalDatePicker appointmentPicker;
    private UniversalDatePicker leavingPicker;
    private JTextArea addressArea;
    //  private JTextArea addressArea;

    public EmployeeRegistrationFormPanel() {
        EmployeeRegistrationFormPanelHelper.stylePanel(this);
        add(EmployeeRegistrationFormPanelHelper.createFormContent(buildForm()), BorderLayout.NORTH);
    }

    // ================= ROOT =================
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

    // ================= IMAGE PANEL =================
    private JPanel buildLeftPanel() {
        JPanel left = EmployeeRegistrationFormPanelHelper.createPhotoPanel();

        photoPreview = EmployeeRegistrationFormPanelHelper.createPhotoPreview("Photo");

        photoPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });

        uploadLabel = new JLabel("Upload / Replace");
        EmployeeRegistrationFormPanelHelper.styleUploadLabel(uploadLabel);

        uploadLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });

        JPanel bottom = EmployeeRegistrationFormPanelHelper.createPhotoInfoPanel();

        JLabel info = EmployeeRegistrationFormPanelHelper.createPhotoInfoLabel("JPEG only • Max 400KB");

        bottom.add(info);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(uploadLabel);

        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);

        return left;
    }

    // ================= RIGHT FORM =================
    private JPanel buildRightForm() {
        JPanel panel = EmployeeRegistrationFormPanelHelper.createRightFormPanel();

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

        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        reasonCombo = new JComboBox<>(new String[]{"Layoff", "Retirement", "Others"});
        addRow(panel, gbc, y++, "Gender", genderCombo, "Reason", reasonCombo);

        appointmentPicker = new UniversalDatePicker(new Date());

        leavingPicker = new UniversalDatePicker(new Date());

        addRow(panel, gbc, y++, "Date of Arrival", appointmentPicker, "Leaving Date", leavingPicker);

        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        addressArea = new JTextArea(4, 20);
        EmployeeRegistrationFormPanelHelper.styleAddressArea(addressArea);

        JScrollPane scroll = EmployeeRegistrationFormPanelHelper.createAddressScrollPane(addressArea);

        panel.add(new FormField("Permanent Address", scroll), gbc);

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

    // ================= IMAGE HANDLING (ONLY FIXED PART) =================
  private void chooseImage(JLabel target) {
    JFileChooser fc = new JFileChooser();

    // ✅ ADD THIS (JPEG FILTER ONLY)
    javax.swing.filechooser.FileNameExtensionFilter filter =
            new javax.swing.filechooser.FileNameExtensionFilter(
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

            // ✔ STORE FOR MAIN PANEL
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
}

    // ================= FORM FIELD =================
    class FormField extends JPanel {
        JLabel label;
        JComponent input;

        public FormField(String text, JComponent comp) {
            EmployeeRegistrationFormPanelHelper.styleFormField(this);

            label = EmployeeRegistrationFormPanelHelper.createFieldLabel(text);

            input = comp;
            EmployeeRegistrationFormPanelHelper.styleInput(input);

            add(label, BorderLayout.NORTH);
            add(input, BorderLayout.CENTER);
        }
    }

    // ================= DAO SUPPORT =================
    public Employee getEmployeeFromForm() {
        Employee e = new Employee();

        e.setEMPLOYEE_CODE(empIdField.getText());
        e.setEMP_NAME(nameField.getText());
        e.setFATHER_NAME(fatherNameField.getText());
        e.setNID(cnicField.getText());
        e.setEMP_CONTNO(phoneField.getText());
        e.setPERSONAL_EMAIL(emailField.getText());
        e.setDEPARTMENT(departmentField.getText());
        e.setDESIGNATION(designationField.getText());
        e.setGENDER(genderCombo.getSelectedItem().toString());
        e.setRESIGN_REASON(reasonCombo.getSelectedItem().toString());
        e.setJOINING_DATE(formatDbDate(appointmentPicker));
        e.setRESIGN_DATE(formatDbDate(leavingPicker));

        e.setPERMANENT_ADR(addressArea.getText());

        return e;
    }

    private String formatDbDate(UniversalDatePicker picker) {
        Date date = picker == null ? null : picker.getDate();
        return date == null ? "" : DB_DATE_FORMAT.format(date);
    }

    // ✔ ADDED (ONLY NEW METHOD FOR MAIN FILE)
    public File getSelectedImage() {
        return selectedImage;
    }

    public void clearForm() {
        empIdField.setText("");
        nameField.setText("");
        fatherNameField.setText("");
        cnicField.setText("");
        phoneField.setText("");
        emailField.setText("");
        departmentField.setText("");
        designationField.setText("");
        resetCombo(genderCombo);
        resetCombo(reasonCombo);
        appointmentPicker.setDate(new Date());
        leavingPicker.setDate(new Date());
        addressArea.setText("");
        selectedImage = null;
        photoPreview.setIcon(null);
        photoPreview.setText("Photo");
        empIdField.requestFocusInWindow();
        revalidate();
        repaint();
    }

    private void resetCombo(JComboBox<String> comboBox) {
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }
}

