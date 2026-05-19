package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class EmployeeInductionStyle {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color NAVY = new Color(0, 38, 77);
    private static final Color LINK_BLUE = new Color(0, 102, 204);

    private EmployeeInductionStyle() {
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

    public static JPanel createBackRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.setBackground(PAGE_BACKGROUND);
        return row;
    }

    public static void styleBackButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(LINK_BLUE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
    }

    public static JPanel createCenterWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        wrapper.setOpaque(true);
        wrapper.setBackground(PAGE_BACKGROUND);
        return wrapper;
    }

    public static JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(PAGE_BACKGROUND);
        return footer;
    }

    public static void styleFooterButton(JButton button) {
        button.setPreferredSize(new Dimension(100, 32));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setBackground(NAVY);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }
}
