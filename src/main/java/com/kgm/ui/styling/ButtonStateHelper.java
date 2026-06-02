package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.beans.PropertyChangeListener;

public final class ButtonStateHelper {
    private static final String INSTALLED = "kgm.buttonState.installed";
    private static final String ENABLED_BACKGROUND = "kgm.buttonState.enabledBackground";
    private static final String ENABLED_FOREGROUND = "kgm.buttonState.enabledForeground";
    private static final String ENABLED_CURSOR = "kgm.buttonState.enabledCursor";
    private static final String ROUND_RADIUS = "kgm.buttonState.roundRadius";
    private static final String HOVER_BACKGROUND = "kgm.buttonState.hoverBackground";
    private static final String PRESSED_BACKGROUND = "kgm.buttonState.pressedBackground";
    private static final String PAINT_ROUNDED_BACKGROUND = "kgm.buttonState.paintRoundedBackground";
    private static final Color FILLED_BUTTON_TEXT = Color.WHITE;
    private static final Color PLAIN_BUTTON_TEXT = new Color(99, 115, 129);
    private static final Color DISABLED_FILLED_TEXT = Color.WHITE;
    private static final Color DISABLED_PLAIN_TEXT = PLAIN_BUTTON_TEXT;

    private ButtonStateHelper() {
    }

    public static void install(AbstractButton button) {
        if (button == null || Boolean.TRUE.equals(button.getClientProperty(INSTALLED))) {
            return;
        }

        button.putClientProperty(INSTALLED, true);
        button.setUI(new ReadableButtonUI());
        captureEnabledStyle(button);
        PropertyChangeListener listener = event -> {
            if ("enabled".equals(event.getPropertyName())) {
                applyState(button);
            }
        };
        button.addPropertyChangeListener(listener);
        applyState(button);
    }

    public static void installRounded(AbstractButton button, int radius) {
        if (button == null) {
            return;
        }
        button.putClientProperty(PAINT_ROUNDED_BACKGROUND, true);
        button.putClientProperty(ROUND_RADIUS, Math.max(0, radius));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        install(button);
    }

    public static void setHoverBackground(AbstractButton button, Color hoverBackground, Color pressedBackground) {
        if (button == null) {
            return;
        }
        button.putClientProperty(HOVER_BACKGROUND, hoverBackground);
        button.putClientProperty(PRESSED_BACKGROUND, pressedBackground);
        button.repaint();
    }

    public static void setEnabled(AbstractButton button, boolean enabled) {
        install(button);
        button.setEnabled(enabled);
        applyState(button);
    }

    public static void setEnabledForeground(AbstractButton button, Color foreground) {
        install(button);
        button.putClientProperty(ENABLED_FOREGROUND, foreground);
        applyState(button);
    }

    private static void captureEnabledStyle(AbstractButton button) {
        button.putClientProperty(ENABLED_BACKGROUND, button.getBackground());
        button.putClientProperty(ENABLED_FOREGROUND, button.getForeground());
        button.putClientProperty(ENABLED_CURSOR, button.getCursor());
    }

