package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class AppTabsHelper {
    private static final int DEFAULT_TAB_CONTENT_INSET = 28;
    private static final int DEFAULT_TAB_DIVIDER_GAP = 8;

    private AppTabsHelper() {
    }

    public static void styleTabs(JTabbedPane tabs) {
        styleTabs(
                tabs,
                new Insets(6, DEFAULT_TAB_CONTENT_INSET, 12, DEFAULT_TAB_CONTENT_INSET),
                new Insets(8, 0, 0, 0)
        );
    }

    public static void styleTabs(JTabbedPane tabs, Insets tabAreaSpacing, Insets contentSpacing) {
        styleTabs(
                tabs,
                tabAreaSpacing,
                contentSpacing,
                new Insets(12, 14, 11, 14),
                DEFAULT_TAB_DIVIDER_GAP
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
        tabs.setForeground(EmployeeRegistrationViewHelper.TEXT_SECONDARY);
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
                g2.setColor(EmployeeRegistrationViewHelper.BORDER);
                g2.drawLine(tabAreaInsets.left, lineY, tabPane.getWidth() - tabAreaInsets.right, lineY);

                if (selectedIndex >= 0 && rects != null && selectedIndex < rects.length) {
                    Rectangle selected = rects[selectedIndex];
                    if (selected != null) {
                        int underlineWidth = Math.max(24, selected.width - 32);
                        g2.setColor(EmployeeRegistrationViewHelper.PRIMARY);
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
                g2.setColor(isSelected
                        ? EmployeeRegistrationViewHelper.PRIMARY
                        : EmployeeRegistrationViewHelper.TEXT_SECONDARY);
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
