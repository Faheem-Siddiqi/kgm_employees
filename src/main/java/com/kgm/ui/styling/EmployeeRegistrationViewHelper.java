package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public final class EmployeeRegistrationViewHelper {
    // Color constants
    public static final Color PRIMARY = new Color(0, 112, 210);
    public static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    public static final Color BORDER = new Color(220, 226, 232);
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color LINK_BLUE = new Color(0, 102, 204);
    private static final Color BUTTON_SECONDARY_BG = new Color(245, 245, 245);
    private static final Color BUTTON_SECONDARY_TEXT = new Color(80, 80, 80);
    private static final int TAB_CONTENT_INSET = 28;
    private static final int TAB_DIVIDER_GAP = 8;

    private EmployeeRegistrationViewHelper() {
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

    public static void styleBackButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(LINK_BLUE);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
    }

    public static JPanel createCenterWrapper() {
        JPanel wrapper = new PageContentPanel();
        wrapper.setBorder(new EmptyBorder(0, 20, 10, 20));
        wrapper.setOpaque(true);
        wrapper.setBackground(PAGE_BACKGROUND);
        return wrapper;
    }

    public static JScrollPane createPageScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        return scrollPane;
    }

    public static void installPageWheelForwarding(JScrollPane pageScroll, JComponent root) {
        MouseWheelListener listener = event -> forwardWheelToPageScroll(pageScroll, event);
        installWheelForwarding(root, listener);
    }

    public static GridBagConstraints pageConstraints(int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1.0;
        return gbc;
    }

    public static JPanel screenHeader(Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BACKGROUND);
        header.setBorder(new EmptyBorder(25, TAB_CONTENT_INSET, 0, TAB_CONTENT_INSET));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(PAGE_BACKGROUND);

        JLabel title = new JLabel("Employee Registration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Enter employee details and upload documents");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(100, 100, 100));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        JButton back = new JButton("Dashboard");
        styleBackButton(back);
        back.addActionListener(e -> onBack.run());

        header.add(titleBlock, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        return header;
    }

    public static JPanel createActionRow() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(PAGE_BACKGROUND);
        actions.setBorder(new EmptyBorder(8, TAB_CONTENT_INSET, 0, TAB_CONTENT_INSET));
        return actions;
    }

    public static JPanel createTabContent(JComponent content, JComponent actions) {
        JPanel tabContent = new JPanel(new BorderLayout());
        tabContent.setBackground(PAGE_BACKGROUND);

        JPanel stack = new JPanel(new GridBagLayout());
        stack.setBackground(PAGE_BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        stack.add(content, gbc);

        if (actions != null) {
            gbc.gridy = 1;
            stack.add(actions, gbc);
        }

        tabContent.add(stack, BorderLayout.NORTH);
        return tabContent;
    }

    public static void stylePrimaryButton(JButton button) {
        button.setPreferredSize(new Dimension(110, 34));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    public static void styleSecondaryButton(JButton button) {
        button.setPreferredSize(new Dimension(110, 34));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(BUTTON_SECONDARY_TEXT);
        button.setBackground(BUTTON_SECONDARY_BG);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(170, 170, 170)),
                new EmptyBorder(7, 16, 7, 16)
        ));
    }

    public static void styleFooterButton(JButton button) {
        stylePrimaryButton(button);
    }

    /**
     * Apply custom tab styling matching HomeViewHelper's tab UI.
     * This creates modern tabs with underline indicator for selected tab.
     */
    public static void styleTabs(JTabbedPane tabs) {
        styleTabs(tabs, new Insets(6, TAB_CONTENT_INSET, 12, TAB_CONTENT_INSET), new Insets(8, 0, 0, 0));
    }

    public static void styleTabs(JTabbedPane tabs, Insets tabAreaSpacing, Insets contentSpacing) {
        styleTabs(
                tabs,
                tabAreaSpacing,
                contentSpacing,
                new Insets(12, 14, 11, 14),
                TAB_DIVIDER_GAP
        );
    }

    public static void styleTabs(
            JTabbedPane tabs,
            Insets tabAreaSpacing,
            Insets contentSpacing,
            Insets tabLabelSpacing,
            int dividerGap
    ) {
        tabs.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        tabs.setBackground(Color.WHITE);
        tabs.setForeground(TEXT_SECONDARY);
        tabs.setOpaque(true);
        tabs.setBorder(null);
        tabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        tabs.setFocusable(false);
        installTabHoverCursor(tabs);
        tabs.setUI(new BasicTabbedPaneUI() {
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = tabLabelSpacing;
                selectedTabPadInsets = new Insets(0, 0, 0, 0); 
                contentBorderInsets = contentSpacing;
                tabAreaInsets = tabAreaSpacing;
            }

            protected void paintTabBackground(
                    Graphics graphics,
                    int tabPlacement,
                    int tabIndex,
                    int x,
                    int y,
                    int width,
                    int height,
                    boolean isSelected
            ) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(x, y, width, height);
            }

            protected void paintTabBorder(
                    Graphics graphics,
                    int tabPlacement,
                    int tabIndex,
                    int x,
                    int y,
                    int width,
                    int height,
                    boolean isSelected
            ) {
            }

            protected void paintTabArea(Graphics graphics, int tabPlacement, int selectedIndex) {
                super.paintTabArea(graphics, tabPlacement, selectedIndex);
                int lineY = 0;
                if (rects != null) {
                    for (Rectangle rect : rects) {
                        if (rect != null && rect.height > 0) {
                            lineY = Math.max(lineY, rect.y + rect.height + dividerGap);
                        }
                    }
                }
                if (lineY <= 0) {
                    lineY = 42;
                }

                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER);
                g2.drawLine(tabAreaInsets.left, lineY, tabPane.getWidth() - tabAreaInsets.right, lineY);

                if (selectedIndex >= 0 && rects != null && selectedIndex < rects.length) {
                    Rectangle selected = rects[selectedIndex];
                    if (selected != null) {
                        int underlineWidth = Math.max(24, selected.width - 32);
                        g2.setColor(PRIMARY);
                        g2.fillRoundRect(selected.x + 16, lineY - 1, underlineWidth, 3, 3, 3);
                    }
                }
                g2.dispose();
            }

            protected void paintContentBorder(Graphics graphics, int tabPlacement, int selectedIndex) {
            }

            protected void paintFocusIndicator(
                    Graphics graphics,
                    int tabPlacement,
                    Rectangle[] rectangles,
                    int tabIndex,
                    Rectangle iconRect,
                    Rectangle textRect,
                    boolean isSelected
            ) {
            }

            protected void paintText(
                    Graphics graphics,
                    int tabPlacement,
                    Font font,
                    FontMetrics metrics,
                    int tabIndex,
                    String title,
                    Rectangle textRect,
                    boolean isSelected
            ) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
                g2.setFont(font);
                g2.setColor(isSelected ? PRIMARY : TEXT_SECONDARY);
                g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
                g2.dispose();
            }
        });
    }

    private static void installTabHoverCursor(JTabbedPane tabs) {
        MouseAdapter tabCursor = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int tabIndex = tabs.indexAtLocation(event.getX(), event.getY());
                boolean hoveringEnabledTab = tabIndex >= 0 && tabs.isEnabledAt(tabIndex);
                tabs.setCursor(Cursor.getPredefinedCursor(
                        hoveringEnabledTab ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
                ));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                tabs.setCursor(Cursor.getDefaultCursor());
            }
        };
        tabs.addMouseMotionListener(tabCursor);
        tabs.addMouseListener(tabCursor);
    }

    private static void installWheelForwarding(Component component, MouseWheelListener listener) {
        if (component instanceof JScrollBar) {
            return;
        }
        if (component instanceof JComponent jComponent) {
            String key = "kgm.pageWheelForwarding";
            if (!Boolean.TRUE.equals(jComponent.getClientProperty(key))) {
                jComponent.putClientProperty(key, true);
                jComponent.addMouseWheelListener(listener);
            }
        } else {
            component.addMouseWheelListener(listener);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installWheelForwarding(child, listener);
            }
        }
    }

    private static void forwardWheelToPageScroll(JScrollPane pageScroll, MouseWheelEvent event) {
        if (event.isConsumed() || pageScroll == null) {
            return;
        }
        JScrollBar vertical = pageScroll.getVerticalScrollBar();
        if (vertical == null || !vertical.isVisible()) {
            return;
        }

        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount;
        if (event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
            amount = event.getWheelRotation() * vertical.getBlockIncrement(direction);
        } else {
            amount = event.getUnitsToScroll() * vertical.getUnitIncrement(direction);
        }

        int maxValue = vertical.getMaximum() - vertical.getVisibleAmount();
        int nextValue = Math.max(vertical.getMinimum(), Math.min(maxValue, vertical.getValue() + amount));
        if (nextValue != vertical.getValue()) {
            vertical.setValue(nextValue);
            event.consume();
        }
    }

    private static class PageContentPanel extends JPanel implements Scrollable {
        PageContentPanel() {
            super(new GridBagLayout());
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 32);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}

