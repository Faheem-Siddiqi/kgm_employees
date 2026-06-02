package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class HomeViewHelper {
    private static final int SEARCH_FIELD_WIDTH = 300;
    private static final int SEARCH_FIELD_MIN_WIDTH = 190;
    private static final int SEARCH_BUTTON_WIDTH = 96;
    private static final int SEARCH_CONTROL_WIDTH = SEARCH_FIELD_WIDTH + SEARCH_BUTTON_WIDTH + 10;
    private static final int SEARCH_CONTROL_MIN_WIDTH = SEARCH_FIELD_MIN_WIDTH + SEARCH_BUTTON_WIDTH + 10;
    private static final int SEARCH_CONTROL_HEIGHT = 36;
    private static final int COMMAND_BUTTON_RADIUS = 8;
    private static final String SEARCH_CONTROLS = "searchControls";
    private static final String ACTION_CONTROLS = "actionControls";

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY = new Color(18, 104, 217);
    private static final Color PRIMARY_HOVER = new Color(15, 92, 194);
    private static final Color PRIMARY_PRESSED = new Color(12, 74, 164);
    private static final Color ADD_GREEN = new Color(22, 128, 84);
    private static final Color ADD_GREEN_HOVER = new Color(18, 112, 73);
    private static final Color ADD_GREEN_PRESSED = new Color(15, 93, 61);
    private static final Color REFRESH_TEAL = new Color(14, 116, 144);
    private static final Color REFRESH_TEAL_HOVER = new Color(12, 103, 129);
    private static final Color REFRESH_TEAL_PRESSED = new Color(10, 82, 103);
    private static final Color SERVICES_SLATE = new Color(51, 65, 85);
    private static final Color SERVICES_SLATE_HOVER = new Color(40, 52, 70);
    private static final Color SERVICES_SLATE_PRESSED = new Color(30, 41, 59);
    private static final Color CLEAR_RED = new Color(190, 18, 60);
    private static final Color CLEAR_FILTER_RED = new Color(185, 28, 28);
    private static final Color CLEAR_FILTER_RED_HOVER = new Color(153, 27, 27);
    private static final Color CLEAR_FILTER_RED_PRESSED = new Color(127, 29, 29);
    private static final Color ACTION_BLUE = new Color(30, 144, 255);
    private static final Color TEXT_PRIMARY = new Color(35, 43, 54);
    private static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    private static final Color BORDER = new Color(203, 213, 225);
    private static final Color MENU_SELECTION = new Color(239, 246, 255);
    private static final Color FIELD_BORDER = new Color(203, 213, 225);
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);
    private static final Color FIELD_ICON = new Color(100, 116, 139);
    private static final CommandButtonStyle SEARCH_BUTTON_STYLE = new CommandButtonStyle(PRIMARY, PRIMARY_HOVER, PRIMARY_PRESSED);
    private static final CommandButtonStyle ADD_BUTTON_STYLE = new CommandButtonStyle(ADD_GREEN, ADD_GREEN_HOVER, ADD_GREEN_PRESSED);
    private static final CommandButtonStyle REFRESH_BUTTON_STYLE = new CommandButtonStyle(REFRESH_TEAL, REFRESH_TEAL_HOVER, REFRESH_TEAL_PRESSED);
    private static final CommandButtonStyle SERVICES_BUTTON_STYLE = new CommandButtonStyle(SERVICES_SLATE, SERVICES_SLATE_HOVER, SERVICES_SLATE_PRESSED);
    private static final CommandButtonStyle CLEAR_FILTER_BUTTON_STYLE = new CommandButtonStyle(CLEAR_FILTER_RED, CLEAR_FILTER_RED_HOVER, CLEAR_FILTER_RED_PRESSED);

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
        searchControls.setMinimumSize(new Dimension(SEARCH_CONTROL_MIN_WIDTH, SEARCH_CONTROL_HEIGHT));
        searchControls.setPreferredSize(new Dimension(SEARCH_CONTROL_WIDTH, SEARCH_CONTROL_HEIGHT));

        JPanel actionControls = new CommandActionPanel(8, 8);
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
            searchBox.setBackground(Color.WHITE);
            Dimension searchSize = new Dimension(SEARCH_FIELD_WIDTH, SEARCH_CONTROL_HEIGHT);
            searchBox.setMinimumSize(new Dimension(SEARCH_FIELD_MIN_WIDTH, SEARCH_CONTROL_HEIGHT));
            searchBox.setPreferredSize(searchSize);
            searchBox.setBorder(new CompoundBorder(
                    new RoundedLineBorder(COMMAND_BUTTON_RADIUS, FIELD_BORDER),
                    new EmptyBorder(0, 10, 0, 4)
            ));
            JLabel searchIcon = new JLabel(new SearchIcon(FIELD_ICON));
            searchIcon.setBorder(new EmptyBorder(0, 0, 0, 8));
            searchBox.add(searchIcon, BorderLayout.WEST);
            searchBox.add(field, BorderLayout.CENTER);
            searchBox.add(clearButton, BorderLayout.EAST);

            GridBagConstraints searchBoxConstraints = new GridBagConstraints();
            searchBoxConstraints.gridx = 0;
            searchBoxConstraints.gridy = 0;
            searchBoxConstraints.weightx = 1.0;
            searchBoxConstraints.fill = GridBagConstraints.HORIZONTAL;
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
        styleFilledCommandButton(button, size, Font.BOLD, SEARCH_BUTTON_STYLE);
        button.setIcon(new SearchIcon(Color.WHITE));
        button.setIconTextGap(8);
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
        Font font = new Font("Segoe UI", Font.BOLD, 12);
        styleFilledCommandButton(button, buttonSizeFor(button, font, 136, SEARCH_CONTROL_HEIGHT),
                Font.BOLD, ADD_BUTTON_STYLE);
        button.setIcon(new PlusIcon(Color.WHITE));
        button.setIconTextGap(8);
    }

    public static void styleBulkDocumentButton(JButton button) {
        styleBaseButton(button, new Dimension(150, 32), Font.PLAIN);
        button.setBackground(ACTION_BLUE);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    public static void styleRefreshButton(JButton button) {
        Font font = new Font("Segoe UI", Font.BOLD, 12);
        styleFilledCommandButton(button, buttonSizeFor(button, font, 108, SEARCH_CONTROL_HEIGHT),
                Font.BOLD, REFRESH_BUTTON_STYLE);
        button.setIcon(new RefreshIcon(Color.WHITE));
        button.setIconTextGap(8);
    }

    public static void styleActiveFilterButton(JButton button) {
        Font font = new Font("Segoe UI", Font.BOLD, 12);
        styleFilledCommandButton(button, buttonSizeFor(button, font, 154, SEARCH_CONTROL_HEIGHT),
                Font.BOLD, CLEAR_FILTER_BUTTON_STYLE);
        button.setIcon(new ClearFilterIcon(Color.WHITE));
        button.setIconTextGap(8);
    }

    public static JButton createServicesMenuButton() {
        JButton button = new JButton("Services");
        styleServicesMenuButton(button);
        return button;
    }

    public static void styleServicesMenuButton(JButton button) {
        Dimension size = new Dimension(116, SEARCH_CONTROL_HEIGHT);
        styleFilledCommandButton(button, size, Font.BOLD, SERVICES_BUTTON_STYLE);
        button.setIcon(new ChevronDownIcon(Color.WHITE));
        button.setIconTextGap(8);
        button.setHorizontalTextPosition(SwingConstants.LEFT);
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

    private static void styleFilledCommandButton(
            JButton button,
            Dimension size,
            int fontStyle,
            CommandButtonStyle style
    ) {
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setFont(new Font("Segoe UI", fontStyle, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(style.background());
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setBorder(new CompoundBorder(
                new RoundedLineBorder(COMMAND_BUTTON_RADIUS, translucentWhite(70)),
                new EmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ButtonStateHelper.installRounded(button, COMMAND_BUTTON_RADIUS);
        ButtonStateHelper.setHoverBackground(button, style.hoverBackground(), style.pressedBackground());
    }

    private static Dimension buttonSizeFor(JButton button, Font font, int minimumWidth, int height) {
        String text = button.getText() == null ? "" : button.getText();
        FontMetrics metrics = button.getFontMetrics(font);
        int width = Math.max(minimumWidth, metrics.stringWidth(text) + 54);
        return new Dimension(width, height);
    }

    public static void setTextButtonEnabled(JButton button, boolean enabled) {
        ButtonStateHelper.setEnabled(button, enabled);
    }

    private static void styleSearchField(JTextField field) {
        field.setBorder(null);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
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
        private static final int HORIZONTAL_GAP = 18;
        private static final int VERTICAL_GAP = 12;

        private final JComponent searchControls;
        private final JComponent actionControls;

        private ResponsiveCommandBar(JComponent searchControls, JComponent actionControls) {
            super(null);
            this.searchControls = searchControls;
            this.actionControls = actionControls;
            setOpaque(false);
            setBorder(new CompoundBorder(
                    new RoundedBorder(CARD_RADIUS, BORDER),
                    new EmptyBorder(12, 14, 12, 14)));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            add(searchControls);
            add(actionControls);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(PAGE_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS);
            g2.dispose();
            super.paintComponent(graphics);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int width = Math.max(0, getWidth() - insets.left - insets.right);
            Dimension searchSize = searchControls.getPreferredSize();
            Dimension actionSize = actionControls.getPreferredSize();
            boolean compact = width < searchSize.width + actionSize.width + HORIZONTAL_GAP;

            if (compact) {
                int actionHeight = actionHeightFor(width);
                searchControls.setBounds(insets.left, insets.top, width, SEARCH_CONTROL_HEIGHT);
                actionControls.setBounds(
                        insets.left,
                        insets.top + SEARCH_CONTROL_HEIGHT + VERTICAL_GAP,
                        width,
                        actionHeight
                );
                return;
            }

            int actionX = insets.left + width - actionSize.width;
            searchControls.setBounds(insets.left, insets.top, searchSize.width, SEARCH_CONTROL_HEIGHT);
            actionControls.setBounds(actionX, insets.top, actionSize.width, actionHeightFor(actionSize.width));
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
            int actionHeight = actionHeightFor(compact ? Math.max(SEARCH_FIELD_MIN_WIDTH, availableWidth) : actionSize.width);
            int height = compact
                    ? SEARCH_CONTROL_HEIGHT + VERTICAL_GAP + actionHeight
                    : Math.max(SEARCH_CONTROL_HEIGHT, actionHeight);
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        private int actionHeightFor(int width) {
            if (actionControls instanceof CommandActionPanel panel) {
                return panel.preferredHeightForWidth(width);
            }
            return Math.max(SEARCH_CONTROL_HEIGHT, actionControls.getPreferredSize().height);
        }
    }

    private static class CommandActionPanel extends JPanel {
        private final int horizontalGap;
        private final int verticalGap;

        private CommandActionPanel(int horizontalGap, int verticalGap) {
            super(null);
            this.horizontalGap = horizontalGap;
            this.verticalGap = verticalGap;
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int width = Math.max(0, getWidth());
            int rowY = 0;
            int index = 0;
            while (index < getComponentCount()) {
                RowMetrics row = rowMetrics(index, width);
                layoutRow(index, row.endIndex(), width, row.width(), rowY);
                rowY += row.height() + verticalGap;
                index = row.endIndex();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int width = 0;
            int height = SEARCH_CONTROL_HEIGHT;
            for (int index = 0; index < getComponentCount(); index++) {
                Component child = getComponent(index);
                if (!child.isVisible()) {
                    continue;
                }
                Dimension size = child.getPreferredSize();
                width += size.width + (width > 0 ? horizontalGap : 0);
                height = Math.max(height, size.height);
            }
            return new Dimension(width, height);
        }

        private int preferredHeightForWidth(int width) {
            if (getComponentCount() == 0) {
                return SEARCH_CONTROL_HEIGHT;
            }
            int safeWidth = Math.max(1, width);
            int height = 0;
            int index = 0;
            while (index < getComponentCount()) {
                RowMetrics row = rowMetrics(index, safeWidth);
                height += row.height();
                index = row.endIndex();
                if (index < getComponentCount()) {
                    height += verticalGap;
                }
            }
            return Math.max(SEARCH_CONTROL_HEIGHT, height);
        }

        private RowMetrics rowMetrics(int startIndex, int availableWidth) {
            int rowWidth = 0;
            int rowHeight = SEARCH_CONTROL_HEIGHT;
            int index = startIndex;
            for (; index < getComponentCount(); index++) {
                Component child = getComponent(index);
                if (!child.isVisible()) {
                    continue;
                }
                Dimension size = child.getPreferredSize();
                int nextWidth = rowWidth == 0 ? size.width : rowWidth + horizontalGap + size.width;
                if (rowWidth > 0 && nextWidth > availableWidth) {
                    break;
                }
                rowWidth = nextWidth;
                rowHeight = Math.max(rowHeight, size.height);
            }
            if (index == startIndex) {
                index++;
            }
            return new RowMetrics(index, rowWidth, rowHeight);
        }

        private void layoutRow(int startIndex, int endIndex, int availableWidth, int rowWidth, int rowY) {
            int x = Math.max(0, availableWidth - rowWidth);
            for (int index = startIndex; index < endIndex; index++) {
                Component child = getComponent(index);
                if (!child.isVisible()) {
                    continue;
                }
                Dimension size = child.getPreferredSize();
                child.setBounds(x, rowY, Math.min(size.width, Math.max(1, availableWidth)), size.height);
                x += size.width + horizontalGap;
            }
        }

        private record RowMetrics(int endIndex, int width, int height) {
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
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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

    private static class PlusIcon implements Icon {
        private final Color color;

        private PlusIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 8, y + 2, x + 8, y + 14);
            g2.drawLine(x + 2, y + 8, x + 14, y + 8);
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

    private static class RefreshIcon implements Icon {
        private final Color color;

        private RefreshIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(x + 2, y + 2, 12, 12, 35, 285);
            g2.drawLine(x + 13, y + 3, x + 13, y + 8);
            g2.drawLine(x + 13, y + 3, x + 8, y + 3);
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

    private static class ClearFilterIcon implements Icon {
        private final Color color;

        private ClearFilterIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 4, y + 4, x + 12, y + 12);
            g2.drawLine(x + 12, y + 4, x + 4, y + 12);
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

    private static class RoundedLineBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        private RoundedLineBorder(int radius, Color color) {
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
            return new Insets(1, 1, 1, 1);
        }
    }

    private static Color translucentWhite(int alpha) {
        return new Color(255, 255, 255, alpha);
    }

    private record CommandButtonStyle(Color background, Color hoverBackground, Color pressedBackground) {
    }
}

