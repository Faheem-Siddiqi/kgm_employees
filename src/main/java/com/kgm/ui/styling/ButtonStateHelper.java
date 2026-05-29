package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

public final class ButtonStateHelper {
    private static final String INSTALLED = "kgm.buttonState.installed";
    private static final String ENABLED_BACKGROUND = "kgm.buttonState.enabledBackground";
    private static final String ENABLED_CURSOR = "kgm.buttonState.enabledCursor";
    private static final Color FILLED_BUTTON_TEXT = Color.WHITE;
    private static final Color PLAIN_BUTTON_TEXT = new Color(99, 115, 129);

    private ButtonStateHelper() {
    }

    public static void install(AbstractButton button) {
        if (button == null || Boolean.TRUE.equals(button.getClientProperty(INSTALLED))) {
            return;
        }

        button.putClientProperty(INSTALLED, true);
        captureEnabledStyle(button);
        PropertyChangeListener listener = event -> {
            if ("enabled".equals(event.getPropertyName())) {
                applyState(button);
            }
        };
        button.addPropertyChangeListener(listener);
        applyState(button);
    }

    public static void setEnabled(AbstractButton button, boolean enabled) {
        install(button);
        button.setEnabled(enabled);
        applyState(button);
    }

    private static void captureEnabledStyle(AbstractButton button) {
        button.putClientProperty(ENABLED_BACKGROUND, button.getBackground());
        button.putClientProperty(ENABLED_CURSOR, button.getCursor());
    }

    private static void applyState(AbstractButton button) {
        Color background = colorProperty(button, ENABLED_BACKGROUND, button.getBackground());
        Cursor cursor = cursorProperty(button, ENABLED_CURSOR);

        button.setForeground(textColorFor(button, background));
        button.setBackground(button.isEnabled() ? background : faded(background));
        button.setCursor(button.isEnabled()
                ? cursor
                : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private static Color colorProperty(AbstractButton button, String key, Color fallback) {
        Object value = button.getClientProperty(key);
        return value instanceof Color color ? color : fallback;
    }

    private static Cursor cursorProperty(AbstractButton button, String key) {
        Object value = button.getClientProperty(key);
        return value instanceof Cursor cursor ? cursor : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    private static Color faded(Color color) {
        if (color == null) {
            return null;
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

    private static Color textColorFor(AbstractButton button, Color background) {
        if (!button.isContentAreaFilled() || isWhite(background)) {
            return PLAIN_BUTTON_TEXT;
        }
        return FILLED_BUTTON_TEXT;
    }

    private static boolean isWhite(Color color) {
        return color != null
                && color.getAlpha() > 0
                && color.getRed() >= 250
                && color.getGreen() >= 250
                && color.getBlue() >= 250;
    }
}
