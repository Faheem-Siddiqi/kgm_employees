package com.kgm.ui.styling;

import com.kgm.util.EmployeeDocumentUtil;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

public final class EmployeeDocumentUploadPanelHelper {
    private static final int SEARCH_CONTROL_HEIGHT = 36;
    private static final int TOP_PANEL_BOTTOM_MARGIN = 10;

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color MUTED_BACKGROUND = new Color(248, 250, 252);
    private static final Color PRIMARY = new Color(22, 163, 74);
    private static final Color PRIMARY_HOVER = new Color(21, 128, 61);
    private static final Color PRIMARY_PRESSED = new Color(22, 101, 52);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color PRIMARY_BLUE_HOVER = new Color(29, 78, 216);
    private static final Color PRIMARY_BLUE_PRESSED = new Color(30, 64, 175);
    private static final Color CLEAR_RED = new Color(185, 28, 28);
    private static final Color FIELD_BORDER = new Color(203, 213, 225);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color TEXT_PRIMARY = new Color(2, 8, 23);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color CELL_DIVIDER = new Color(226, 232, 240);
    private static final Color ROW_SELECTION = new Color(229, 242, 255);

    private EmployeeDocumentUploadPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        panel.setOpaque(true);
        panel.setBorder(new EmptyBorder(14, 28, 15, 28));
    }

    public static JPanel createTopPanel() {
        JPanel panel = new RoundedPanel(CARD_BACKGROUND, 8, TOP_PANEL_BOTTOM_MARGIN);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(0, 0, TOP_PANEL_BOTTOM_MARGIN, 0),
                new CompoundBorder(
                        new RoundedBorder(8, BORDER),
                        new EmptyBorder(16, 16, 16, 16)
                )
        ));
        return panel;
    }

    public static JLabel createUploadedCountLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JPanel createSummaryPanel(JLabel primaryLabel, JLabel secondaryLabel, JButton actionButton) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        summary.setOpaque(false);
        summary.add(primaryLabel);
        if (secondaryLabel != null) {
            summary.add(createTextDivider());
            summary.add(secondaryLabel);
        }

        row.add(summary, BorderLayout.CENTER);
        if (actionButton != null) {
            row.add(actionButton, BorderLayout.EAST);
        }
        return row;
    }

    public static JPanel createSearchPanel(JTextField searchField, JButton clearButton, JButton actionButton) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(560, SEARCH_CONTROL_HEIGHT));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, SEARCH_CONTROL_HEIGHT));

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(PAGE_BACKGROUND);
        searchBox.setBorder(new CompoundBorder(
                new RoundedBorder(8, FIELD_BORDER),
                new EmptyBorder(0, 10, 0, 4)
        ));
        JLabel searchIcon = new JLabel(new SearchIcon(TEXT_SECONDARY));
        searchIcon.setBorder(new EmptyBorder(0, 0, 0, 8));
        searchBox.add(searchIcon, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);
        searchBox.add(clearButton, BorderLayout.EAST);

        row.add(searchBox, BorderLayout.CENTER);
        if (actionButton != null) {
            row.add(actionButton, BorderLayout.EAST);
        }
        return row;
    }

    public static JPanel createBulkActionPanel(JButton uploadAllButton) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBackground(PAGE_BACKGROUND);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.add(uploadAllButton);
        return row;
    }

    public static void styleSearchField(JTextField field) {
        field.setBorder(null);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(PAGE_BACKGROUND);
        field.setPreferredSize(new Dimension(240, 34));
    }

    public static void styleSearchButton(JButton button) {
        styleFilledCtaButton(button, new Dimension(96, SEARCH_CONTROL_HEIGHT), PRIMARY_BLUE, PRIMARY_BLUE_HOVER, PRIMARY_BLUE_PRESSED);
    }

    public static void styleTextCtaButton(JButton button) {
        styleFilledCtaButton(button, new Dimension(112, SEARCH_CONTROL_HEIGHT), PRIMARY, PRIMARY_HOVER, PRIMARY_PRESSED);
    }

    public static void styleClearButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setForeground(CLEAR_RED);
        button.setBorder(new EmptyBorder(7, 8, 7, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
        ButtonStateHelper.setEnabledForeground(button, CLEAR_RED);
        updateClearButtonState(button, false);
    }

    public static void updateClearButtonState(JButton button, boolean enabled) {
        ButtonStateHelper.setEnabled(button, enabled);
    }

    public static JLabel createSizeLabel() {
        return new PillLabel(
                "Max size: " + EmployeeDocumentUtil.maxUploadSizeLabel() + " per file",
                MUTED_BACKGROUND,
                BORDER,
                TEXT_SECONDARY
        );
    }

    public static JPanel createRendererPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        return panel;
    }

    public static void styleRendererPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());
        panel.setOpaque(true);
    }

    public static JPanel createEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        return panel;
    }

    public static void styleActionCell(JPanel panel, boolean selected) {
        panel.setBackground(selected ? ROW_SELECTION : PAGE_BACKGROUND);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 1, CELL_DIVIDER),
                new EmptyBorder(0, 4, 0, 4)
        ));
    }

    public static JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel.setOpaque(false);
        return panel;
    }

    public static GridBagConstraints actionCellConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        return gbc;
    }

    public static JButton createActionLink(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(TEXT_SECONDARY);
        button.setBorder(new EmptyBorder(6, 8, 6, 8));
        ButtonStateHelper.install(button);
        return button;
    }

    public static void styleViewLink(JButton button, boolean uploaded) {
        styleActionLink(button, uploaded);
    }

    public static void styleActionLink(JButton button, boolean enabled) {
        ButtonStateHelper.setEnabledForeground(button, enabled ? PRIMARY_BLUE : TEXT_SECONDARY);
        ButtonStateHelper.setEnabled(button, enabled);
    }

    public static void stylePreviewFrame(JFrame frame, Component relativeTo) {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int availableWidth = bounds != null && bounds.width > 0
                ? bounds.width
                : Toolkit.getDefaultToolkit().getScreenSize().width;
        int availableHeight = bounds != null && bounds.height > 0
                ? bounds.height
                : Toolkit.getDefaultToolkit().getScreenSize().height;
        int width = Math.min(1040, Math.max(680, Math.round(availableWidth * 0.78f)));
        int height = Math.min(760, Math.max(500, Math.round(availableHeight * 0.78f)));
        width = Math.max(520, Math.min(width, availableWidth - 48));
        height = Math.max(360, Math.min(height, availableHeight - 64));

        frame.setResizable(true);
        frame.setSize(new Dimension(width, height));
        frame.setMinimumSize(new Dimension(Math.min(520, width), Math.min(360, height)));
        frame.setLocationByPlatform(false);
        Window owner = relativeTo == null ? null : SwingUtilities.getWindowAncestor(relativeTo);
        frame.setLocationRelativeTo(owner == null ? relativeTo : owner);
    }

    private static JLabel createTextDivider() {
        JLabel divider = new JLabel("|");
        divider.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        divider.setForeground(BORDER);
        return divider;
    }

    private static void styleFilledCtaButton(
            JButton button,
            Dimension size,
            Color background,
            Color hoverBackground,
            Color pressedBackground
    ) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(7, 13, 7, 13));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.installRounded(button, 8);
        ButtonStateHelper.setHoverBackground(button, hoverBackground, pressedBackground);
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color color;
        private final int radius;
        private final int bottomGap;

        private RoundedPanel(Color color, int radius, int bottomGap) {
            this.color = color;
            this.radius = radius;
            this.bottomGap = bottomGap;
            setOpaque(false);
        }

        protected void paintComponent(Graphics graphics) {
            int paintedHeight = Math.max(0, getHeight() - bottomGap);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), paintedHeight, radius, radius);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class PillLabel extends JLabel {
        private final Color background;
        private final Color border;

        private PillLabel(String text, Color background, Color border, Color foreground) {
            super(text);
            this.background = background;
            this.border = border;
            setOpaque(false);
            setForeground(foreground);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setBorder(new EmptyBorder(4, 9, 4, 9));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class SearchIcon implements Icon {
        private final Color color;

        private SearchIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 1, y + 1, 10, 10);
            g2.drawLine(x + 10, y + 10, x + 15, y + 15);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}

