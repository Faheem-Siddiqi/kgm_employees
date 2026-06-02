package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Styling constants and utilities for HomeStatsChartsPanel.
 * Centralizes all chart colors, fonts, spacing, and border definitions
 * so visual customization stays in one file.
 */
public final class HomeStatsChartHelper {
    // ── Background & Surface ──
    public static final Color BACKGROUND = Color.WHITE;
    public static final Color SURFACE = Color.WHITE;
    public static final Color MUTED_SURFACE = new Color(248, 250, 252);
    public static final Color CARD_BORDER = new Color(226, 232, 240);
    public static final Color GRID_LINE = new Color(241, 245, 249);

    // ── Text ──
    public static final Color TEXT_PRIMARY = new Color(2, 8, 23);
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);

    // ── Chart Palette ──
    public static final Color BLUE = new Color(37, 99, 235);
    public static final Color TEAL = new Color(13, 148, 136);
    public static final Color PURPLE = new Color(124, 58, 237);
    public static final Color ORANGE = new Color(245, 158, 11);
    public static final Color RED = new Color(239, 68, 68);
    public static final Color GREEN = new Color(34, 197, 94);

    // ── Layout Dimensions ──
    public static final int CARD_GAP = 12;
    public static final int SINGLE_COLUMN_WIDTH = 760;
    public static final int FULL_WIDTH_ITEM_LIMIT = 7;
    public static final int CHART_TOP = 24;
    public static final int CHART_LEFT = 50;
    public static final int CHART_RIGHT = 22;
    public static final int CHART_MIN_WIDTH = 400;
    public static final int CHART_HORIZONTAL_SCROLLBAR_HEIGHT = 12;
    public static final int CHART_SCROLL_UNIT = 40;
    public static final int CHART_SCROLL_BLOCK_GAP = 72;

    // ── Bar Dimensions ──
    public static final int BAR_MIN_WIDTH = 24;
    public static final int BAR_MAX_WIDTH = 48;
    public static final int SLOT_MIN_WIDTH = 64;
    public static final int SLOT_MAX_WIDTH = 100;
    public static final double BAR_WIDTH_RATIO = 0.54;
    public static final int LABEL_LINE_HEIGHT = 14;
    public static final int MAX_LABEL_LINES = 4;

    // ── Pie Dimensions ──
    public static final int PIE_MIN_SIZE = 138;
    public static final int PIE_MAX_SIZE = 178;
    public static final int LEGEND_ROW_HEIGHT = 24;

    // ── Fonts ──
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font CARD_TITLE_FONT = new Font("Segoe UI Semibold", Font.PLAIN, 14);
    public static final Font VALUE_FONT = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font LABEL_FONT_HOVER = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font GRID_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font LINK_FONT = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font EMPTY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font PIE_TOTAL_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font PIE_LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);

    private HomeStatsChartHelper() {
    }

    public static Color hoverColor(Color color) {
        int r = Math.max(0, color.getRed() - 18);
        int g = Math.max(0, color.getGreen() - 18);
        int b = Math.max(0, color.getBlue() - 18);
        return new Color(r, g, b);
    }

    public static CompoundBorder cardBorder() {
        return new CompoundBorder(
                new RoundedBorder(8, CARD_BORDER),
                new EmptyBorder(16, 16, 16, 16)
        );
    }

    public static CompoundBorder tooltipBorder() {
        return new CompoundBorder(
                new RoundedBorder(8, new Color(205, 214, 224)),
                new EmptyBorder(2, 2, 2, 2)
        );
    }

    public static CompoundBorder buttonBorder() {
        return new CompoundBorder(
                new RoundedBorder(8, CARD_BORDER),
                new EmptyBorder(4, 10, 4, 10)
        );
    }

    public static void styleHorizontalScrollBar(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }
        scrollBar.setOpaque(false);
        scrollBar.setPreferredSize(new Dimension(0, CHART_HORIZONTAL_SCROLLBAR_HEIGHT));
        scrollBar.setUnitIncrement(CHART_SCROLL_UNIT);
        scrollBar.setBlockIncrement(Math.max(CHART_SCROLL_UNIT, CHART_MIN_WIDTH - CHART_SCROLL_BLOCK_GAP));
        scrollBar.setUI(new ModernHorizontalScrollBarUI());
    }

    /**
     * Custom rounded border utility for card and tooltip styling.
     */
    public static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
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

    private static class ModernHorizontalScrollBarUI extends BasicScrollBarUI {
        private static final Color TRACK = new Color(248, 250, 252);
        private static final Color THUMB = new Color(203, 213, 225);
        private static final Color THUMB_HOVER = new Color(148, 163, 184);

        @Override
        protected void configureScrollBarColors() {
            trackColor = TRACK;
            thumbColor = THUMB;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return invisibleButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return invisibleButton();
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = 6;
            int y = trackBounds.y + Math.max(0, (trackBounds.height - height) / 2);
            g2.setColor(TRACK);
            g2.fillRoundRect(trackBounds.x, y, trackBounds.width, height, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = 6;
            int y = thumbBounds.y + Math.max(0, (thumbBounds.height - height) / 2);
            g2.setColor(isDragging || isThumbRollover() ? THUMB_HOVER : THUMB);
            g2.fillRoundRect(thumbBounds.x, y, thumbBounds.width, height, 6, 6);
            g2.dispose();
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(36, 6);
        }

        private JButton invisibleButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            button.setBorder(null);
            button.setFocusable(false);
            button.setOpaque(false);
            return button;
        }
    }
}
