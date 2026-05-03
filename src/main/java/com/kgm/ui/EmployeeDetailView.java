package com.kgm.ui;

import com.kgm.dao.EmployeeRepositoryDao;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.OtherDetailsPanel;
import com.kgm.ui.panel.BasicDetailsPanel;
import com.kgm.model.Employee;
import com.kgm.ui.panel.DocumentViewPanel;

public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton backBtn;
    private JButton updateBtn;

    // 🔹 Constructor with employee code
    public EmployeeDetailView(String empCode) {

        this.empCode = (empCode != null) ? empCode.trim() : null;
        Employee emp = null;

        try {
            if (this.empCode != null && !this.empCode.isEmpty()) {
                emp = new EmployeeRepositoryDao().getFullEmployeeByCode(this.empCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "An unexpected error occurred.\nPlease contact the administrator.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        initializeUI(emp, true);
    }

    // 🔹 Default constructor
    public EmployeeDetailView() {
        initializeUI(null, false);
    }

    // 🔹 Common UI method (removes duplication)
    private void initializeUI(Employee emp, boolean isWithData) {

        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 🔸 Header
        JPanel topContainer = new JPanel(new BorderLayout());
        String title = (empCode != null)
                ? "Employee Record - " + empCode
                : "Employee Record";

        topContainer.add(new HeaderPanel(title), BorderLayout.NORTH);

        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backRow.setBackground(Color.WHITE);

        backBtn = new JButton("← Back");
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> {
            this.dispose();
            new HomeView();
        });

        backRow.add(backBtn);
        topContainer.add(backRow, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // 🔸 Center Tabs
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setBackground(Color.WHITE);

        JTabbedPane tabs = new JTabbedPane();

        if (isWithData) {
            tabs.addTab("Basic", new BasicDetailsPanel(emp));
            tabs.addTab("Others", new OtherDetailsPanel(emp));
        } else {
            tabs.addTab("Core", new BasicDetailsPanel());
            tabs.addTab("Details", new OtherDetailsPanel());
        }

        tabs.addTab("Documents", new DocumentViewPanel());

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // 🔸 Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        updateBtn = new JButton("Update");

        Dimension btnSize = new Dimension(110, 32);
        updateBtn.setPreferredSize(btnSize);
        updateBtn.setFocusPainted(false);
        updateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setBackground(new Color(0, 38, 77));
        updateBtn.setEnabled(false);

        footer.add(updateBtn);
        add(footer, BorderLayout.SOUTH);

        // 🔸 Tab behavior
        tabs.addChangeListener(e -> {
            updateBtn.setEnabled(tabs.getSelectedIndex() != 0);
        });

        // 🔸 Update action
        updateBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Update logic goes here");
        });

        setVisible(true);
    }
}