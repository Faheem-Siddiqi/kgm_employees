package com.kgm.ui;

import com.kgm.dao.EmployeeRepositoryDao;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.FormViewPanel;
import com.kgm.model.Employee;
import com.kgm.ui.panel.DocumentViewPanel;

public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton backBtn;
    private JButton nextBackBtn;
    private JButton updateBtn;

    
    public EmployeeDetailView(String empCode) {

        EmployeeRepositoryDao dao = new EmployeeRepositoryDao();

        this.empCode = (empCode != null) ? empCode.trim() : null;

        Employee emp = null;

        try {
            if (this.empCode != null && !this.empCode.isEmpty()) {
                emp = dao.getFullEmployeeByCode(this.empCode);
            }
        } 
        
        catch (Exception ex) {
    ex.printStackTrace(); 

    JOptionPane.showMessageDialog(
            null,
            "An unexpected error occurred.\nPlease contact the administrator.",
            "Error",
            JOptionPane.ERROR_MESSAGE
    );

    emp = null;
}



        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(new HeaderPanel("Employee Record - " + this.empCode), BorderLayout.NORTH);

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

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setBackground(Color.WHITE);

        JTabbedPane tabs = new JTabbedPane();

        FormViewPanel formPanel = new FormViewPanel();
        DocumentViewPanel documentPanel = new DocumentViewPanel();

        tabs.addTab("Basic", new FormViewPanel(emp));
        tabs.addTab("Organizational ", formPanel);
        tabs.addTab("Personal ", formPanel);
        tabs.addTab("Finance", formPanel);
        tabs.addTab("Compliance", formPanel);
        tabs.addTab("Emergency", formPanel);
        tabs.addTab("Wellness", documentPanel);
        tabs.addTab("Documents", documentPanel);

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        nextBackBtn = new JButton("Next");
        updateBtn = new JButton("Update");

        Dimension btnSize = new Dimension(110, 32);
        nextBackBtn.setPreferredSize(btnSize);
        updateBtn.setPreferredSize(btnSize);

        nextBackBtn.setFocusPainted(false);
        updateBtn.setFocusPainted(false);

        nextBackBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        nextBackBtn.setForeground(Color.WHITE);
        updateBtn.setForeground(Color.WHITE);

        nextBackBtn.setBackground(new Color(0, 38, 77));
        updateBtn.setBackground(new Color(0, 38, 77));

        updateBtn.setEnabled(false);

        footer.add(nextBackBtn);
        footer.add(updateBtn);

        add(footer, BorderLayout.SOUTH);

        tabs.addChangeListener(e -> {
            int index = tabs.getSelectedIndex();
            nextBackBtn.setText(index == 0 ? "Next" : "Back");
            updateBtn.setEnabled(index != 0);
        });

        nextBackBtn.addActionListener(e -> {
            int index = tabs.getSelectedIndex();
            tabs.setSelectedIndex(index == 0 ? 1 : 0);
        });

        updateBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Update logic goes here");
        });

        setVisible(true);
    }

    // ✅ OLD CONSTRUCTOR (kept untouched)
    public EmployeeDetailView() {
        setTitle("Employee Form");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);

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

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerWrapper.setBackground(Color.WHITE);

        JTabbedPane tabs = new JTabbedPane();

        FormViewPanel formPanel = new FormViewPanel();
        DocumentViewPanel documentPanel = new DocumentViewPanel();

        tabs.addTab("Basic", formPanel);
        tabs.addTab("Organizational ", formPanel);
        tabs.addTab("Personal ", formPanel);
        tabs.addTab("Finance", formPanel);
        tabs.addTab("Compliance", formPanel);
        tabs.addTab("Emergency", formPanel);
        tabs.addTab("Wellness", documentPanel);
        tabs.addTab("Wellness", documentPanel);

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);

        nextBackBtn = new JButton("Next");
        updateBtn = new JButton("Update");

        Dimension btnSize = new Dimension(110, 32);
        nextBackBtn.setPreferredSize(btnSize);
        updateBtn.setPreferredSize(btnSize);

        nextBackBtn.setForeground(Color.WHITE);
        updateBtn.setForeground(Color.WHITE);

        nextBackBtn.setBackground(new Color(0, 38, 77));
        updateBtn.setBackground(new Color(0, 38, 77));

        updateBtn.setEnabled(false);

        footer.add(nextBackBtn);
        footer.add(updateBtn);

        add(footer, BorderLayout.SOUTH);

        setVisible(true);
    }
}