package com.kgm.ui.panel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Date;
import com.kgm.model.Employee;
public class FormViewPanel extends JPanel {
    // ================= IMAGE =================
    private JLabel photoPreview;
    private JLabel uploadLabel;
    private final Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font inputFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final int PHOTO_SIZE = 200;
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
    private JSpinner appointmentSpinner;
    private JSpinner leavingSpinner;
    private JTextArea addressArea;


    //   private String CURRENT_ADR;
    // ✔ ADDED (ONLY CHANGE)
    private JLabel infoLabel;
    public FormViewPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(buildForm());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }
    public FormViewPanel(Employee employee) {
        this();
        this.employee = employee;
        loadEmployeeData();
    }
    private void loadEmployeeData() {
        if (employee == null) {
            System.out.println("FormViewPanel: Employee is NULL");
            return;
        }
        empIdField.setText(employee.getEMPLOYEE_CODE());
        nameField.setText(employee.getEMP_NAME());
        fatherNameField.setText(employee.getFATHER_NAME());
        cnicField.setText(employee.getNID());
        phoneField.setText(employee.getEMP_CONTNO());
        emailField.setText(employee.getPERSONAL_EMAIL());
        departmentField.setText(employee.getDEPARTMENT());
        designationField.setText(employee.getDESIGNATION());
        if (employee.getGENDER() != null)
            genderCombo.setSelectedItem(employee.getGENDER());
        if (employee.getRESIGN_REASON() != null)
            reasonCombo.setSelectedItem(employee.getRESIGN_REASON());
        addressArea.setText(employee.getPERMANENT_ADR());
        if (employee.getEMP_IMG() != null
                && !employee.getEMP_IMG().trim().isEmpty()) {
            try {
                File imgFile = new File(employee.getEMP_IMG());
                System.err.println("Attempting to load image from: " + imgFile.getAbsolutePath());
                if (imgFile.exists()) {
                    BufferedImage img = ImageIO.read(imgFile);
                    Image scaled = img.getScaledInstance(
                            PHOTO_SIZE,
                            PHOTO_SIZE,
                            Image.SCALE_SMOOTH);
                    photoPreview.setIcon(new ImageIcon(scaled));
                    photoPreview.setText("");
                    // ✔ HIDE BOTH UI ELEMENTS
                    uploadLabel.setVisible(false);
                    if (infoLabel != null) {
                        infoLabel.setVisible(false);
                    }
                    System.out.println("Image loaded: " + employee.getEMP_IMG());
                } else {
                    System.out.println("Image not found: " + employee.getEMP_IMG());
                }
            } catch (Exception ex) {
                System.out.println("Image load error: " + ex.getMessage());
            }
        }
    }
    private JPanel buildForm() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
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
        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(240, 300));
        left.setBackground(Color.WHITE);
        photoPreview = new JLabel("Photo", SwingConstants.CENTER);
        photoPreview.setPreferredSize(new Dimension(220, 220));
        photoPreview.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        photoPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });
        uploadLabel = new JLabel("Upload / Replace");
        uploadLabel.setForeground(new Color(0, 102, 204));
        uploadLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        uploadLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });
        JPanel bottom = new JPanel();
        bottom.setBackground(Color.WHITE);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        infoLabel = new JLabel("JPEG only • Max 400KB");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottom.add(infoLabel);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(uploadLabel);
        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);
        return left;
    }
    private JPanel buildRightForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235, 235, 235)),
                new EmptyBorder(20, 20, 20, 20)));
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
        genderCombo = new JComboBox<>(new String[] { "Male", "Female", "Other" });
        reasonCombo = new JComboBox<>(new String[] { "Layoff", "Retirement", "Others" });
        addRow(panel, gbc, y++, "Gender", genderCombo, "Reason", reasonCombo);
        appointmentSpinner = new JSpinner(
                new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        leavingSpinner = new JSpinner(
                new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        addRow(panel, gbc, y++, "Appointment Date", appointmentSpinner, "Leaving Date", leavingSpinner);
        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        addressArea = new JTextArea(4, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        addressArea.setFont(inputFont);
    
    
       addressArea = new JTextArea(4, 20);
addressArea.setLineWrap(true);
addressArea.setWrapStyleWord(true);
addressArea.setFont(inputFont);
addressArea.setPreferredSize(new Dimension(240, 90)); // fixed height



        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(addressArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
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
    private void chooseImage(JLabel target) {
        JFileChooser fc = new JFileChooser();
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                "JPEG Images (*.jpg, *.jpeg)", "jpg", "jpeg");
        fc.setFileFilter(filter);
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.length() > 400 * 1024) {
                JOptionPane.showMessageDialog(this, "Max 400KB allowed");
                return;
            }
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) {
                    JOptionPane.showMessageDialog(this, "Invalid Image");
                    return;
                }
                selectedImage = file;
                Image scaled = img.getScaledInstance(
                        PHOTO_SIZE,
                        PHOTO_SIZE,
                        Image.SCALE_SMOOTH);
                target.setIcon(new ImageIcon(scaled));
                target.setText("");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid Image");
            }
        }
    }
    class FormField extends JPanel {
        JLabel label;
        JComponent input;
        public FormField(String text, JComponent comp) {
            setLayout(new BorderLayout(6, 4));
            setBackground(Color.WHITE);
            label = new JLabel(text);
            label.setFont(labelFont);
            label.setForeground(new Color(70, 70, 70));
            input = comp;
            input.setFont(inputFont);
            input.setPreferredSize(new Dimension(240, 34));
            add(label, BorderLayout.NORTH);
            add(input, BorderLayout.CENTER);
        }
    }
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
        e.setJOINING_DATE(appointmentSpinner.getValue().toString());
        e.setRESIGN_DATE(leavingSpinner.getValue().toString());
        e.setPERMANENT_ADR(addressArea.getText());
        return e;
    }
    public File getSelectedImage() {
        return selectedImage;
    }
}