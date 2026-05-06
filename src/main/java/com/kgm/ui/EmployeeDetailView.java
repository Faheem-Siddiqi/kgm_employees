package com.kgm.ui;

import com.kgm.dao.EmployeeRepositoryDao;
import com.kgm.model.Employee;
import com.kgm.ui.panel.BasicDetailsPanel;
import com.kgm.ui.panel.DocumentViewPanel;
import com.kgm.ui.panel.FooterPanel;
import com.kgm.ui.panel.HeaderPanel;
import com.kgm.ui.panel.OtherDetailsPanel;
import com.kgm.ui.styling.EmployeeDetailViewStyle;

import javax.swing.*;
import java.awt.*;

public class EmployeeDetailView extends JFrame {

    private String empCode;
    private JButton backBtn;
    private JButton updateBtn;

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
                    JOptionPane.ERROR_MESSAGE);
        }

        initializeUI(emp, true);
    }

    public EmployeeDetailView() {
        initializeUI(null, false);
    }

    private void initializeUI(Employee emp, boolean isWithData) {
        EmployeeDetailViewStyle.applyFrame(this);

        JPanel topContainer = EmployeeDetailViewStyle.createTopContainer();
        topContainer.add(new HeaderPanel("Employee Record"), BorderLayout.NORTH);

        JPanel secondRow = EmployeeDetailViewStyle.createSecondRow();
        JPanel left = EmployeeDetailViewStyle.createBackButtonPanel();

        backBtn = new JButton("Back");
        EmployeeDetailViewStyle.styleBackButton(backBtn);
        backBtn.addActionListener(e -> {
            this.dispose();
            new HomeView();
        });
        left.add(backBtn);

        JPanel right = EmployeeDetailViewStyle.createEmployeeSummaryPanel();
        String nameValue = (emp != null) ? emp.getEMP_NAME() : "";
        String codeValue = (emp != null) ? emp.getEMPLOYEE_CODE() : "";

        JLabel name = new JLabel(nameValue);
        EmployeeDetailViewStyle.styleEmployeeName(name);

        JLabel code = new JLabel("Code: " + codeValue);
        EmployeeDetailViewStyle.styleEmployeeCode(code);

        right.add(name);
        right.add(Box.createVerticalStrut(2));
        right.add(code);

        secondRow.add(left, BorderLayout.WEST);
        secondRow.add(right, BorderLayout.EAST);
        topContainer.add(secondRow, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerWrapper = EmployeeDetailViewStyle.createCenterWrapper();
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

        JPanel footerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        updateBtn = new JButton("Update");
        EmployeeDetailViewStyle.styleUpdateButton(updateBtn);
        footerActions.add(updateBtn);
        add(new FooterPanel(footerActions), BorderLayout.SOUTH);

        Runnable refreshButtonState = () -> {
            boolean canUpdate = false;

            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component comp = tabs.getComponentAt(i);

                if (comp instanceof BasicDetailsPanel bp && panelHasEditableFields(bp)) {
                    canUpdate = true;
                    break;
                }

                if (comp instanceof OtherDetailsPanel op && panelHasEditableFields(op)) {
                    canUpdate = true;
                    break;
                }
            }

            updateBtn.setEnabled(canUpdate);
        };

        tabs.addChangeListener(e -> refreshButtonState.run());
        refreshButtonState.run();

        updateBtn.addActionListener(e -> {
            try {
                BasicDetailsPanel basicPanel = null;
                OtherDetailsPanel otherPanel = null;

                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component comp = tabs.getComponentAt(i);

                    if (comp instanceof BasicDetailsPanel bp) {
                        basicPanel = bp;
                    }

                    if (comp instanceof OtherDetailsPanel op) {
                        otherPanel = op;
                    }
                }

                EmployeeRepositoryDao dao = new EmployeeRepositoryDao();
                boolean updatedAny = false;

                if (basicPanel != null && panelHasEditableFields(basicPanel)) {
                    Employee updatedBasic = basicPanel.getEmployeeFromForm();
                    updatedBasic.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(updatedBasic);
                    updatedAny = true;
                }

                if (otherPanel != null && panelHasEditableFields(otherPanel)) {
                    Employee updatedOther = otherPanel.getUpdatedOtherDetails();
                    updatedOther.setEMPLOYEE_CODE(empCode);
                    dao.updateEmployeeDynamic(updatedOther);
                    updatedAny = true;
                }

                if (!updatedAny) {
                    JOptionPane.showMessageDialog(this, "No editable fields found");
                    return;
                }

                JOptionPane.showMessageDialog(this, "Updated successfully");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Update failed");
            }
        });
        setVisible(true);
    }

    private boolean panelHasEditableFields(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextField tf && tf.isEditable()) {
                return true;
            }

            if (comp instanceof JTextArea ta && ta.isEditable()) {
                return true;
            }

            if (comp instanceof JComboBox<?> cb && cb.isEnabled()) {
                return true;
            }

            if (comp instanceof JSpinner sp && sp.isEnabled()) {
                return true;
            }

            if (comp instanceof Container child && panelHasEditableFields(child)) {
                return true;
            }
        }

        return false;
    }
}
