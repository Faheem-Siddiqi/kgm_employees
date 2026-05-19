package com.kgm.ui.styling;

import com.kgm.ui.dialog.UniversalDialog;

import javax.swing.*;
import java.awt.*;

public final class UniversalDialogHelper {
    public static final Color INFO_ACCENT = TableStyleHelper.PRIMARY;
    public static final Color SUCCESS_ACCENT = new Color(28, 137, 85);
    public static final Color WARNING_ACCENT = new Color(176, 76, 19);
    public static final Color ERROR_ACCENT = new Color(217, 45, 32);

    public static final int BODY_WIDTH = 500;
    public static final int MESSAGE_BOX_WIDTH = 456;
    public static final int MESSAGE_TEXT_WIDTH = 400;

    private static final Color FOOTER = new Color(247, 249, 251);
    private static final Color ERROR_SURFACE = new Color(255, 246, 245);
    private static final Color ERROR_BORDER = new Color(254, 205, 202);

    private UniversalDialogHelper() {
    }

    public static void styleRoot(JPanel root) {
        root.setBackground(Color.WHITE);
    }

    public static JPanel createHeader(String title, Color accent) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel titleLabel = new JLabel(title == null || title.isBlank() ? "Message" : title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);
        return header;
    }

    public static JPanel createBodyPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        return panel;
    }

    public static JScrollPane createBodyScroll(JPanel panel, int preferredHeight) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(BODY_WIDTH, preferredHeight));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    public static JPanel createMessageRow(UniversalDialog.Type type) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(surface(type));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(type)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        return row;
    }

    public static JLabel createBadge(UniversalDialog.Type type, Color accent) {
        JLabel badge = new JLabel(badgeText(type));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setVerticalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(24, 24));
        badge.setOpaque(true);
        badge.setBackground(accent);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return badge;
    }

    public static JPanel createMessageTextPanel() {
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        return textPanel;
    }

    public static JLabel createHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setForeground(TableStyleHelper.TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return heading;
    }

    public static JTextArea createMessageText(String text, Color background, int rows) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setRows(rows);
        textArea.setColumns(0);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textArea.setForeground(TableStyleHelper.TEXT_PRIMARY);
        textArea.setBackground(background);
        textArea.setBorder(BorderFactory.createEmptyBorder());
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        return textArea;
    }

    public static JScrollPane createSectionScroll(JTextArea text, Color background, Dimension size) {
        JScrollPane sectionScroll = new JScrollPane(text);
        sectionScroll.setBorder(BorderFactory.createLineBorder(TableStyleHelper.BORDER));
        sectionScroll.getViewport().setBackground(background);
        sectionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sectionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        setFixedSize(sectionScroll, size);
        sectionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sectionScroll;
    }

    public static JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(FOOTER);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        return footer;
    }

    public static JButton primaryButton(String text, Color accent) {
        JButton button = new JButton(buttonText(text));
        button.setPreferredSize(buttonSize(button.getText(), 92, 34));
        button.setBackground(accent);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(buttonText(text));
        button.setPreferredSize(buttonSize(button.getText(), 82, 30));
        button.setBackground(Color.WHITE);
        button.setForeground(TableStyleHelper.TEXT_SECONDARY);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(TableStyleHelper.BORDER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void setFixedSize(JComponent component, Dimension size) {
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    public static Color surface(UniversalDialog.Type type) {
        return type == UniversalDialog.Type.ERROR ? ERROR_SURFACE : Color.WHITE;
    }

    public static Color border(UniversalDialog.Type type) {
        return type == UniversalDialog.Type.ERROR ? ERROR_BORDER : TableStyleHelper.BORDER;
    }

    private static String badgeText(UniversalDialog.Type type) {
        return switch (type) {
            case SUCCESS -> "OK";
            case WARNING, ERROR -> "!";
            case INFO -> "i";
        };
    }

    private static Dimension buttonSize(String text, int minimumWidth, int padding) {
        return new Dimension(Math.max(minimumWidth, text.length() * 9 + padding), 34);
    }

    private static String buttonText(String text) {
        return text == null || text.isBlank() ? "OK" : text;
    }
}
