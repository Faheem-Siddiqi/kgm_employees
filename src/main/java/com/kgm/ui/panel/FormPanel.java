package com.kgm.ui.panel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Date;

public class FormPanel extends JPanel {

    private JLabel photoPreview;
    private JLabel uploadLabel;

    private final Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font inputFont = new Font("Segoe UI", Font.PLAIN, 13);

    private final int PHOTO_SIZE = 200;

    public FormPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(new JScrollPane(buildForm()), BorderLayout.CENTER);
    }

    // ================= ROOT LAYOUT FIXED =================
    private JPanel buildForm() {

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Color.WHITE);

        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;

        // LEFT IMAGE
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        root.add(buildLeftPanel(), gbc);

        // GAP
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        root.add(Box.createRigidArea(new Dimension(35, 1)), gbc);

        // FORM
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(buildRightForm(), gbc);

        return root;
    }

    // ================= LEFT PANEL =================
    private JPanel buildLeftPanel() {

        JPanel left = new JPanel(new BorderLayout(10, 10));

        left.setPreferredSize(new Dimension(220, 250));
        left.setMinimumSize(new Dimension(220, 250));
        left.setMaximumSize(new Dimension(220, 250));

        left.setBackground(Color.WHITE);

        photoPreview = new JLabel("No Image", SwingConstants.CENTER);

        photoPreview.setPreferredSize(new Dimension(220, 250));
        photoPreview.setMinimumSize(new Dimension(220, 250));
        photoPreview.setMaximumSize(new Dimension(220, 250));

        photoPreview.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        photoPreview.setFont(labelFont);
        photoPreview.setCursor(new Cursor(Cursor.HAND_CURSOR));

        photoPreview.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooseImage(photoPreview);
            }
        });

        uploadLabel = new JLabel("Upload");
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

        JLabel sizeInfo = new JLabel("File size allowed (400KB, JPEG only)");
        sizeInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bottom.add(sizeInfo);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(uploadLabel);

        left.add(photoPreview, BorderLayout.CENTER);
        left.add(bottom, BorderLayout.SOUTH);

        return left;
    }

    // ================= FORM =================
    private JPanel buildRightForm() {

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int y = 0;

        y = addRow(form, gbc, y, "Employee ID", "Name");
        y = addRow(form, gbc, y, "Father Name", "CNIC");
        y = addRow(form, gbc, y, "Phone", "Email");
        y = addRow(form, gbc, y, "Department", "Designation");

        y = addGenderAndReason(form, gbc, y);
        y = addDateRow(form, gbc, y);

        gbc.gridy = y;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        form.add(createFieldSingle("Permanent Address"), gbc);

        return form;
    }

    // ================= ROW =================
    private int addRow(JPanel panel, GridBagConstraints gbc, int y, String l1, String l2) {

        gbc.gridy = y;

        gbc.gridx = 0;
        panel.add(createField(l1), gbc);

        gbc.gridx = 1;
        panel.add(createField(l2), gbc);

        return y + 1;
    }

    // ================= FIELD =================
    private JPanel createField(String labelText) {

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(labelFont);

        JTextField field = new JTextField();
        field.setFont(inputFont);
        field.setPreferredSize(new Dimension(240, 32));

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 8, 5, 8)));

        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);

        return p;
    }

    // ================= FULL WIDTH =================
    private JPanel createFieldSingle(String labelText) {

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(labelFont);

        JTextArea area = new JTextArea(3, 20);
        area.setFont(inputFont);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        p.add(lbl, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ================= GENDER + REASON =================
    private int addGenderAndReason(JPanel panel, GridBagConstraints gbc, int y) {

        gbc.gridy = y;

        gbc.gridx = 0;
        panel.add(createGender(), gbc);

        gbc.gridx = 1;
        panel.add(createReason(), gbc);

        return y + 1;
    }

    private JPanel createGender() {

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel("Gender");
        lbl.setFont(labelFont);

        JComboBox<String> combo = new JComboBox<>(new String[] { "Male", "Female", "Other" });
        combo.setPreferredSize(new Dimension(240, 32));

        p.add(lbl, BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);

        return p;
    }

    // NEW DROPDOWN
    private JPanel createReason() {

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel("Reason of Leaving");
        lbl.setFont(labelFont);

        JComboBox<String> combo = new JComboBox<>(new String[] { "Layoff", "Retirement", "Others" });
        combo.setSelectedItem("Retirement");
        combo.setPreferredSize(new Dimension(240, 32));

        p.add(lbl, BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);

        return p;
    }

    // ================= DATE ROW =================
    private int addDateRow(JPanel panel, GridBagConstraints gbc, int y) {

        gbc.gridy = y;

        gbc.gridx = 0;
        panel.add(createDate("Date of Appointment"), gbc);

        gbc.gridx = 1;
        panel.add(createDate("Date of Leaving"), gbc);

        return y + 1;
    }

    private JPanel createDate(String labelText) {

        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(labelFont);

        JSpinner spinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(240, 32));

        p.add(lbl, BorderLayout.NORTH);
        p.add(spinner, BorderLayout.CENTER);

        return p;
    }

    // ================= IMAGE =================
    private void chooseImage(JLabel target) {

        JFileChooser fc = new JFileChooser();

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            File file = fc.getSelectedFile();

            if (file.length() > 400 * 1024) {
                JOptionPane.showMessageDialog(this, "File must be 400KB or less");
                return;
            }

            String name = file.getName().toLowerCase();
            if (!(name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
                JOptionPane.showMessageDialog(this, "Only JPEG format is allowed");
                return;
            }

            setImage(target, file);
            uploadLabel.setText("Replace");
        }
    }

    private void setImage(JLabel label, File file) {

        try {
            BufferedImage img = ImageIO.read(file);
            Image scaled = img.getScaledInstance(PHOTO_SIZE, PHOTO_SIZE, Image.SCALE_SMOOTH);

            label.setIcon(new ImageIcon(scaled));
            label.setText("");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Image File");
        }
    }
}