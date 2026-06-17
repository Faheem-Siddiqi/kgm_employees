package com.kgm.ui;

import com.kgm.config.DatabaseConnection;
import com.kgm.dao.EmployeeRegistrationDao;
import com.kgm.model.Employee;
import com.kgm.ui.component.EmployeeStorageStatusBanner;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.panel.EmployeeDocumentUploadPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.EmployeeRegistrationFormPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.styling.AppTabsHelper;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeRegistrationViewHelper;
import com.kgm.util.ApplicationStartup;
import com.kgm.util.EmployeeDocumentUtil;
import com.kgm.util.EmployeeFormMetadata;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRegistrationView extends JFrame {
    private EmployeeRegistrationFormPanel formPanel;
    private EmployeeDocumentUploadPanel documentPanel;
    private EmployeeStorageStatusBanner storageStatusBanner;
    private JPanel centerWrapper;
    private JScrollPane pageScroll;
    private Runnable onBack;

    public EmployeeRegistrationView() {
        EmployeeRegistrationViewHelper.applyFrame(this);

        onBack = () -> {
            new HomeView();
            this.dispose();
        };

        JPanel topContainer = EmployeeRegistrationViewHelper.createTopContainer();
        topContainer.add(new HeaderPanel("Register Ex-Employee"), BorderLayout.NORTH);
        storageStatusBanner = new EmployeeStorageStatusBanner(this);
        topContainer.add(EmployeeStorageStatusBanner.stickyRow(storageStatusBanner), BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        centerWrapper = EmployeeRegistrationViewHelper.createCenterWrapper();
        centerWrapper.add(EmployeeRegistrationViewHelper.screenHeader(onBack), EmployeeRegistrationViewHelper.pageConstraints(0));
        centerWrapper.add(createLoadingPanel(), EmployeeRegistrationViewHelper.pageConstraints(1));

        pageScroll = EmployeeRegistrationViewHelper.createPageScrollPane(centerWrapper);
        EmployeeRegistrationViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);
        add(pageScroll, BorderLayout.CENTER);
        add(new FooterPanel(), BorderLayout.SOUTH);

        if (ApplicationStartup.isReady()) {
            try {
                showRegistrationContent(EmployeeFormMetadata.snapshot());
            } catch (RuntimeException exception) {
                loadRegistrationContentAsync();
            }
        } else {
            SwingUtilities.invokeLater(this::loadRegistrationContentAsync);
        }
        setVisible(true);
    }

    private JComponent createLoadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JLabel label = new JLabel("Preparing employee form...");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(99, 115, 129));
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private void loadRegistrationContentAsync() {
        SwingWorker<EmployeeFormMetadata, Void> worker = new SwingWorker<>() {
            @Override
            protected EmployeeFormMetadata doInBackground() {
                return EmployeeFormMetadata.snapshot();
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    showRegistrationContent(get());
                } catch (Exception exception) {
                    showRegistrationContent(EmployeeFormMetadata.fallback());
                    DialogHelper.warning(
                            EmployeeRegistrationView.this,
                            "Employee Form",
                            "Unable to load latest field settings. Default fields are shown."
                    );
                }
            }
        };
        worker.execute();
    }

    private void showRegistrationContent(EmployeeFormMetadata metadata) {
        centerWrapper.removeAll();
        centerWrapper.add(EmployeeRegistrationViewHelper.screenHeader(onBack), EmployeeRegistrationViewHelper.pageConstraints(0));

        JTabbedPane tabs = new HugHeightTabbedPane();
        formPanel = new EmployeeRegistrationFormPanel(metadata.basicDefinitions(), metadata.profileImageRequired());
        documentPanel = new EmployeeDocumentUploadPanel();
        documentPanel.setProfileImageUploadListener(formPanel::setSelectedImageFromDocumentUpload);
        formPanel.setSelectedImageListener(documentPanel::setProfileImageFromMainTab);

        JButton backButton = new JButton("Back");
        JButton submitButton = new JButton("Submit");
        EmployeeRegistrationViewHelper.styleSecondaryButton(backButton);
        EmployeeRegistrationViewHelper.stylePrimaryButton(submitButton);
        JPanel documentActions = EmployeeRegistrationViewHelper.createActionRow();
        documentActions.add(backButton);
        documentActions.add(submitButton);

        tabs.addTab("Form", EmployeeRegistrationViewHelper.createTabContent(formPanel, null));
        tabs.addTab("Documents", EmployeeRegistrationViewHelper.createTabContent(documentPanel, documentActions));

        AppTabsHelper.styleTabs(tabs);
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

        tabs.addChangeListener(event -> SwingUtilities.invokeLater(() -> {
            centerWrapper.revalidate();
            centerWrapper.repaint();
            pageScroll.getVerticalScrollBar().setValue(0);
        }));
        EmployeeRegistrationViewHelper.installPageWheelForwarding(pageScroll, centerWrapper);
        backButton.addActionListener(e -> tabs.setSelectedIndex(0));

        submitButton.addActionListener(e -> {
            String validationMessage = formPanel.validationMessage();
            if (validationMessage != null) {
                DialogHelper.warning(this, "Check Employee Details", validationMessage);
                return;
            }

            List<String> missingRequiredUploads = missingRequiredUploads(documentPanel);
            if (!missingRequiredUploads.isEmpty()) {
                DialogHelper.errorSections(
                        this,
                        "Upload Required Documents",
                        "Mandatory uploads\nUpload these mandatory documents before saving the employee record.",
                        "Missing documents\n" + bulletList(missingRequiredUploads)
                );
                tabs.setSelectedIndex(formPanel.isRequiredProfileImageMissing() ? 0 : 1);
                return;
            }

            Employee emp = formPanel.getEmployeeFromForm();
            String empCode = emp.getEMPLOYEE_CODE();
            File selectedImage = formPanel.getSelectedImage();
            String[] selectedDocuments = documentPanel.getAllDocumentPaths();
            LoadingOverlay.Handle loader = LoadingOverlay.show(
                    this,
                    "Saving Employee",
                    "Copying files and saving employee record..."
            );

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    saveEmployeeRecord(emp, empCode, selectedImage, selectedDocuments);
                    return null;
                }

                @Override
                protected void done() {
                    loader.close();
                    try {
                        get();
                        DialogHelper.success(EmployeeRegistrationView.this, "Employee saved successfully.");
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
                                EmployeeRegistrationView.this,
                                "Error",
                                "Failed to save employee:\n" + ex.getMessage());
                    }
                }
            };
            worker.execute();
        });
        centerWrapper.revalidate();
        centerWrapper.repaint();
        pageScroll.getVerticalScrollBar().setValue(0);
    }

    @Override
    public void dispose() {
        if (storageStatusBanner != null) {
            storageStatusBanner.dispose();
            storageStatusBanner = null;
        }
        super.dispose();
    }

    private void saveEmployeeRecord(
            Employee emp,
            String empCode,
            File selectedImage,
            String[] selectedDocuments
    ) throws Exception {
        if (selectedImage != null) {
            emp.setEMP_IMG(EmployeeDocumentUtil.copyProfileImageToEmployeeStorage(empCode, selectedImage));
        }

        if (selectedDocuments != null) {
            for (int i = 0; i < selectedDocuments.length; i++) {
                if (selectedDocuments[i] == null) {
                    continue;
                }

                File src = new File(selectedDocuments[i]);
                String dbPath = EmployeeDocumentUtil.copyDocumentToEmployeeStorage(empCode, i, src);
                EmployeeDocumentUtil.setDocumentPath(emp, i, dbPath);
            }
        }

        EmployeeRegistrationDao dao = new EmployeeRegistrationDao(DatabaseConnection.getConnection());
        dao.insertEmployee(emp);
    }

    public void refreshDynamicFields() {
        if (formPanel != null) {
            formPanel.reloadFields();
        }
        if (documentPanel != null) {
            documentPanel.reloadDocumentRequirements();
        }
        revalidate();
        repaint();
    }

    private List<String> missingRequiredUploads(EmployeeDocumentUploadPanel documentPanel) {
        List<String> missing = new ArrayList<>();
        if (formPanel.isRequiredProfileImageMissing()) {
            missing.add("Employee Photo");
        }
        missing.addAll(documentPanel.missingRequiredDocumentLabels());
        return missing;
    }

    private String bulletList(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(value);
        }
        return builder.toString();
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

