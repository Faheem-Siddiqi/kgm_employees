package com.kgm.ui;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeRegistrationDao;
import com.kgm.model.Employee;
import com.kgm.ui.panel.EmployeeDocumentUploadPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.EmployeeRegistrationFormPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.util.EmployeeDocumentUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class EmployeeRegistrationView extends JFrame {
    public EmployeeRegistrationView() {
        EmployeeRegistrationViewHelper.applyFrame(this);

        Runnable onBack = () -> {
            this.dispose();
            new HomeView();
        };

        JPanel topContainer = EmployeeRegistrationViewHelper.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Registration"), BorderLayout.NORTH);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeRegistrationViewHelper.createCenterWrapper();
        centerWrapper.add(EmployeeRegistrationViewHelper.screenHeader(onBack), EmployeeRegistrationViewHelper.pageConstraints(0));

        JTabbedPane tabs = new HugHeightTabbedPane();
        EmployeeRegistrationFormPanel formPanel = new EmployeeRegistrationFormPanel();
        EmployeeDocumentUploadPanel documentPanel = new EmployeeDocumentUploadPanel();

        JButton backButton = new JButton("Back");
        JButton submitButton = new JButton("Submit");
        EmployeeRegistrationViewHelper.styleSecondaryButton(backButton);
        EmployeeRegistrationViewHelper.stylePrimaryButton(submitButton);
        JPanel documentActions = EmployeeRegistrationViewHelper.createActionRow();
        documentActions.add(backButton);
        documentActions.add(submitButton);

        tabs.addTab("Form", EmployeeRegistrationViewHelper.createTabContent(formPanel, null));
        tabs.addTab("Documents", EmployeeRegistrationViewHelper.createTabContent(documentPanel, documentActions));

        EmployeeRegistrationViewHelper.styleTabs(tabs);
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

        centerWrapper.add(tabs, EmployeeRegistrationViewHelper.pageConstraints(1));

        JScrollPane pageScroll = EmployeeRegistrationViewHelper.createPageScrollPane(centerWrapper);
        tabs.addChangeListener(event -> SwingUtilities.invokeLater(() -> {
            centerWrapper.revalidate();
            centerWrapper.repaint();
            pageScroll.getVerticalScrollBar().setValue(0);
        }));
        EmployeeRegistrationViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);
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

                EmployeeDocumentUploadPanel docPanel = documentPanel;
                String[] docs = docPanel.getAllDocumentPaths();
                if (docs != null) {
                    for (int i = 0; i < docs.length; i++) {
                        if (docs[i] != null) {
                            File src = new File(docs[i]);
                            String storageName = EmployeeDocumentUtil.documentType(i).storageName();
                            File dest = new File(docDir, storageName);
                            try (java.io.InputStream in = new java.io.FileInputStream(src);
                                    java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = in.read(buffer)) > 0) {
                                    out.write(buffer, 0, len);
                                }
                            }

                            String dbPath = "employees/" + empCode + "/documents/" + storageName;
                            EmployeeDocumentUtil.setDocumentPath(emp, i, dbPath);
                        }
                    }
                }

                EmployeeRegistrationDao dao = new EmployeeRegistrationDao(DatabaseConnection.getConnection());
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

