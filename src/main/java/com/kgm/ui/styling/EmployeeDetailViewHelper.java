package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;

public final class EmployeeDetailViewHelper {
    private EmployeeDetailViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        EmployeeDetailViewLayoutHelper.applyFrame(frame);
    }

    public static JPanel createTopContainer() {
        return EmployeeDetailViewLayoutHelper.createTopContainer();
    }

    public static JPanel createSecondRow() {
        return EmployeeDetailViewLayoutHelper.createSecondRow();
    }

    public static JPanel screenHeader(String employeeName, String employeeCode, Runnable onBack) {
        return EmployeeDetailViewLayoutHelper.screenHeader(employeeName, employeeCode, onBack);
    }

    public static JPanel screenHeader(
            String employeeName,
            String employeeCode,
            Runnable onBack,
            Runnable onDownloadReport
    ) {
        return EmployeeDetailViewLayoutHelper.screenHeader(employeeName, employeeCode, onBack, onDownloadReport);
    }

    public static JPanel createBackButtonPanel() {
        return EmployeeDetailViewLayoutHelper.createBackButtonPanel();
    }

    public static void styleBackButton(JButton button) {
        EmployeeDetailViewLayoutHelper.styleBackButton(button);
    }

    public static JPanel createEmployeeSummaryPanel() {
        return EmployeeDetailViewLayoutHelper.createEmployeeSummaryPanel();
    }

    public static void styleEmployeeName(JLabel label) {
        EmployeeDetailViewLayoutHelper.styleEmployeeName(label);
    }

    public static void styleEmployeeCode(JLabel label) {
        EmployeeDetailViewLayoutHelper.styleEmployeeCode(label);
    }

    public static JPanel createCenterWrapper() {
        return EmployeeRegistrationViewHelper.createCenterWrapper();
    }

    public static void styleUpdateButton(JButton button) {
        EmployeeRegistrationViewHelper.stylePrimaryButton(button);
    }

    public static JPanel createActionRow() {
        return EmployeeRegistrationViewHelper.createActionRow();
    }

    public static JScrollPane createPageScrollPane(JComponent content) {
        return EmployeeRegistrationViewHelper.createPageScrollPane(content);
    }

    public static void installPageWheelForwarding(JScrollPane pageScroll, JComponent root) {
        EmployeeRegistrationViewHelper.installPageWheelForwarding(pageScroll, root);
    }

    public static GridBagConstraints pageConstraints(int y) {
        return EmployeeRegistrationViewHelper.pageConstraints(y);
    }

    public static JPanel createTabContent(JComponent content, JComponent actions) {
        return EmployeeRegistrationViewHelper.createTabContent(content, actions);
    }

    /**
     * Apply custom tab styling matching HomeViewHelper's tab UI.
     * This creates modern tabs with underline indicator for selected tab.
     */
    public static void styleTabs(JTabbedPane tabs) {
        EmployeeRegistrationViewHelper.styleTabs(tabs);
    }
}

