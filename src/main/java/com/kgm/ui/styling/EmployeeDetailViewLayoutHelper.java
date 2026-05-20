package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class EmployeeDetailViewLayoutHelper {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color NAVY = new Color(0, 38, 77);
    private static final Color LINK_BLUE = new Color(0, 102, 204);
    private static final Color SUCCESS_GREEN = new Color(15, 139, 76);
    private static final Color EMPLOYEE_NAME = new Color(100, 100, 100);
    private static final Color EMPLOYEE_CODE = new Color(90, 90, 90);

    private EmployeeDetailViewLayoutHelper() {
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

    public static JPanel screenHeader(String employeeName, String employeeCode, Runnable onBack) {
        return screenHeader(employeeName, employeeCode, onBack, null);
    }

    public static JPanel screenHeader(
            String employeeName,
            String employeeCode,
            Runnable onBack,
            Runnable onDownloadReport
    ) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BACKGROUND);
        header.setBorder(new EmptyBorder(25, 28, 0, 28));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(PAGE_BACKGROUND);

        JLabel title = new JLabel("Employee Record");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String cleanName = employeeName == null ? "" : employeeName.trim();
        String cleanCode = employeeCode == null ? "" : employeeCode.trim();
        String subtitleText = cleanName.isEmpty() && cleanCode.isEmpty()
                ? "View and update employee details"
                : cleanName + (cleanCode.isEmpty() ? "" : " - Code: " + cleanCode);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(100, 100, 100));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        actionGroup.setOpaque(false);
        if (onDownloadReport != null) {
            JButton downloadReport = createDownloadReportButton();
            downloadReport.addActionListener(e -> onDownloadReport.run());
            actionGroup.add(downloadReport);
        }

        JButton back = new JButton("Dashboard");
        styleBackButton(back);
        back.addActionListener(e -> onBack.run());
        actionGroup.add(back);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(actionGroup, BorderLayout.EAST);
        return header;
    }

    private static JButton createDownloadReportButton() {
        JButton button = new JButton("Download Profile");
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(SUCCESS_GREEN);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        return button;
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

