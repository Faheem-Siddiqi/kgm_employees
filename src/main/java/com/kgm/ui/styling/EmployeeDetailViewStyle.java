package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class EmployeeDetailViewStyle {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color NAVY = new Color(0, 38, 77);
    private static final Color LINK_BLUE = new Color(0, 102, 204);
    private static final Color EMPLOYEE_NAME = new Color(100, 100, 100);
    private static final Color EMPLOYEE_CODE = new Color(90, 90, 90);

    private EmployeeDetailViewStyle() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("Employee Form");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    public static JPanel createTopContainer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel createSecondRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(PAGE_BACKGROUND);
        row.setBorder(new EmptyBorder(10, 20, 0, 16));
        return row;
    }

    public static JPanel createBackButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static void styleBackButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(LINK_BLUE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
    }

    public static JPanel createEmployeeSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PAGE_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));
        return panel;
    }

    public static void styleEmployeeName(JLabel label) {
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        label.setForeground(EMPLOYEE_NAME);
        label.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    public static void styleEmployeeCode(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(EMPLOYEE_CODE);
        label.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    public static JPanel createCenterWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        wrapper.setBackground(PAGE_BACKGROUND);
        return wrapper;
    }

    public static JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(PAGE_BACKGROUND);
        return footer;
    }

    public static void styleUpdateButton(JButton button) {
        button.setPreferredSize(new Dimension(110, 32));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setBackground(NAVY);
    }
}
