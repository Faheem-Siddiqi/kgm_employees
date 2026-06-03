package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class AppWindowStateHelper {
    private static final String FULL_SIZE_INITIALIZED_KEY = "kgm.fullSizeInitialized";

    private AppWindowStateHelper() {
    }

    public static void lockFullSize(JFrame frame) {
        if (frame == null) {
            return;
        }

        frame.setResizable(true);
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        SwingUtilities.invokeLater(() -> maximizeIfVisible(frame));

        if (Boolean.TRUE.equals(frame.getRootPane().getClientProperty(FULL_SIZE_INITIALIZED_KEY))) {
            return;
        }

        frame.getRootPane().putClientProperty(FULL_SIZE_INITIALIZED_KEY, true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                SwingUtilities.invokeLater(() -> maximizeIfVisible(frame));
            }
        });
    }

    private static void maximizeIfVisible(JFrame frame) {
        if (frame.isDisplayable() && (frame.getExtendedState() & Frame.ICONIFIED) != Frame.ICONIFIED) {
            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }
}
