package com.kgm.ui;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeDao;
import com.kgm.model.Employee;
import com.kgm.ui.panel.DocumentPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.FormPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeInductionStyle;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.io.File;

public class EmployeeInduction extends JFrame {
    private JButton nextBackBtn;
    private JButton submitBtn;
    private JButton backBtn;

    public EmployeeInduction() {
        EmployeeInductionStyle.applyFrame(this);

        JPanel topContainer = EmployeeInductionStyle.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Induction"), BorderLayout.NORTH);

        JPanel backRow = EmployeeInductionStyle.createBackRow();
        backBtn = new JButton("Back");
        EmployeeInductionStyle.styleBackButton(backBtn);
        backBtn.addActionListener(e -> {
            this.dispose();
            new HomeView();
        });
        backRow.add(backBtn);

        topContainer.add(backRow, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeInductionStyle.createCenterWrapper();
        JTabbedPane tabs = new JTabbedPane();
        FormPanel formPanel = new FormPanel();
        DocumentPanel documentPanel = new DocumentPanel();

        tabs.addTab("Form", formPanel);
        tabs.addTab("Documents", documentPanel);
        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel footerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        nextBackBtn = new JButton("Next");
        submitBtn = new JButton("Submit");
        EmployeeInductionStyle.styleFooterButton(nextBackBtn);
        EmployeeInductionStyle.styleFooterButton(submitBtn);
        submitBtn.setEnabled(false);

        footerActions.add(nextBackBtn);
        footerActions.add(submitBtn);
        add(new FooterPanel(footerActions), BorderLayout.SOUTH);

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

        nextBackBtn.addActionListener(e -> {
            int index = tabs.getSelectedIndex();
            if (index == 0) {
                tabs.setSelectedIndex(1);
            } else {
                tabs.setSelectedIndex(0);
            }
        });

        submitBtn.addActionListener(e -> {
            try {
                Employee emp = formPanel.getEmployeeFromForm();
                String empCode = emp.getEMPLOYEE_CODE();
                String basePath = System.getProperty("user.dir") + "/employees/";
                File empDir = new File(basePath + empCode);
                File docDir = new File(empDir, "documents");
                if (!docDir.exists()) {
                    docDir.mkdirs();
                }

                File img = formPanel.getSelectedImage();
                if (img != null) {
                    File dest = new File(empDir, "EMP_IMG.jpg");
                    try (java.io.InputStream in = new java.io.FileInputStream(img);
                            java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        emp.setEMP_IMG("employees/" + empCode + "/EMP_IMG.jpg");
                    }
                }

                DocumentPanel docPanel = documentPanel;
                String[] docs = docPanel.getAllDocumentPaths();
                if (docs != null) {
                    String[] fileNames = {
                            "CNIC_COPY.jpg",
                            "EOBI_CARD_COPY.jpg",
                            "SS_CARD_COPY.jpg",
                            "FINAL_SETTLEMENT.jpg",
                            "CLEARANCE_CERT.jpg",
                            "JOB_APPOINTMENT.jpg",
                            "APPLICATION_DOC.jpg",
                            "ISSUANCE_DOC.jpg",
                            "SETTLEMENT_DOC.jpg",
                            "TRIAL_CARD.jpg",
                            "INTERVIEW_DOC.jpg",
                            "SERVICE_LETTER.jpg",
                            "EXTENSION_LETTER.jpg",
                            "RETIREMENT_LETTER.jpg",
                            "COVID_CERT.jpg"
                    };

                    for (int i = 0; i < docs.length; i++) {
                        if (docs[i] != null) {
                            File src = new File(docs[i]);
                            File dest = new File(docDir, fileNames[i]);
                            try (java.io.InputStream in = new java.io.FileInputStream(src);
                                    java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = in.read(buffer)) > 0) {
                                    out.write(buffer, 0, len);
                                }
                            }

                            String dbPath = "employees/" + empCode + "/documents/" + fileNames[i];
                            switch (i) {
                                case 0 -> emp.setCNIC_COPY(dbPath);
                                case 1 -> emp.setEOBI_CARD_COPY(dbPath);
                                case 2 -> emp.setSS_CARD_COPY(dbPath);
                                case 3 -> emp.setFINAL_SETTLEMENT(dbPath);
                                case 4 -> emp.setCLEARANCE_CERT(dbPath);
                                case 5 -> emp.setJOB_APPOINTMENT(dbPath);
                                case 6 -> emp.setAPPLICATION_DOC(dbPath);
                                case 7 -> emp.setISSUANCE_DOC(dbPath);
                                case 8 -> emp.setSETTLEMENT_DOC(dbPath);
                                case 9 -> emp.setTRIAL_CARD(dbPath);
                                case 10 -> emp.setINTERVIEW_DOC(dbPath);
                                case 11 -> emp.setSERVICE_LETTER(dbPath);
                                case 12 -> emp.setEXTENSION_LETTER(dbPath);
                                case 13 -> emp.setRETIREMENT_LETTER(dbPath);
                                case 14 -> emp.setCOVID_CERT(dbPath);
                            }
                        }
                    }
                }

                EmployeeDao dao = new EmployeeDao(DatabaseConnection.getConnection());
                dao.insertEmployee(emp);
                DialogHelper.success(this, "Employee saved successfully.");
            } catch (Exception ex) {
                DialogHelper.error(
                        this,
                        "Error",
                        "Failed to save employee:\n" + ex.getMessage());
            }
        });
        setVisible(true);
    }
}
