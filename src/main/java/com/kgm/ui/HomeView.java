package com.kgm.ui;

import javax.swing.*;
import java.awt.*;

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

        // ================= TOP HEADER =================
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);

        top.add(new HeaderPanel("Home Dashboard"), BorderLayout.NORTH);

        // ================= BUTTON ROW =================
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        btnRow.setBorder(BorderFactory.createEmptyBorder(20, 15, 0, 15));
        btnRow.setOpaque(false);

        // Excel Button
        ExcelImportButton excelBtn = new ExcelImportButton(() -> {
            System.out.println("Import Excel clicked");
        });

        // Add Record Button
        JButton addBtn = new JButton("Add Record");
        addBtn.setPreferredSize(new Dimension(120, 32));
        addBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addBtn.setFocusPainted(false);
        addBtn.setBackground(Color.WHITE);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // ================= FIXED DISPOSE =================
        addBtn.addActionListener(e -> {
            new com.kgm.ui.EmployeeInduction().setVisible(true);
            dispose();
        });

        btnRow.add(excelBtn);
        btnRow.add(addBtn);

        top.add(btnRow, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        // ================= BODY =================
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10, 25, 0, 25));

        EmployeeTablePanel tablePanel = new EmployeeTablePanel();
        body.add(tablePanel, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        setVisible(true);
    }
}