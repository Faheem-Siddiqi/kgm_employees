package com.kgm.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class LoadingOverlay extends JPanel {
    private final JLabel titleLabel;
    private final JLabel messageLabel;
    private final JProgressBar progressBar;

    private LoadingOverlay(String title, String message) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
        });
        addMouseMotionListener(new MouseAdapter() {
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                event.consume();
            }
        });
        addKeyListener(new KeyAdapter() {
        });

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(226, 232, 240), 18, 1),
                BorderFactory.createEmptyBorder(22, 26, 22, 26)
        ));

        titleLabel = new JLabel(blankToDefault(title, "Working"));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 17));
        titleLabel.setForeground(new Color(17, 24, 39));

        messageLabel = new JLabel(blankToDefault(message, "Please wait..."));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(new Color(100, 116, 139));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(280, 8));
        progressBar.setMaximumSize(new Dimension(280, 8));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(progressBar);
        add(card, new GridBagConstraints());
    }

    public static Handle show(Component parent, String title, String message) {
        Window window = parent instanceof Window
                ? (Window) parent
                : SwingUtilities.getWindowAncestor(parent);
        if (!(window instanceof RootPaneContainer container)) {
            return Handle.noop();
        }

        JRootPane rootPane = container.getRootPane();
        Component previousGlassPane = rootPane.getGlassPane();
        boolean previousVisible = previousGlassPane != null && previousGlassPane.isVisible();
        LoadingOverlay overlay = new LoadingOverlay(title, message);
        rootPane.setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.requestFocusInWindow();
        return new Handle(rootPane, previousGlassPane, previousVisible, overlay);
    }

    private void setMessageText(String message) {
        messageLabel.setText(blankToDefault(message, "Please wait..."));
        revalidate();
        repaint();
    }

    private void setProgressValue(int value) {
        if (value < 0 || value > 100) {
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(false);
            return;
        }
        progressBar.setIndeterminate(false);
        progressBar.setValue(value);
        progressBar.setStringPainted(true);
        progressBar.setString(value + "%");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(new Color(15, 23, 42, 86));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(graphics);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static final class Handle implements AutoCloseable {
        private final JRootPane rootPane;
        private final Component previousGlassPane;
        private final boolean previousVisible;
        private final LoadingOverlay overlay;
        private boolean closed;

        private Handle(
                JRootPane rootPane,
                Component previousGlassPane,
                boolean previousVisible,
                LoadingOverlay overlay
        ) {
            this.rootPane = rootPane;
            this.previousGlassPane = previousGlassPane;
            this.previousVisible = previousVisible;
            this.overlay = overlay;
        }

        private static Handle noop() {
            return new Handle(null, null, false, null);
        }

        public void setMessage(String message) {
            if (overlay == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> overlay.setMessageText(message));
        }

        public void setProgress(int value) {
            if (overlay == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> overlay.setProgressValue(value));
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            Runnable closer = () -> {
                if (rootPane == null || overlay == null) {
                    return;
                }
                overlay.setVisible(false);
                if (rootPane.getGlassPane() == overlay && previousGlassPane != null) {
                    rootPane.setGlassPane(previousGlassPane);
                    previousGlassPane.setVisible(previousVisible);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                closer.run();
            } else {
                SwingUtilities.invokeLater(closer);
            }
        }
    }

    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            if (thickness <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            for (int index = 0; index < thickness; index++) {
                g2.drawRoundRect(
                        x + index,
                        y + index,
                        width - index - index - 1,
                        height - index - index - 1,
                        radius,
                        radius
                );
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.left = thickness;
            insets.top = thickness;
            insets.right = thickness;
            insets.bottom = thickness;
            return insets;
        }
    }
}
