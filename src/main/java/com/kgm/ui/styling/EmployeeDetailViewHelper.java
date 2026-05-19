package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public final class EmployeeDetailViewHelper {
    // Color constants matching HomeViewHelper
    public static final Color PRIMARY = new Color(0, 112, 210);
    public static final Color TEXT_SECONDARY = new Color(99, 115, 129);
    public static final Color BORDER = new Color(220, 226, 232);
    private static final int TAB_DIVIDER_GAP = 8;

    private EmployeeDetailViewHelper() {
    }

    public static void applyFrame(JFrame frame) {
        EmployeeDetailViewStyle.applyFrame(frame);
    }

    public static JPanel createTopContainer() {
        return EmployeeDetailViewStyle.createTopContainer();
    }

    public static JPanel createSecondRow() {
        return EmployeeDetailViewStyle.createSecondRow();
    }

    public static JPanel createBackButtonPanel() {
        return EmployeeDetailViewStyle.createBackButtonPanel();
    }

    public static void styleBackButton(JButton button) {
        EmployeeDetailViewStyle.styleBackButton(button);
    }

    public static JPanel createEmployeeSummaryPanel() {
        return EmployeeDetailViewStyle.createEmployeeSummaryPanel();
    }

    public static void styleEmployeeName(JLabel label) {
        EmployeeDetailViewStyle.styleEmployeeName(label);
    }

    public static void styleEmployeeCode(JLabel label) {
        EmployeeDetailViewStyle.styleEmployeeCode(label);
    }

    public static JPanel createCenterWrapper() {
        return EmployeeDetailViewStyle.createCenterWrapper();
    }

    public static void styleUpdateButton(JButton button) {
        EmployeeDetailViewStyle.styleUpdateButton(button);
    }

    /**
     * Apply custom tab styling matching HomeViewHelper's tab UI.
     * This creates modern tabs with underline indicator for selected tab.
     */
    public static void styleTabs(JTabbedPane tabs) {
        tabs.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        tabs.setBackground(Color.WHITE);
        tabs.setForeground(TEXT_SECONDARY);
        tabs.setOpaque(true);
        tabs.setBorder(null);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        installTabHoverCursor(tabs);
        tabs.setUI(new BasicTabbedPaneUI() {
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(12, 26, 11, 26);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(10, 0, 0, 0);
                tabAreaInsets = new Insets(TAB_DIVIDER_GAP, 28, 18, 28);
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
                            lineY = Math.max(lineY, rect.y + rect.height + TAB_DIVIDER_GAP);
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
}
