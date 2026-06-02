package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class HomeViewHelper {
    private static final int SEARCH_FIELD_WIDTH = 360;
    private static final int SEARCH_BUTTON_WIDTH = 92;
    private static final int SEARCH_CONTROL_WIDTH = SEARCH_FIELD_WIDTH + SEARCH_BUTTON_WIDTH + 10;
    private static final int SEARCH_CONTROL_HEIGHT = 36;
    private static final String SEARCH_CONTROLS = "searchControls";
    private static final String ACTION_CONTROLS = "actionControls";

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY = new Color(0, 112, 210);
    private static final Color ACTION_BLUE = new Color(30, 144, 255);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BORDER = new Color(220, 226, 232);
    private static final Color MENU_SELECTION = new Color(239, 246, 255);
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);

    private HomeViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        frame.setTitle("Home");
        AppWindowStateHelper.lockFullSize(frame);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    public static JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel createSearchRow() {
        return createCommandBar();
    }

    public static JPanel createCommandBar() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(24, 28, 0, 28));

        JPanel searchControls = new JPanel(new GridBagLayout());
        searchControls.setOpaque(false);
        searchControls.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchControls.setMinimumSize(new Dimension(SEARCH_CONTROL_WIDTH, SEARCH_CONTROL_HEIGHT));
        searchControls.setPreferredSize(new Dimension(SEARCH_CONTROL_WIDTH, SEARCH_CONTROL_HEIGHT));

        JPanel actionControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionControls.setOpaque(false);
        actionControls.setMinimumSize(new Dimension(0, SEARCH_CONTROL_HEIGHT));

        JPanel card = new ResponsiveCommandBar(searchControls, actionControls);

        wrapper.putClientProperty(SEARCH_CONTROLS, searchControls);
        wrapper.putClientProperty(ACTION_CONTROLS, actionControls);
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    public static JTextField createSearchField(String placeholder) {
        return new PlaceholderTextField(placeholder);
    }

    public static void addSearchControls(JPanel searchRow, JTextField field, JButton searchButton, JButton clearButton) {
        Object controls = searchRow.getClientProperty(SEARCH_CONTROLS);
        if (controls instanceof JPanel row) {
            styleSearchField(field);

            JPanel searchBox = new JPanel(new BorderLayout(6, 0));
            searchBox.setBackground(PAGE_BACKGROUND);
            Dimension searchSize = new Dimension(SEARCH_FIELD_WIDTH, SEARCH_CONTROL_HEIGHT);
            searchBox.setMinimumSize(searchSize);
            searchBox.setPreferredSize(searchSize);
            searchBox.setMaximumSize(searchSize);
            searchBox.setBorder(new CompoundBorder(
                    new LineBorder(FIELD_BORDER),
                    new EmptyBorder(0, 10, 0, 4)
            ));
            searchBox.add(field, BorderLayout.CENTER);
            searchBox.add(clearButton, BorderLayout.EAST);

            GridBagConstraints searchBoxConstraints = new GridBagConstraints();
            searchBoxConstraints.gridx = 0;
            searchBoxConstraints.gridy = 0;
            searchBoxConstraints.fill = GridBagConstraints.NONE;
            searchBoxConstraints.anchor = GridBagConstraints.WEST;
            searchBoxConstraints.insets = new Insets(0, 0, 0, 10);
            row.add(searchBox, searchBoxConstraints);

            GridBagConstraints buttonConstraints = new GridBagConstraints();
            buttonConstraints.gridx = 1;
            buttonConstraints.gridy = 0;
            buttonConstraints.fill = GridBagConstraints.NONE;
            buttonConstraints.anchor = GridBagConstraints.WEST;
            row.add(searchButton, buttonConstraints);
        }
    }

    public static void addCommandActions(JPanel commandBar, JButton addButton, JButton refreshButton, JButton servicesButton) {
        Object controls = commandBar.getClientProperty(ACTION_CONTROLS);
        if (controls instanceof JPanel row) {
            row.add(addButton);
            row.add(refreshButton);
            row.add(servicesButton);
        }
    }

    public static void styleSearchButton(JButton button) {
        Dimension size = new Dimension(SEARCH_BUTTON_WIDTH, SEARCH_CONTROL_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
    }

    public static void styleClearButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setForeground(TEXT_SECONDARY);
        button.setBorder(new EmptyBorder(7, 8, 7, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
    }

    public static JPanel createNorthContainer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JPanel createButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        row.setBorder(BorderFactory.createEmptyBorder(20, 28, 10, 28));
        row.setOpaque(false);
        return row;
    }

    public static void styleAddButton(JButton button) {
        styleBaseButton(button, new Dimension(132, 36), Font.BOLD);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
    }

    public static void styleBulkDocumentButton(JButton button) {
        styleBaseButton(button, new Dimension(150, 32), Font.PLAIN);
        button.setBackground(ACTION_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    public static void styleRefreshButton(JButton button) {
        Font font = new Font("Segoe UI", Font.BOLD, 12);
        styleBaseButton(button, buttonSizeFor(button, font, 108, 36), Font.BOLD);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(PAGE_BACKGROUND);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 13, 7, 13)));
    }

    public static void styleActiveFilterButton(JButton button) {
        Font font = new Font("Segoe UI", Font.BOLD, 12);
        styleBaseButton(button, buttonSizeFor(button, font, 150, 36), Font.BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(180, 60, 50));
        button.setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
    }

    public static JButton createServicesMenuButton() {
        JButton button = new JButton("Services");
        styleServicesMenuButton(button);
        return button;
    }

    public static void styleServicesMenuButton(JButton button) {
        Dimension size = new Dimension(118, 36);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setIcon(new ChevronDownIcon(TEXT_SECONDARY));
        button.setIconTextGap(8);
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(PAGE_BACKGROUND);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 13, 7, 13)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
    }

    public static JMenuItem createServicesMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        styleServicesMenuItem(item);
        return item;
    }

    public static JPopupMenu createServicesMenu(JMenuItem... items) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(PAGE_BACKGROUND);
        menu.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(6, 0, 6, 0)));
        for (JMenuItem item : items) {
            styleServicesMenuItem(item);
            menu.add(item);
        }
        return menu;
    }

    public static void styleServicesMenuItem(JMenuItem item) {
        Dimension size = new Dimension(232, 38);
        item.setPreferredSize(size);
        item.setMinimumSize(size);
        item.setMaximumSize(size);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setOpaque(true);
        item.setForeground(item.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);
        item.setBackground(PAGE_BACKGROUND);
        item.setBorder(new EmptyBorder(9, 14, 9, 14));
        item.setCursor(Cursor.getPredefinedCursor(item.isEnabled() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                selectionBackground = MENU_SELECTION;
                selectionForeground = TEXT_PRIMARY;
                disabledForeground = TEXT_SECONDARY;
            }
        });
    }

    public static JPanel createBodyPanel() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(PAGE_BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(10, 28, 0, 28));
        return body;
    }

    public static JPanel createMainContentPanel() {
        JPanel panel = new HomeContentPanel();
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JScrollPane createMainScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    private static void styleBaseButton(JButton button, Dimension size, int fontStyle) {
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setFont(new Font("Segoe UI", fontStyle, 12));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.install(button);
    }

    private static Dimension buttonSizeFor(JButton button, Font font, int minimumWidth, int height) {
        String text = button.getText() == null ? "" : button.getText();
        FontMetrics metrics = button.getFontMetrics(font);
        int width = Math.max(minimumWidth, metrics.stringWidth(text) + 32);
        return new Dimension(width, height);
    }

    public static void setTextButtonEnabled(JButton button, boolean enabled) {
        ButtonStateHelper.setEnabled(button, enabled);
    }

    private static void styleSearchField(JTextField field) {
        field.setBorder(null);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(PAGE_BACKGROUND);
        field.setColumns(24);
        field.setPreferredSize(new Dimension(260, 34));
    }

    private static JPanel sectionCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(PAGE_BACKGROUND);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(16, BORDER),
                new EmptyBorder(20, 20, 20, 20)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitleLabel);
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(130, 140, 150));
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, 0, y);
            g2.dispose();
        }
    }

    private static class ResponsiveCommandBar extends JPanel {
        private static final int CARD_RADIUS = 8;
        private static final int HORIZONTAL_GAP = 16;
        private static final int VERTICAL_GAP = 10;

        private final JComponent searchControls;
        private final JComponent actionControls;

        private ResponsiveCommandBar(JComponent searchControls, JComponent actionControls) {
            super(null);
            this.searchControls = searchControls;
            this.actionControls = actionControls;
            setBackground(PAGE_BACKGROUND);
            setBorder(new CompoundBorder(
                    new RoundedBorder(CARD_RADIUS, BORDER),
                    new EmptyBorder(14, 16, 14, 16)));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            add(searchControls);
            add(actionControls);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int width = Math.max(0, getWidth() - insets.left - insets.right);
            Dimension searchSize = searchControls.getPreferredSize();
            Dimension actionSize = actionControls.getPreferredSize();
            boolean compact = width < searchSize.width + actionSize.width + HORIZONTAL_GAP;

            if (compact) {
                searchControls.setBounds(insets.left, insets.top, Math.min(width, searchSize.width), SEARCH_CONTROL_HEIGHT);
                actionControls.setBounds(
                        insets.left,
                        insets.top + SEARCH_CONTROL_HEIGHT + VERTICAL_GAP,
                        width,
                        SEARCH_CONTROL_HEIGHT
                );
                return;
            }

            int actionX = insets.left + width - actionSize.width;
            searchControls.setBounds(insets.left, insets.top, searchSize.width, SEARCH_CONTROL_HEIGHT);
            actionControls.setBounds(actionX, insets.top, actionSize.width, SEARCH_CONTROL_HEIGHT);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            Dimension searchSize = searchControls.getPreferredSize();
            Dimension actionSize = actionControls.getPreferredSize();
            int availableWidth = getWidth() > 0 ? getWidth() - insets.left - insets.right : Integer.MAX_VALUE;
            boolean compact = availableWidth < searchSize.width + actionSize.width + HORIZONTAL_GAP;
            int width = compact
                    ? Math.max(searchSize.width, actionSize.width)
                    : searchSize.width + actionSize.width + HORIZONTAL_GAP;
            int height = compact
                    ? SEARCH_CONTROL_HEIGHT * 2 + VERTICAL_GAP
                    : SEARCH_CONTROL_HEIGHT;
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }
    }

    private static class ChevronDownIcon implements Icon {
        private final Color color;

        private ChevronDownIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int centerY = y + 5;
            g2.drawLine(x + 1, centerY, x + 4, centerY + 3);
            g2.drawLine(x + 4, centerY + 3, x + 7, centerY);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 9;
        }

        @Override
        public int getIconHeight() {
            return 9;
        }
    }

    private static class HomeContentPanel extends JPanel implements Scrollable {
        private HomeContentPanel() {
            super(new BorderLayout());
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(80, visibleRect.height - 80);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }
}

