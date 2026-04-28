package com.kgm.ui;
import javax.swing.*;
import java.awt.*;
import com.kgm.dao.EmployeeRepositoryDao;
import com.kgm.ui.panel.EmployeeTablePanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.ExcelImportButton;
public class HomeView extends JFrame {
    public HomeView() {
        setTitle("Home");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        EmployeeRepositoryDao repo = new EmployeeRepositoryDao();
        EmployeeTablePanel tablePanel = new EmployeeTablePanel(repo);
        // ================= TOP HEADER =================
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(new HeaderPanel("Home Dashboard"), BorderLayout.NORTH);
        // ================= SEARCH ROW =================
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setBorder(BorderFactory.createEmptyBorder(16, 12, 0, 12));
        searchRow.setOpaque(false);
        JTextField searchField = new JTextField();

        // styling
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(300, 34));
        JButton searchBtn = new JButton("Search");
        searchField.addActionListener(e -> searchBtn.doClick());
        searchBtn.setPreferredSize(new Dimension(90, 34));
        searchBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.setBackground(new Color(0, 38, 77));
        searchBtn.setForeground(Color.WHITE);
        JButton clearBtn = new JButton("Clear");
        clearBtn.setPreferredSize(new Dimension(80, 34));
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.setVisible(false);
        searchBtn.addActionListener(e -> {
            String empCode = searchField.getText().trim();
            if (empCode.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter Employee Code");
                return;
            }
            var emp = repo.getEmployeeByCode(empCode);
            if (emp != null) {
                tablePanel.showSingleEmployee(emp);
                clearBtn.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "No employee found");
            }
        });
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            tablePanel.reload();
            clearBtn.setVisible(false);
        });
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(searchBtn);
        rightPanel.add(clearBtn);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(rightPanel, BorderLayout.EAST);
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(top, BorderLayout.NORTH);
        northContainer.add(searchRow, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);
        // ================= BUTTON ROW =================
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        btnRow.setBorder(BorderFactory.createEmptyBorder(20, 15, 10, 15));
        btnRow.setOpaque(false);
        ExcelImportButton excelBtn = new ExcelImportButton(() -> {
            System.out.println("Import Excel clicked");
        });
        JButton addBtn = new JButton("Add Record");
        addBtn.setPreferredSize(new Dimension(120, 32));
        addBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addBtn.setFocusPainted(false);
        addBtn.setBackground(Color.WHITE);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        addBtn.addActionListener(e -> {
            new com.kgm.ui.EmployeeInduction().setVisible(true);
            dispose();
        });
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setPreferredSize(new Dimension(100, 32));
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(new Color(0, 38, 77));
        refreshBtn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnRow.add(excelBtn);
        btnRow.add(addBtn);
        btnRow.add(refreshBtn);
        northContainer.add(btnRow, BorderLayout.SOUTH);
        // ================= BODY =================
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10, 25, 0, 25));
        refreshBtn.addActionListener(e -> tablePanel.reload());
        body.add(tablePanel, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        setVisible(true);
    }
}