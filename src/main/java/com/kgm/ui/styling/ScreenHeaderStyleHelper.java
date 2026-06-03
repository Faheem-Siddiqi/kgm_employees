package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class ScreenHeaderStyleHelper {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color PRIMARY = TableThemeHelper.PRIMARY;
    private static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color PRIMARY_PRESSED = new Color(30, 64, 175);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color SUCCESS_HOVER = new Color(21, 128, 61);
    private static final Color SUCCESS_PRESSED = new Color(22, 101, 52);
    private static final int HEADER_CTA_GAP = 10;

    private ScreenHeaderStyleHelper() {
    }

    public static JPanel screenHeader(String titleText, String subtitleText, Runnable onDashboard) {
        return screenHeader(titleText, subtitleText, onDashboard, null);
    }

    public static JPanel screenHeader(
            String titleText,
            String subtitleText,
            Runnable onDashboard,
            Runnable onDownloadProfile
    ) {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(PAGE_BACKGROUND);
        header.setBorder(new EmptyBorder(25, 28, 18, 28));

        JPanel titleBlock = createTitleBlock(titleText, subtitleText);
        titleBlock.setMinimumSize(new Dimension(0, titleBlock.getPreferredSize().height));

        JPanel actionGroup = new JPanel();
        actionGroup.setLayout(new BoxLayout(actionGroup, BoxLayout.X_AXIS));
        actionGroup.setOpaque(false);
        if (onDownloadProfile != null) {
            actionGroup.add(downloadProfileButton(onDownloadProfile));
            actionGroup.add(Box.createHorizontalStrut(HEADER_CTA_GAP));
        }
        if (onDashboard != null) {
            actionGroup.add(dashboardButton(onDashboard));
        }

        GridBagConstraints titleGbc = new GridBagConstraints();
        titleGbc.gridx = 0;
        titleGbc.gridy = 0;
        titleGbc.weightx = 1.0;
        titleGbc.fill = GridBagConstraints.HORIZONTAL;
        titleGbc.anchor = GridBagConstraints.WEST;
        header.add(titleBlock, titleGbc);

        if (actionGroup.getComponentCount() > 0) {
            GridBagConstraints actionGbc = new GridBagConstraints();
            actionGbc.gridx = 1;
            actionGbc.gridy = 0;
            actionGbc.weightx = 0;
            actionGbc.anchor = GridBagConstraints.EAST;
            actionGbc.insets = new Insets(0, 24, 0, 0);
            header.add(actionGroup, actionGbc);
        }
        return header;
    }

    public static JButton dashboardButton(Runnable action) {
        JButton button = new JButton("Dashboard");
        styleDashboardButton(button);
        if (action != null) {
            button.addActionListener(event -> action.run());
        }
        return button;
    }

    public static JButton downloadProfileButton(Runnable action) {
        JButton button = new JButton("Download Profile");
        styleDownloadProfileButton(button);
        if (action != null) {
            button.addActionListener(event -> action.run());
        }
        return button;
    }

    public static JButton returnToTopButton(Runnable action) {
        JButton button = new JButton("Return to top");
        stylePrimaryTextButton(button);
        if (action != null) {
            button.addActionListener(event -> action.run());
        }
        return button;
    }

    public static void styleDashboardButton(JButton button) {
        styleHeaderCtaButton(button, new Dimension(116, 36), PRIMARY, PRIMARY_HOVER, PRIMARY_PRESSED);
    }

    public static void styleDownloadProfileButton(JButton button) {
        styleHeaderCtaButton(button, new Dimension(156, 36), SUCCESS, SUCCESS_HOVER, SUCCESS_PRESSED);
    }

    private static void styleHeaderCtaButton(
            JButton button,
            Dimension size,
            Color background,
            Color hoverBackground,
            Color pressedBackground
    ) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(7, 14, 7, 14));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setMinimumSize(size);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setVerticalAlignment(SwingConstants.CENTER);
        ButtonStateHelper.installRounded(button, 8);
        ButtonStateHelper.setHoverBackground(button, hoverBackground, pressedBackground);
    }

    public static void stylePrimaryTextButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(PRIMARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(6, 4, 6, 4));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setForeground(PRIMARY_HOVER);
                }
            }

            public void mouseExited(MouseEvent event) {
                button.setForeground(PRIMARY);
            }
        });
    }

    private static JPanel createTitleBlock(String titleText, String subtitleText) {
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(PAGE_BACKGROUND);

        JLabel title = new JLabel(cleanText(titleText, "Employee Record"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(cleanText(subtitleText, " "));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);
        return titleBlock;
    }

    private static String cleanText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

}
