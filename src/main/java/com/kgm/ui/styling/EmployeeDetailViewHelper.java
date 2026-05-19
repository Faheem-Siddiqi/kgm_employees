package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;

public final class EmployeeDetailViewHelper {
    private EmployeeDetailViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        EmployeeDetailViewStyle.applyFrame(frame);
    }

    public static JPanel createTopContainer() {
        return EmployeeDetailViewStyle.createTopContainer();
    }

    public static JPanel createSecondRow() {
        return EmployeeDetailViewStyle.createSecondRow();
    }

    public static JPanel createBackButtonPanel() {
        return EmployeeDetailViewStyle.createBackButtonPanel();
    }

    public static void styleBackButton(JButton button) {
        EmployeeDetailViewStyle.styleBackButton(button);
    }

    public static JPanel createEmployeeSummaryPanel() {
        return EmployeeDetailViewStyle.createEmployeeSummaryPanel();
    }

    public static void styleEmployeeName(JLabel label) {
        EmployeeDetailViewStyle.styleEmployeeName(label);
    }

    public static void styleEmployeeCode(JLabel label) {
        EmployeeDetailViewStyle.styleEmployeeCode(label);
    }

    public static JPanel createCenterWrapper() {
        return EmployeeInductionHelper.createCenterWrapper();
    }

    public static void styleUpdateButton(JButton button) {
        EmployeeInductionHelper.stylePrimaryButton(button);
    }

    public static JScrollPane createPageScrollPane(JComponent content) {
        return EmployeeInductionHelper.createPageScrollPane(content);
    }

    public static void installPageWheelForwarding(JScrollPane pageScroll, JComponent root) {
        EmployeeInductionHelper.installPageWheelForwarding(pageScroll, root);
    }

    public static GridBagConstraints pageConstraints(int y) {
        return EmployeeInductionHelper.pageConstraints(y);
    }

    public static JPanel createTabContent(JComponent content, JComponent actions) {
        return EmployeeInductionHelper.createTabContent(content, actions);
    }

    /**
     * Apply custom tab styling matching HomeViewHelper's tab UI.
     * This creates modern tabs with underline indicator for selected tab.
     */
    public static void styleTabs(JTabbedPane tabs) {
        EmployeeInductionHelper.styleTabs(tabs);
    }
}
