package com.kgm.ui;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeDao;
import com.kgm.model.Employee;
import com.kgm.ui.panel.DocumentPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.FormPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeInductionHelper;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class EmployeeInduction extends JFrame {
    public EmployeeInduction() {
        EmployeeInductionHelper.applyFrame(this);

        Runnable onBack = () -> {
            this.dispose();
            new HomeView();
        };

        JPanel topContainer = EmployeeInductionHelper.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Induction"), BorderLayout.NORTH);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeInductionHelper.createCenterWrapper();
        centerWrapper.add(EmployeeInductionHelper.screenHeader(onBack), EmployeeInductionHelper.pageConstraints(0));

        JTabbedPane tabs = new HugHeightTabbedPane();
        FormPanel formPanel = new FormPanel();
        DocumentPanel documentPanel = new DocumentPanel();

        JButton backButton = new JButton("Back");
        JButton submitButton = new JButton("Submit");
        EmployeeInductionHelper.styleSecondaryButton(backButton);
        EmployeeInductionHelper.stylePrimaryButton(submitButton);
        JPanel documentActions = EmployeeInductionHelper.createActionRow();
        documentActions.add(backButton);
        documentActions.add(submitButton);

        tabs.addTab("Form", EmployeeInductionHelper.createTabContent(formPanel, null));
        tabs.addTab("Documents", EmployeeInductionHelper.createTabContent(documentPanel, documentActions));

        EmployeeInductionHelper.styleTabs(tabs);
        tabs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent event) {
                int tabIndex = tabs.indexAtLocation(event.getX(), event.getY());
                if (tabIndex >= 0 && tabs.isEnabledAt(tabIndex)) {
                    tabs.setSelectedIndex(tabIndex);
                    tabs.revalidate();
                    tabs.repaint();
                }
            }
        });

        centerWrapper.add(tabs, EmployeeInductionHelper.pageConstraints(1));

        JScrollPane pageScroll = EmployeeInductionHelper.createPageScrollPane(centerWrapper);
        tabs.addChangeListener(event -> SwingUtilities.invokeLater(() -> {
            centerWrapper.revalidate();
            centerWrapper.repaint();
            pageScroll.getVerticalScrollBar().setValue(0);
        }));
        EmployeeInductionHelper.installPageWheelForwarding(pageScroll, centerWrapper);
        add(pageScroll, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        backButton.addActionListener(e -> tabs.setSelectedIndex(0));

        submitButton.addActionListener(e -> {
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
                            "COVID_CERT.jpg",
                            "DISCIPLINARY_I.jpg",
                            "DISCIPLINARY_II.jpg",
                            "DISCIPLINARY_III.jpg"
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
                                case 15 -> emp.setDISCIPLINARY_I(dbPath);
                                case 16 -> emp.setDISCIPLINARY_II(dbPath);
                                case 17 -> emp.setDISCIPLINARY_III(dbPath);
                            }
                        }
                    }
                }

                EmployeeDao dao = new EmployeeDao(DatabaseConnection.getConnection());
                dao.insertEmployee(emp);
                DialogHelper.success(this, "Employee saved successfully.");
                formPanel.clearForm();
                documentPanel.clearDocuments();
                tabs.setSelectedIndex(0);
                SwingUtilities.invokeLater(() -> {
                    centerWrapper.revalidate();
                    centerWrapper.repaint();
                    pageScroll.getVerticalScrollBar().setValue(0);
                });
            } catch (Exception ex) {
                DialogHelper.error(
                        this,
                        "Error",
                        "Failed to save employee:\n" + ex.getMessage());
            }
        });
        setVisible(true);
    }

    private static class HugHeightTabbedPane extends JTabbedPane {
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            Component selected = getSelectedComponent();
            if (selected == null) {
                return preferred;
            }

            int tallestTabContentHeight = 0;
            for (int index = 0; index < getTabCount(); index++) {
                Component component = getComponentAt(index);
                if (component != null) {
                    tallestTabContentHeight = Math.max(
                            tallestTabContentHeight,
                            component.getPreferredSize().height
                    );
                }
            }

            int tabChromeHeight = Math.max(0, preferred.height - tallestTabContentHeight);
            Dimension selectedSize = selected.getPreferredSize();
            return new Dimension(preferred.width, selectedSize.height + tabChromeHeight);
        }
    }
}
