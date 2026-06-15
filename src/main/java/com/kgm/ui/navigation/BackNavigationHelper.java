package com.kgm.ui.navigation;

import com.kgm.ui.HomeView;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

public final class BackNavigationHelper {
    private static final Color HOVER_BACKGROUND = new Color(241, 245, 249);
    private static final Dimension BUTTON_SIZE = new Dimension(18,18);
    private static final int ICON_SIZE = 18;
    private static final Deque<WeakReference<Window>> WINDOW_HISTORY = new ArrayDeque<>();

    private BackNavigationHelper() {
    }

    public static JButton createBackButton(String currentTitle) {
        JButton button = new JButton();
        button.setPreferredSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
        button.setToolTipText("Back");
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setVisible(shouldShowBackButton(currentTitle));

        ImageIcon icon = loadBackArrowIcon();
        if (icon.getIconWidth() > 0) {
            button.setIcon(icon);
        } else {
            button.setText("<");
            button.setFont(button.getFont().deriveFont(20f));
        }

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(HOVER_BACKGROUND);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(Color.WHITE);
            }
        });
        button.addActionListener(event -> navigateBack(button));
        return button;
    }

    public static boolean shouldShowBackButton(String currentTitle) {
        String normalizedTitle = currentTitle == null ? "" : currentTitle.trim();
        return !normalizedTitle.isBlank() && !"Dashboard".equalsIgnoreCase(normalizedTitle);
    }

    public static void registerWindow(Component component) {
        Window window = SwingUtilities.getWindowAncestor(component);
        if (window == null) {
            return;
        }

        synchronized (WINDOW_HISTORY) {
            removeWindow(window);
            WINDOW_HISTORY.addFirst(new WeakReference<>(window));
            trimHistory();
        }
    }

    private static void navigateBack(Component source) {
        SwingUtilities.invokeLater(() -> {
            Window currentWindow = SwingUtilities.getWindowAncestor(source);
            Window previousWindow = findPreviousWindow(currentWindow);
            if (previousWindow != null) {
                previousWindow.setVisible(true);
                previousWindow.toFront();
                previousWindow.requestFocus();
                if (currentWindow != null && currentWindow != previousWindow) {
                    currentWindow.dispose();
                }
                return;
            }

            if (!(currentWindow instanceof HomeView)) {
                new HomeView().setVisible(true);
            }
            if (currentWindow != null) {
                currentWindow.dispose();
            }
        });
    }

    private static Window findPreviousWindow(Window currentWindow) {
        synchronized (WINDOW_HISTORY) {
            removeWindow(currentWindow);
            while (!WINDOW_HISTORY.isEmpty()) {
                Window candidate = WINDOW_HISTORY.removeFirst().get();
                if (candidate != null && candidate != currentWindow && candidate.isDisplayable()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void removeWindow(Window window) {
        if (window == null) {
            return;
        }
        WINDOW_HISTORY.removeIf(reference -> {
            Window candidate = reference.get();
            return candidate == null || candidate == window || !candidate.isDisplayable();
        });
    }

    private static void trimHistory() {
        while (WINDOW_HISTORY.size() > 12) {
            WINDOW_HISTORY.removeLast();
        }
    }

    private static ImageIcon loadBackArrowIcon() {
        ImageIcon icon = loadClasspathIcon("/images/BackArrow.png");
        if (icon.getIconWidth() <= 0) {
            icon = loadFileIcon(Path.of("images", "BackArrow.png"));
        }
        if (icon.getIconWidth() <= 0) {
            return new ImageIcon();
        }
        Image image = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private static ImageIcon loadClasspathIcon(String resourcePath) {
        URL resource = BackNavigationHelper.class.getResource(resourcePath);
        return resource == null ? new ImageIcon() : new ImageIcon(resource);
    }

    private static ImageIcon loadFileIcon(Path path) {
        if (!Files.isRegularFile(path)) {
            return new ImageIcon();
        }
        return new ImageIcon(path.toString());
    }
}
