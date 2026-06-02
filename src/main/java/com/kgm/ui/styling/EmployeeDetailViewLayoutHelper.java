package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class EmployeeDetailViewLayoutHelper {
    private static final int HEADER_CTA_GAP = 10;

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color NAVY = new Color(0, 38, 77);
    private static final Color DASHBOARD_BLUE = new Color(37, 99, 235);
    private static final Color DASHBOARD_BLUE_HOVER = new Color(29, 78, 216);
    private static final Color DASHBOARD_BLUE_PRESSED = new Color(30, 64, 175);
    private static final Color DOWNLOAD_GREEN = new Color(22, 163, 74);
    private static final Color DOWNLOAD_GREEN_HOVER = new Color(21, 128, 61);
    private static final Color DOWNLOAD_GREEN_PRESSED = new Color(22, 101, 52);
    private static final Color EMPLOYEE_NAME = new Color(100, 100, 100);
    private static final Color EMPLOYEE_CODE = new Color(90, 90, 90);
    private static final HeaderButtonStyle DASHBOARD_BUTTON_STYLE = new HeaderButtonStyle(
            DASHBOARD_BLUE,
            DASHBOARD_BLUE_HOVER,
            DASHBOARD_BLUE_PRESSED
    );
    private static final HeaderButtonStyle DOWNLOAD_BUTTON_STYLE = new HeaderButtonStyle(
            DOWNLOAD_GREEN,
            DOWNLOAD_GREEN_HOVER,
            DOWNLOAD_GREEN_PRESSED
    );

    private EmployeeDetailViewLayoutHelper() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("Employee Form");
        AppWindowStateHelper.lockFullSize(frame);
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
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(PAGE_BACKGROUND);
        header.setBorder(new EmptyBorder(25, 28, 18, 28));

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
        titleBlock.setMinimumSize(new Dimension(0, titleBlock.getPreferredSize().height));

        JPanel actionGroup = new JPanel();
        actionGroup.setLayout(new BoxLayout(actionGroup, BoxLayout.X_AXIS));
        actionGroup.setOpaque(false);
        if (onDownloadReport != null) {
            JButton downloadReport = createDownloadReportButton();
            downloadReport.addActionListener(e -> onDownloadReport.run());
            actionGroup.add(downloadReport);
            actionGroup.add(Box.createHorizontalStrut(HEADER_CTA_GAP));
        }

        JButton back = new JButton("Dashboard");
        styleBackButton(back);
        back.addActionListener(e -> onBack.run());
        actionGroup.add(back);

        GridBagConstraints titleGbc = new GridBagConstraints();
        titleGbc.gridx = 0;
        titleGbc.gridy = 0;
        titleGbc.weightx = 1.0;
        titleGbc.fill = GridBagConstraints.HORIZONTAL;
        titleGbc.anchor = GridBagConstraints.WEST;
        header.add(titleBlock, titleGbc);

        GridBagConstraints actionGbc = new GridBagConstraints();
        actionGbc.gridx = 1;
        actionGbc.gridy = 0;
        actionGbc.weightx = 0;
        actionGbc.anchor = GridBagConstraints.EAST;
        actionGbc.insets = new Insets(0, 24, 0, 0);
        header.add(actionGroup, actionGbc);
        return header;
    }

    private static JButton createDownloadReportButton() {
        JButton button = new JButton("Download Profile");
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleHeaderCtaButton(button, new Dimension(156, 36), DOWNLOAD_BUTTON_STYLE);
        return button;
    }

    public static JPanel createBackButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static void styleBackButton(JButton button) {
        styleHeaderCtaButton(button, new Dimension(116, 36), DASHBOARD_BUTTON_STYLE);
    }

    private static void styleHeaderCtaButton(JButton button, Dimension size, HeaderButtonStyle style) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setBackground(style.background());
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(7, 14, 7, 14));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setMinimumSize(size);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setVerticalAlignment(SwingConstants.CENTER);
        ButtonStateHelper.installRounded(button, 8);
        ButtonStateHelper.setHoverBackground(button, style.hoverBackground(), style.pressedBackground());
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

    private record HeaderButtonStyle(Color background, Color hoverBackground, Color pressedBackground) {
    }
}

