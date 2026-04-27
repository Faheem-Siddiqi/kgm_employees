package com.kgm.ui;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.FormPanel;
import com.kgm.ui.panel.DocumentPanel;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeDao;
import com.kgm.model.Employee;

public class EmployeeInduction extends JFrame {

    private JButton nextBackBtn;
    private JButton submitBtn;

    public EmployeeInduction() {

        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(new HeaderPanel("Employee Induction"), BorderLayout.NORTH);

        // ================= CENTER =================
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();

        FormPanel formPanel = new FormPanel();
        DocumentPanel documentPanel = new DocumentPanel();

        tabs.addTab("Form", formPanel);
        tabs.addTab("Documents", documentPanel);

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // ================= FOOTER =================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        nextBackBtn = new JButton("Next");
        submitBtn = new JButton("Submit");
        submitBtn.setEnabled(false); // disabled initially

        footer.add(nextBackBtn);
        footer.add(submitBtn);

        add(footer, BorderLayout.SOUTH);

        // ================= TAB CHANGE LOGIC =================
        tabs.addChangeListener((ChangeEvent e) -> {
            int index = tabs.getSelectedIndex();

            if (index == 0) {
                nextBackBtn.setText("Next");
                submitBtn.setEnabled(false);
            } else {
                nextBackBtn.setText("Back");
                submitBtn.setEnabled(true);
            }
        });

        // ================= NAVIGATION BUTTON =================
        nextBackBtn.addActionListener(e -> {
            int index = tabs.getSelectedIndex();

            if (index == 0) {
                tabs.setSelectedIndex(1);
            } else {
                tabs.setSelectedIndex(0);
            }
        });

        // ================= SUBMIT BUTTON (IMPORTANT PART) =================
       submitBtn.addActionListener(e -> {

    try {
        Employee emp = formPanel.getEmployeeFromForm();

        EmployeeDao dao = new EmployeeDao(
                DatabaseConnection.getConnection()
        );

        dao.insertEmployee(emp);

        JOptionPane.showMessageDialog(
                this,
                "Employee Saved Successfully!"
        );

    } catch (Exception ex) {

        JOptionPane.showMessageDialog(
                this,
                "Failed to Save Employee:\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
});

        setVisible(true);
    }
}