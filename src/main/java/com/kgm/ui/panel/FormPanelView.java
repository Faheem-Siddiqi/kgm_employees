package com.kgm.ui.panel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Date;

public class FormPanelView extends JPanel {

    // ================= IMAGE =================
    private JLabel photoPreview;
    private JLabel uploadLabel;
    private final Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font inputFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final int PHOTO_SIZE = 200;

    private File selectedImage;

    // ================= FIELDS =================
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

    public FormPanelView() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(buildForm());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);

        add(scroll, BorderLayout.CENTER);

        // dummy data for now
        loadDummyData();
    }

    // ================= ROOT =================
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

    // ================= IMAGE PANEL =================
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(240, 300));
        left.setBackground(Color.WHITE);

        photoPreview = new JLabel("Photo", SwingConstants.CENTER);
        photoPreview.setPreferredSize(new Dimension(220, 220));
        photoPreview.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));

        uploadLabel = new JLabel("Upload / Replace");
        uploadLabel.setForeground(new Color(0, 102, 204));
        uploadLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel bottom = new JPanel();
        bottom.setBackground(Color.WHITE);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JLabel info = new JLabel("JPEG only • Max 400KB");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        bottom.add(info);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(uploadLabel);

        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);

        return left;
    }

    // ================= RIGHT FORM =================
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

        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        reasonCombo = new JComboBox<>(new String[]{"Layoff", "Retirement", "Others"});
        addRow(panel, gbc, y++, "Gender", genderCombo, "Reason", reasonCombo);

        appointmentSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        leavingSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));

        addRow(panel, gbc, y++, "Appointment Date", appointmentSpinner, "Leaving Date", leavingSpinner);

        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        addressArea = new JTextArea(4, 20);
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

    // ================= DUMMY DATA =================
    public void loadDummyData() {

        empIdField.setText("EMP-1001");
        nameField.setText("Ali Raza");
        fatherNameField.setText("Muhammad Raza");
        cnicField.setText("35202-1234567-8");
        phoneField.setText("03001234567");
        emailField.setText("ali@example.com");
        departmentField.setText("IT");
        designationField.setText("Software Engineer");

        genderCombo.setSelectedItem("Male");
        reasonCombo.setSelectedItem("Others");

        addressArea.setText("Rawalpindi, Pakistan");
    }

    // ================= FORM FIELD =================
    class FormField extends JPanel {
        public FormField(String text, JComponent comp) {
            setLayout(new BorderLayout(6, 4));
            setBackground(Color.WHITE);

            JLabel label = new JLabel(text);
            label.setFont(labelFont);
            label.setForeground(new Color(70, 70, 70));

            comp.setFont(inputFont);
            comp.setPreferredSize(new Dimension(240, 34));

            add(label, BorderLayout.NORTH);
            add(comp, BorderLayout.CENTER);
        }
    }
}