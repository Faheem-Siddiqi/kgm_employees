package com.kgm.ui.styling;

import com.kgm.ui.dialog.UniversalDialog;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UniversalDialogHelper {
    public static Color PRIMARY = new Color(37, 99, 235);
    public static String FONT_FAMILY = Font.SANS_SERIF;

    public static Color INFO_ACCENT = PRIMARY;
    public static Color SUCCESS_ACCENT = new Color(22, 163, 74);
    public static Color WARNING_ACCENT = new Color(217, 119, 6);
    public static Color ERROR_ACCENT = new Color(220, 38, 38);

    private static final int MAX_DIALOG_WIDTH = 560;
    private static final int SCREEN_GUTTER = 48;
    private static final int BODY_HORIZONTAL_PADDING = 24;
    private static final int BODY_TOP_PADDING = 16;
    private static final int BODY_BOTTOM_PADDING = 20;
    private static final int ALERT_ICON_SIZE = 18;
    private static final int ALERT_GAP = 12;
    private static final int ALERT_HORIZONTAL_PADDING = 16;
    private static final int ALERT_VERTICAL_PADDING = 15;
    private static final int SECTION_SCROLL_PADDING_X = 12;
    private static final int SECTION_SCROLL_PADDING_Y = 10;

    public static final Color BACKGROUND = Color.WHITE;
    public static final Color SURFACE = new Color(248, 250, 252);
    public static final Color CARD_BORDER = new Color(229, 231, 235);
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    public static final Color TEXT_SECONDARY = new Color(71, 85, 105);
    public static final Color MUTED_TEXT = new Color(100, 116, 139);
    public static final Color CLOSE_HOVER = new Color(241, 245, 249);
    private static final Color INFO_SURFACE = new Color(248, 250, 252);
    private static final Color SUCCESS_SURFACE = new Color(240, 253, 244);
    private static final Color WARNING_SURFACE = new Color(255, 251, 235);
    private static final Color ERROR_SURFACE = new Color(254, 242, 242);
    private static final Color INFO_BORDER = new Color(191, 219, 254);
    private static final Color SUCCESS_BORDER = new Color(187, 247, 208);
    private static final Color WARNING_BORDER = new Color(253, 230, 138);
    private static final Color ERROR_BORDER = new Color(254, 202, 202);

    private UniversalDialogHelper() {
    }

    public static void styleRoot(JPanel root) {
        root.setOpaque(true);
        root.setBackground(BACKGROUND);
        root.setBorder(roundedBorder(CARD_BORDER, 8, 1));
    }

    public static Color accentFor(UniversalDialog.Type type) {
        return switch (type) {
            case INFO -> PRIMARY;
            case SUCCESS -> SUCCESS_ACCENT;
            case WARNING -> WARNING_ACCENT;
            case ERROR -> ERROR_ACCENT;
        };
    }

    public static Font regularFont(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font mediumFont(int size) {
        return new Font(FONT_FAMILY, Font.BOLD, size);
    }

    public static Border roundedBorder(Color color, int radius, int thickness) {
        return new RoundedLineBorder(radius, color, thickness);
    }

    public static void styleDialogWindow(JDialog dialog) {
        dialog.setUndecorated(true);
        dialog.getRootPane().setOpaque(true);
        dialog.getRootPane().setBackground(BACKGROUND);
        dialog.setBackground(BACKGROUND);
    }

    public static int dialogWidth(Window owner) {
        Rectangle bounds = screenBounds(owner);
        int available = Math.max(300, bounds.width - SCREEN_GUTTER);
        return Math.min(MAX_DIALOG_WIDTH, available);
    }

    public static int maxBodyHeight(Window owner) {
        Rectangle bounds = screenBounds(owner);
        int available = Math.max(180, bounds.height - 240);
        return Math.min(520, available);
    }

    public static int alertWidth(int bodyWidth) {
        return Math.max(220, bodyWidth - BODY_HORIZONTAL_PADDING * 2);
    }

    public static int messageTextWidth(int alertWidth) {
        int padding = ALERT_HORIZONTAL_PADDING * 2 + ALERT_ICON_SIZE + ALERT_GAP + 2;
        return Math.max(140, alertWidth - padding);
    }

    public static int wrapColumns(int textWidth) {
        return Math.max(20, textWidth / 8);
    }

    public static int alertHeight(int textHeight, int headingHeight) {
        return textHeight + headingHeight + ALERT_VERTICAL_PADDING * 2 + 2;
    }

    public static int bodyHeightWithPadding(int contentHeight) {
        return contentHeight + BODY_TOP_PADDING + BODY_BOTTOM_PADDING;
    }

    public static int sectionScrollHeight(int textHeight) {
        return textHeight + SECTION_SCROLL_PADDING_Y * 2 + 2;
    }

    public static JPanel createHeader(UniversalDialog.Type type, String title, Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(24, 24, 6, 20));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(displayTitle(type, title));
        titleLabel.setFont(mediumFont(18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(statusText(type));
        description.setFont(regularFont(13));
        description.setForeground(MUTED_TEXT);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        copy.add(titleLabel);
        copy.add(Box.createVerticalStrut(4));
        copy.add(description);

        header.add(copy, BorderLayout.CENTER);

        JButton close = closeButton();
        close.addActionListener(event -> onClose.run());
        header.add(close, BorderLayout.EAST);
        return header;
    }

    public static JPanel createBodyPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(BODY_TOP_PADDING, BODY_HORIZONTAL_PADDING, BODY_BOTTOM_PADDING, BODY_HORIZONTAL_PADDING));
        return panel;
    }

    public static JScrollPane createBodyScroll(JPanel panel, int preferredHeight, int bodyWidth, int maxHeight) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setOpaque(true);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.setBackground(BACKGROUND);
        scroll.setPreferredSize(new Dimension(bodyWidth, clamp(preferredHeight, 112, maxHeight)));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }

    public static JPanel createMessageRow(UniversalDialog.Type type) {
        JPanel row = new JPanel(new BorderLayout(ALERT_GAP, 0));
        row.setOpaque(true);
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(8, border(type), 1),
                new EmptyBorder(ALERT_VERTICAL_PADDING, ALERT_HORIZONTAL_PADDING, ALERT_VERTICAL_PADDING, ALERT_HORIZONTAL_PADDING)));
        row.setBackground(surface(type));
        return row;
    }

    public static JLabel createBadge(UniversalDialog.Type type, Color accent) {
        StatusIcon badge = new StatusIcon(type, accent, ALERT_ICON_SIZE);
        Dimension size = new Dimension(ALERT_ICON_SIZE, ALERT_ICON_SIZE);
        badge.setPreferredSize(size);
        badge.setMinimumSize(size);
        badge.setMaximumSize(size);
        badge.setAlignmentY(Component.TOP_ALIGNMENT);
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
        heading.setFont(mediumFont(14));
        heading.setForeground(TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return heading;
    }

    public static JTextArea createMessageText(String text, Color background, int rows) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setRows(rows);
        textArea.setColumns(0);
        textArea.setFont(regularFont(14));
        textArea.setForeground(TEXT_SECONDARY);
        textArea.setBackground(background);
        textArea.setBorder(BorderFactory.createEmptyBorder());
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    public static JScrollPane createSectionScroll(JTextArea text, Color background, Dimension size) {
        JScrollPane sectionScroll = new JScrollPane(text);
        sectionScroll.setBorder(new RoundedLineBorder(8, borderColor(background), 1));
        sectionScroll.getViewport().setBackground(background);
        sectionScroll.setOpaque(true);
        sectionScroll.getViewport().setOpaque(true);
        sectionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sectionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        text.setBorder(new EmptyBorder(
                SECTION_SCROLL_PADDING_Y,
                SECTION_SCROLL_PADDING_X,
                SECTION_SCROLL_PADDING_Y,
                SECTION_SCROLL_PADDING_X
        ));
        setFixedSize(sectionScroll, size);
        sectionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sectionScroll;
    }

    public static JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(true);
        footer.setBackground(BACKGROUND);
        footer.setBorder(new EmptyBorder(0, 24, 24, 24));
        return footer;
    }

    public static JPanel createDialogCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                roundedBorder(CARD_BORDER, 8, 1),
                new EmptyBorder(18, 18, 18, 18)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    public static JPanel createDialogSectionHeader(String titleText, String helperText) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(titleText);
        title.setFont(mediumFont(15));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel helper = new JLabel(htmlWrap(helperText));
        helper.setFont(regularFont(13));
        helper.setForeground(MUTED_TEXT);
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(helper);
        return header;
    }

    public static JPanel createDialogTextStack() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    public static JLabel createDialogHelperLabel(String text) {
        JLabel helper = new JLabel(htmlWrap(text));
        helper.setFont(regularFont(13));
        helper.setForeground(MUTED_TEXT);
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return helper;
    }

    public static JPanel createDialogRow(JComponent left, JComponent right) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setOpaque(true);
        row.setBackground(SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                roundedBorder(CARD_BORDER, 8, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(left, BorderLayout.CENTER);
        if (right != null) {
            row.add(right, BorderLayout.EAST);
        }
        return row;
    }

    public static JPanel createInfoBox(String titleText, String detailText, Color background, Color foreground) {
        JPanel box = new JPanel(new BorderLayout(10, 0));
        box.setOpaque(true);
        box.setBackground(background);
        box.setBorder(new EmptyBorder(12, 14, 12, 14));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel marker = new JLabel("i", SwingConstants.CENTER);
        marker.setFont(mediumFont(13));
        marker.setForeground(foreground);
        marker.setPreferredSize(new Dimension(18, 18));
        box.add(marker, BorderLayout.WEST);

        JPanel text = createDialogTextStack();
        JLabel title = new JLabel(titleText);
        title.setFont(mediumFont(13));
        title.setForeground(foreground);
        JLabel desc = new JLabel(htmlWrap(detailText));
        desc.setFont(regularFont(13));
        desc.setForeground(foreground);

        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(desc);
        box.add(text, BorderLayout.CENTER);

        box.putClientProperty("titleLabel", title);
        box.putClientProperty("descriptionLabel", desc);
        return box;
    }

    public static JLabel createPill(String text, Color background, Color foreground) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(background);
        badge.setForeground(foreground);
        badge.setFont(mediumFont(11));
        badge.setBorder(new EmptyBorder(4, 9, 4, 9));
        return badge;
    }

    public static void styleDialogScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 9));
    }

    public static void resizeLargeDialog(JDialog dialog, Window owner) {
        Rectangle bounds = screenBounds(owner);
        int availableWidth = Math.max(320, bounds.width - 64);
        int availableHeight = Math.max(300, bounds.height - 80);
        int width = Math.min(760, Math.max(360, (int) (bounds.width * 0.52)));
        int height = Math.min(680, Math.max(420, (int) (bounds.height * 0.74)));
        width = Math.min(width, availableWidth);
        height = Math.min(height, availableHeight);
        dialog.setMinimumSize(new Dimension(Math.min(340, width), Math.min(300, height)));
        dialog.setPreferredSize(new Dimension(width, height));
        dialog.setSize(new Dimension(width, height));
    }

    public static String htmlWrap(String text) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(500, Math.max(250, screen.width - 220));
        return "<html><body style='width:" + width + "px; margin:0'>" + text + "</body></html>";
    }

    public static JButton primaryButton(String text, Color accent) {
        JButton button = new JButton(buttonText(text));
        button.setPreferredSize(buttonSize(button.getText(), 92, 36));
        button.setBackground(actionBackground(accent));
        button.setForeground(Color.WHITE);
        button.setFont(mediumFont(13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 15, 8, 15));
        ButtonStateHelper.installRounded(button, 6);
        ButtonStateHelper.setHoverBackground(button, actionHover(accent), actionPressed(accent));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(buttonText(text));
        button.setPreferredSize(buttonSize(button.getText(), 86, 36));
        button.setBackground(BACKGROUND);
        button.setForeground(TEXT_PRIMARY);
        button.setFont(mediumFont(13));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new RoundedLineBorder(6, CARD_BORDER, 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.installRounded(button, 6);
        ButtonStateHelper.setHoverBackground(button, CLOSE_HOVER, new Color(226, 232, 240));
        return button;
    }

    public static void setFixedSize(JComponent component, Dimension size) {
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    public static Color surface(UniversalDialog.Type type) {
        return switch (type) {
            case INFO -> INFO_SURFACE;
            case SUCCESS -> SUCCESS_SURFACE;
            case WARNING -> WARNING_SURFACE;
            case ERROR -> ERROR_SURFACE;
        };
    }

    public static Color border(UniversalDialog.Type type) {
        return switch (type) {
            case INFO -> INFO_BORDER;
            case SUCCESS -> SUCCESS_BORDER;
            case WARNING -> WARNING_BORDER;
            case ERROR -> ERROR_BORDER;
        };
    }

    public static String displayTitle(UniversalDialog.Type type, String title) {
        String value = title == null || title.isBlank() ? "" : title.trim();
        if (value.isEmpty() || "Message".equalsIgnoreCase(value) || "Info".equalsIgnoreCase(value)) {
            return defaultTitle(type);
        }
        if ("Success".equalsIgnoreCase(value)) {
            return "All set";
        }
        if ("Warning".equalsIgnoreCase(value)) {
            return "Heads up";
        }
        if ("Error".equalsIgnoreCase(value)) {
            return "Something went wrong";
        }
        return value;
    }

    public static JButton closeButton() {
        JButton close = new JButton("X");
        Dimension size = new Dimension(32, 32);
        close.setPreferredSize(size);
        close.setMinimumSize(size);
        close.setMaximumSize(size);
        close.setToolTipText("Close dialog");
        close.setBackground(BACKGROUND);
        close.setForeground(MUTED_TEXT);
        close.setFont(mediumFont(14));
        close.setFocusPainted(false);
        close.setBorder(new EmptyBorder(0, 0, 1, 0));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.installRounded(close, 6);
        ButtonStateHelper.setHoverBackground(close, CLOSE_HOVER, new Color(226, 232, 240));
        return close;
    }

    private static String defaultTitle(UniversalDialog.Type type) {
        return switch (type) {
            case SUCCESS -> "All set";
            case WARNING -> "Heads up";
            case ERROR -> "Something went wrong";
            case INFO -> "Notice";
        };
    }

    private static String statusText(UniversalDialog.Type type) {
        return switch (type) {
            case SUCCESS -> "Completed successfully";
            case WARNING -> "Needs your attention";
            case ERROR -> "Action could not be completed";
            case INFO -> "Information";
        };
    }

    private static Dimension buttonSize(String text, int minimumWidth, int padding) {
        return new Dimension(Math.max(minimumWidth, text.length() * 8 + padding), 36);
    }

    private static String buttonText(String text) {
        if (text == null || text.isBlank() || "OK".equalsIgnoreCase(text.trim())) {
            return "Okay";
        }
        return text.trim();
    }

    private static Color borderColor(Color background) {
        if (ERROR_SURFACE.equals(background)) {
            return ERROR_BORDER;
        }
        if (WARNING_SURFACE.equals(background)) {
            return WARNING_BORDER;
        }
        if (SUCCESS_SURFACE.equals(background)) {
            return SUCCESS_BORDER;
        }
        if (INFO_SURFACE.equals(background)) {
            return INFO_BORDER;
        }
        return CARD_BORDER;
    }

    private static Color actionBackground(Color accent) {
        if (ERROR_ACCENT.equals(accent)) {
            return ERROR_ACCENT;
        }
        return PRIMARY;
    }

    private static Color actionHover(Color accent) {
        if (ERROR_ACCENT.equals(accent)) {
            return darken(ERROR_ACCENT, 0.08);
        }
        return darken(PRIMARY, 0.08);
    }

    private static Color actionPressed(Color accent) {
        if (ERROR_ACCENT.equals(accent)) {
            return darken(ERROR_ACCENT, 0.14);
        }
        return darken(PRIMARY, 0.14);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Rectangle screenBounds(Window owner) {
        GraphicsConfiguration configuration = owner == null ? null : owner.getGraphicsConfiguration();
        if (configuration == null) {
            configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }
        return configuration.getBounds();
    }

    private static Color darken(Color color, double amount) {
        double multiplier = Math.max(0.0, 1.0 - amount);
        return new Color(
                Math.max(0, (int) Math.round(color.getRed() * multiplier)),
                Math.max(0, (int) Math.round(color.getGreen() * multiplier)),
                Math.max(0, (int) Math.round(color.getBlue() * multiplier)),
                color.getAlpha()
        );
    }

    private static final class StatusIcon extends JLabel {
        private final UniversalDialog.Type type;
        private final Color accent;
        private final int size;

        private StatusIcon(UniversalDialog.Type type, Color accent, int size) {
            this.type = type;
            this.accent = accent;
            this.size = size;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(accent);
            paintGlyph(g2);
            g2.dispose();
        }

        private void paintGlyph(Graphics2D g2) {
            int center = size / 2;
            if (type == UniversalDialog.Type.SUCCESS) {
                g2.drawLine(center - 6, center, center - 2, center + 5);
                g2.drawLine(center - 2, center + 5, center + 7, center - 6);
                return;
            }
            if (type == UniversalDialog.Type.INFO) {
                g2.drawLine(center, center - 1, center, center + 7);
                g2.fillOval(center - 1, center - 8, 3, 3);
                return;
            }
            g2.drawLine(center, center - 7, center, center + 3);
            g2.fillOval(center - 1, center + 7, 3, 3);
        }
    }

    private static class RoundedLineBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        private final int thickness;

        private RoundedLineBorder(int radius, Color color, int thickness) {
            this.radius = radius;
            this.color = color;
            this.thickness = Math.max(1, thickness);
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            int offset = thickness / 2;
            g2.drawRoundRect(
                    x + offset,
                    y + offset,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius
            );
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.top = thickness;
            insets.left = thickness;
            insets.bottom = thickness;
            insets.right = thickness;
            return insets;
        }
    }

}
