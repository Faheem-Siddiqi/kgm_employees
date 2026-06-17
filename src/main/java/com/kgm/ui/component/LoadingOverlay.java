package com.kgm.ui.component;

import com.kgm.ui.styling.ButtonStateHelper;
import com.kgm.ui.styling.UniversalDialogHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class LoadingOverlay extends JPanel {
    private static final int CARD_WIDTH = 460;
    private static final int CONTENT_WIDTH = 386;
    private static final int PROGRESS_HEIGHT = 18;

    private final JLabel titleLabel;
    private final JTextArea messageArea;
    private final JProgressBar progressBar;
    private final JButton stopButton;

    private LoadingOverlay(String title, String message) {
        this(title, message, null);
    }

    private LoadingOverlay(String title, String message, Runnable onStop) {
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

        JPanel card = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = Math.max(CARD_WIDTH, size.width);
                return size;
            }
        };
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
        titleLabel.setMaximumSize(new Dimension(CONTENT_WIDTH, titleLabel.getPreferredSize().height));

        messageArea = new JTextArea(blankToDefault(message, "Please wait..."));
        messageArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageArea.setForeground(new Color(100, 116, 139));
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder());
        messageArea.setRows(messageRows(messageArea.getText()));
        messageArea.setMaximumSize(new Dimension(CONTENT_WIDTH, textAreaHeight(messageArea)));
        messageArea.setPreferredSize(new Dimension(CONTENT_WIDTH, textAreaHeight(messageArea)));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(CONTENT_WIDTH, PROGRESS_HEIGHT));
        progressBar.setMaximumSize(new Dimension(CONTENT_WIDTH, PROGRESS_HEIGHT));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        stopButton = createStopButton(onStop);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(messageArea);
        card.add(Box.createVerticalStrut(18));
        card.add(progressBar);
        if (stopButton != null) {
            card.add(Box.createVerticalStrut(18));
            card.add(stopButton);
        }
        add(card, new GridBagConstraints());
    }

    public static Handle show(Component parent, String title, String message) {
        return show(parent, title, message, null);
    }

    public static Handle show(Component parent, String title, String message, Runnable onStop) {
        Window window = parent instanceof Window
                ? (Window) parent
                : SwingUtilities.getWindowAncestor(parent);
        if (!(window instanceof RootPaneContainer container)) {
            return Handle.noop();
        }

        JRootPane rootPane = container.getRootPane();
        Component previousGlassPane = rootPane.getGlassPane();
        boolean previousVisible = previousGlassPane != null && previousGlassPane.isVisible();
        LoadingOverlay overlay = new LoadingOverlay(title, message, onStop);
        rootPane.setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.requestFocusInWindow();
        return new Handle(rootPane, previousGlassPane, previousVisible, overlay);
    }

    private JButton createStopButton(Runnable onStop) {
        if (onStop == null) {
            return null;
        }
        JButton button = new JButton("Stop");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(UniversalDialogHelper.mediumFont(13));
        button.setForeground(Color.WHITE);
        button.setBackground(UniversalDialogHelper.ERROR_ACCENT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(110, 36));
        button.setMaximumSize(new Dimension(110, 36));
        button.setBorder(BorderFactory.createCompoundBorder(
                UniversalDialogHelper.roundedBorder(UniversalDialogHelper.ERROR_ACCENT, 6, 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        ButtonStateHelper.installRounded(button, 6);
        ButtonStateHelper.setHoverBackground(
                button,
                UniversalDialogHelper.ERROR_ACCENT.darker(),
                UniversalDialogHelper.ERROR_ACCENT.darker()
        );
        ButtonStateHelper.setDisabledColors(button, UniversalDialogHelper.ERROR_ACCENT, Color.WHITE);
        button.addActionListener(event -> {
            button.setText("Stopping...");
            button.setEnabled(false);
            onStop.run();
        });
        return button;
    }

    private void setMessageText(String message) {
        messageArea.setText(blankToDefault(message, "Please wait..."));
        messageArea.setRows(messageRows(messageArea.getText()));
        Dimension messageSize = new Dimension(CONTENT_WIDTH, textAreaHeight(messageArea));
        messageArea.setPreferredSize(messageSize);
        messageArea.setMaximumSize(messageSize);
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

    private static int messageRows(String message) {
        String text = blankToDefault(message, "");
        int explicitLines = text.split("\\R", -1).length;
        int wrappedLines = (int) Math.ceil(text.length() / 58.0);
        return Math.max(2, Math.min(4, Math.max(explicitLines, wrappedLines)));
    }

    private static int textAreaHeight(JTextArea textArea) {
        FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
        return metrics.getHeight() * textArea.getRows() + 4;
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

        public void setTitle(String title) {
            if (overlay == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> overlay.titleLabel.setText(blankToDefault(title, "Working")));
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