    private static void applyState(AbstractButton button) {
        Color background = colorProperty(button, ENABLED_BACKGROUND, button.getBackground());
        Color foreground = colorProperty(button, ENABLED_FOREGROUND, button.getForeground());
        Cursor cursor = cursorProperty(button, ENABLED_CURSOR);
        boolean filled = isFilledButton(button, background);

        button.setForeground(button.isEnabled()
                ? textColorFor(button, background, foreground)
                : disabledTextColorFor(filled));
        button.setBackground(button.isEnabled()
                ? background
                : disabledBackgroundFor(background, filled));
        button.setCursor(button.isEnabled()
                ? cursor
                : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private static Color colorProperty(AbstractButton button, String key, Color fallback) {
        Object value = button.getClientProperty(key);
        return value instanceof Color color ? color : fallback;
    }

    private static Cursor cursorProperty(AbstractButton button, String key) {
        Object value = button.getClientProperty(key);
        return value instanceof Cursor cursor ? cursor : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    private static Color disabledBackgroundFor(Color color, boolean filled) {
        if (color == null) {
            return null;
        }
        if (!filled) {
            return color;
        }
        return new Color(
                mix(color.getRed()),
                mix(color.getGreen()),
                mix(color.getBlue()),
                color.getAlpha()
        );
    }

    private static int mix(int value) {
        return Math.min(255, (int) Math.round(value * 0.68 + 255 * 0.32));
    }

    private static Color textColorFor(AbstractButton button, Color background, Color foreground) {
        if (!isFilledButton(button, background)) {
            return foreground == null || Color.WHITE.equals(foreground) ? PLAIN_BUTTON_TEXT : foreground;
        }
        return FILLED_BUTTON_TEXT;
    }

    private static Color disabledTextColorFor(boolean filled) {
        return filled ? DISABLED_FILLED_TEXT : DISABLED_PLAIN_TEXT;
    }

    private static boolean isLight(Color color) {
        return color != null
                && color.getAlpha() > 0
                && relativeLuminance(color) >= 0.78;
    }

    private static boolean isFilledButton(AbstractButton button, Color background) {
        return (button.isContentAreaFilled() || Boolean.TRUE.equals(button.getClientProperty(PAINT_ROUNDED_BACKGROUND)))
                && !isLight(background);
    }

    private static double relativeLuminance(Color color) {
        return (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255.0;
    }

    private static class ReadableButtonUI extends BasicButtonUI {
        @Override
        public void update(Graphics graphics, JComponent component) {
            if (paintsRoundedBackground(component)) {
                paint(graphics, component);
                return;
            }
            super.update(graphics, component);
        }

        @Override
        public void paint(Graphics graphics, JComponent component) {
            if (component instanceof AbstractButton button && paintsRoundedBackground(component)) {
                paintRoundedBackground(graphics, button);
            }
            super.paint(graphics, component);
        }

        @Override
        protected void paintText(Graphics graphics, AbstractButton button, Rectangle textRect, String text) {
            ButtonModel model = button.getModel();
            FontMetrics metrics = graphics.getFontMetrics();
            int mnemonicIndex = button.getDisplayedMnemonicIndex();
            graphics.setColor(button.getForeground());
            if (model.isEnabled()) {
                super.paintText(graphics, button, textRect, text);
                return;
            }
            graphics.drawString(text, textRect.x, textRect.y + metrics.getAscent());
            if (mnemonicIndex >= 0 && mnemonicIndex < text.length()) {
                int underlineX = textRect.x + metrics.stringWidth(text.substring(0, mnemonicIndex));
                int underlineY = textRect.y + metrics.getAscent() + 1;
                int underlineWidth = metrics.charWidth(text.charAt(mnemonicIndex));
                graphics.fillRect(underlineX, underlineY, underlineWidth, 1);
            }
        }

        private static boolean paintsRoundedBackground(JComponent component) {
            return Boolean.TRUE.equals(component.getClientProperty(PAINT_ROUNDED_BACKGROUND));
        }

        private static void paintRoundedBackground(Graphics graphics, AbstractButton button) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = button.getBackground();
            ButtonModel model = button.getModel();
            if (button.isEnabled()) {
                if (model.isPressed() && model.isArmed()) {
                    fill = colorProperty(button, PRESSED_BACKGROUND, fill);
                } else if (model.isRollover()) {
                    fill = colorProperty(button, HOVER_BACKGROUND, fill);
                }
            }

            int radius = intProperty(button, ROUND_RADIUS, 8);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), radius, radius);
            g2.dispose();
        }

        private static int intProperty(AbstractButton button, String key, int fallback) {
            Object value = button.getClientProperty(key);
            return value instanceof Number number ? number.intValue() : fallback;
        }
    }
}
