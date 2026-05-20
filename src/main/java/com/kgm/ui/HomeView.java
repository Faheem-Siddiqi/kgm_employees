package com.kgm.ui;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.ui.panel.EmployeeTablePanel;
import com.kgm.ui.panel.ExcelImportButton;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.HomeViewHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class HomeView extends JFrame {
    public HomeView() {
        HomeViewHelper.applyFrame(this);

        EmployeeRecordDao repo = new EmployeeRecordDao();
        EmployeeTablePanel tablePanel = new EmployeeTablePanel(repo);

        JPanel top = HomeViewHelper.createTopPanel();
        top.add(new HeaderPanel("Home Dashboard"), BorderLayout.NORTH);

        JPanel searchRow = HomeViewHelper.createSearchRow();
        JTextField searchField = HomeViewHelper.createSearchField("Search Employee Code");

        JButton searchBtn = new JButton("Search");
        HomeViewHelper.styleSearchButton(searchBtn);
        searchField.addActionListener(e -> searchBtn.doClick());

        JButton clearBtn = new JButton("Clear");
        HomeViewHelper.styleClearButton(clearBtn);
        HomeViewHelper.setTextButtonEnabled(clearBtn, false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateClearButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateClearButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateClearButton();
            }

            private void updateClearButton() {
                HomeViewHelper.setTextButtonEnabled(clearBtn, !searchField.getText().trim().isEmpty());
            }
        });

        searchBtn.addActionListener(e -> {
            String empCode = searchField.getText().trim();
            if (empCode.isEmpty()) {
                DialogHelper.warning(this, "Employee Code Required", "Enter Employee Code.");
                return;
            }

            var emp = repo.getEmployeeByCode(empCode);
            if (emp != null) {
                tablePanel.showSingleEmployee(emp);
            } else {
                DialogHelper.info(this, "No Result", "No employee found.");
            }
        });

        clearBtn.addActionListener(e -> {
            searchField.setText("");
            tablePanel.reload();
            HomeViewHelper.setTextButtonEnabled(clearBtn, false);
        });

        HomeViewHelper.addSearchControls(searchRow, searchField, searchBtn, clearBtn);

        JPanel northContainer = HomeViewHelper.createNorthContainer();
        northContainer.add(top, BorderLayout.NORTH);
        northContainer.add(searchRow, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);

        JPanel btnRow = HomeViewHelper.createButtonRow();
        ExcelImportButton excelBtn = new ExcelImportButton(() -> {
            System.out.println("Import Excel clicked");
        });

        JButton addBtn = new JButton("Add Employee");
        HomeViewHelper.styleAddButton(addBtn);
        addBtn.addActionListener(e -> {
            new EmployeeRegistrationView().setVisible(true);
            dispose();
        });

        JButton refreshBtn = new JButton("Refresh");
        HomeViewHelper.styleRefreshButton(refreshBtn);
        refreshBtn.addActionListener(e -> tablePanel.reload());

        btnRow.add(excelBtn);
        btnRow.add(addBtn);
        btnRow.add(refreshBtn);
        northContainer.add(btnRow, BorderLayout.SOUTH);

        JPanel body = HomeViewHelper.createBodyPanel();
        body.add(tablePanel, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }
}

