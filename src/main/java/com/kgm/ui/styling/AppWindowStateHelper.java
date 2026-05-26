package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class AppWindowStateHelper {
    private static final String LOCKED_FULL_SIZE_KEY = "kgm.lockedFullSizeWindow";

    private AppWindowStateHelper() {
    }

    public static void lockFullSize(JFrame frame) {
        if (frame == null) {
            return;
        }

        frame.setResizable(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        if (Boolean.TRUE.equals(frame.getRootPane().getClientProperty(LOCKED_FULL_SIZE_KEY))) {
            return;
        }

        frame.getRootPane().putClientProperty(LOCKED_FULL_SIZE_KEY, true);
        frame.addWindowStateListener(event -> restoreFullSizeIfNeeded(frame, event.getNewState()));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                restoreFullSize(frame);
            }

            @Override
            public void windowActivated(WindowEvent event) {
                restoreFullSizeIfNeeded(frame, frame.getExtendedState());
            }
        });
    }

    private static void restoreFullSizeIfNeeded(JFrame frame, int state) {
        boolean minimized = (state & Frame.ICONIFIED) == Frame.ICONIFIED;
        if (minimized) {
            return;
        }
        boolean notMaximized = (state & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH;
        if (notMaximized) {
            restoreFullSize(frame);
        }
    }

    private static void restoreFullSize(JFrame frame) {
        SwingUtilities.invokeLater(() -> {
            if (frame.isDisplayable()) {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
    }
}
