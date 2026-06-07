package com.kgm.ui;

import com.kgm.config.DatabaseConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class DatabaseConnectionStatusView extends JFrame {
    private static final int SLOW_CONNECTION_NOTICE_MS = 1_200;
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color PRIMARY_DARK = new Color(29, 78, 216);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(71, 85, 105);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color ERROR = new Color(185, 28, 28);
    private static final Color ERROR_SOFT = new Color(254, 242, 242);
    private static final Color INFO_SOFT = new Color(239, 246, 255);

    private final Runnable retryAction;
    private final JLabel eyebrow = new JLabel("KGM EX-EMPLOYEES");
    private final JLabel title = new JLabel();
    private final JTextArea message = textArea(15, TEXT_SECONDARY);
    private final JTextArea connection = textArea(13, PRIMARY_DARK);
    private final JLabel retryLabel = new JLabel();
    private final JProgressBar progress = new JProgressBar();
    private final JButton retryButton = new JButton("Retry now");
    private final JButton closeButton = new JButton("Close app");
    private final JButton detailsButton = new JButton("Show technical details");
    private final JTextArea details = textArea(12, new Color(51, 65, 85));
    private final JPanel detailsPanel = new JPanel(new BorderLayout());
    private final Timer slowConnectionTimer;
    private boolean detailsVisible;

    public DatabaseConnectionStatusView(Runnable retryAction) {
        this.retryAction = retryAction;
        slowConnectionTimer = new Timer(SLOW_CONNECTION_NOTICE_MS, event -> showSlowConnectionMessage());
        slowConnectionTimer.setRepeats(false);
        setTitle("Database Connection");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(780, 580));
        setSize(980, 700);
        setLocationRelativeTo(null);
        installFullScreenGuard();
        setContentPane(createContent());
        showChecking();
    }

    public void showChecking() {
        runOnEdt(() -> {
            eyebrow.setForeground(PRIMARY);
            title.setText("Checking database connection");
            message.setText("Please wait while the server and database are checked. The app will continue automatically when the server responds.");
            connection.setText(connectionInfo());
            retryLabel.setText("If this takes longer than usual, the recovery screen will keep checking in the background.");
            progress.setIndeterminate(true);
            progress.setVisible(true);
            retryButton.setEnabled(false);
            detailsPanel.setVisible(false);
            detailsButton.setVisible(false);
            slowConnectionTimer.restart();
            revalidate();
            repaint();
            showWindow();
        });
    }

    public void showFailure(Throwable failure) {
        runOnEdt(() -> {
            slowConnectionTimer.stop();
            eyebrow.setForeground(ERROR);
            title.setText("Database connection issue");
            message.setText("The app cannot reach the configured MySQL server right now. Keep this window open; it will retry automatically and continue when the connection is restored.");
            connection.setText(connectionInfo());
            retryLabel.setText("Auto retry is active. You can also retry manually after checking MySQL, LAN, VPN, or credentials.");
            progress.setIndeterminate(true);
            progress.setVisible(true);
            retryButton.setEnabled(true);
            details.setText(detailsText(failure));
            detailsPanel.setVisible(detailsVisible);
            detailsButton.setVisible(true);
            revalidate();
            repaint();
            showWindow();
        });
    }

    public void closeView() {
        runOnEdt(() -> {
            slowConnectionTimer.stop();
            dispose();
        });
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BACKGROUND);

        JPanel shell = new JPanel();
        shell.setOpaque(false);
        shell.setLayout(new BoxLayout(shell, BoxLayout.Y_AXIS));
        shell.setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(30, 34, 30, 34)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));

        eyebrow.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        connection.setAlignmentX(Component.LEFT_ALIGNMENT);

        progress.setBorder(BorderFactory.createEmptyBorder());
        progress.setPreferredSize(new Dimension(680, 8));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progress.setForeground(PRIMARY);
        progress.setBackground(new Color(219, 234, 254));
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(eyebrow);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(message);
        card.add(Box.createVerticalStrut(18));
        card.add(progress);
        card.add(Box.createVerticalStrut(20));
        card.add(section("Configured connection", connection, INFO_SOFT));
        card.add(Box.createVerticalStrut(14));
        card.add(checklist());
        card.add(Box.createVerticalStrut(18));
        card.add(actions());
        card.add(Box.createVerticalStrut(12));
        card.add(detailsSection());

        shell.add(card);
        root.add(shell, centered());
        return root;
    }

    private JPanel checklist() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(checkItem("MySQL server is running on the configured machine."));
        panel.add(Box.createVerticalStrut(8));
        panel.add(checkItem("Network, VPN, firewall, or server power is not blocking access."));
        panel.add(Box.createVerticalStrut(8));
        panel.add(checkItem(".env host, port, database name, username, and password are correct."));
        return panel;
    }

    private JPanel checkItem(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_SECONDARY);

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(10, 12, 10, 12)));
        JLabel dot = new JLabel("•");
        dot.setForeground(PRIMARY);
        dot.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        panel.add(dot, BorderLayout.WEST);
        panel.add(label, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel actions() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        retryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        retryLabel.setForeground(MUTED);
        retryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        retryButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        retryButton.setForeground(Color.WHITE);
        retryButton.setBackground(PRIMARY);
        retryButton.setBorder(new EmptyBorder(9, 18, 9, 18));
        retryButton.setFocusPainted(false);
        retryButton.addActionListener(event -> {
            retryButton.setEnabled(false);
            if (retryAction != null) {
                retryAction.run();
            }
        });

        closeButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        closeButton.setForeground(new Color(51, 65, 85));
        closeButton.setBackground(new Color(229, 231, 235));
        closeButton.setBorder(new EmptyBorder(9, 18, 9, 18));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(event -> {
            System.exit(0);
        });

        buttonPanel.add(retryButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(closeButton);
        buttonPanel.add(Box.createHorizontalGlue());

        wrapper.add(retryLabel);
        wrapper.add(Box.createVerticalStrut(8));
        wrapper.add(buttonPanel);
        return wrapper;
    }

    private JPanel detailsSection() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailsButton.setForeground(PRIMARY_DARK);
        detailsButton.setBorder(new EmptyBorder(6, 0, 6, 0));
        detailsButton.setContentAreaFilled(false);
        detailsButton.setFocusPainted(false);
        detailsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsButton.addActionListener(event -> toggleDetails());

        details.setEditable(false);
        details.setFocusable(false);
        details.setRows(4);

        JScrollPane scrollPane = new JScrollPane(details);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(680, 120));
        scrollPane.setMinimumSize(new Dimension(680, 120));
        detailsPanel.setBackground(ERROR_SOFT);
        detailsPanel.setBorder(new CompoundBorder(new RoundedBorder(8, new Color(254, 202, 202)), new EmptyBorder(12, 12, 12, 12)));
        detailsPanel.add(scrollPane, BorderLayout.CENTER);
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.setVisible(false);

        wrapper.add(detailsButton);
        wrapper.add(detailsPanel);
        return wrapper;
    }

    private JPanel section(String heading, JTextArea body, Color background) {
        JLabel label = new JLabel(heading);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(12, 14, 12, 14)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(body);
        return panel;
    }

    private void toggleDetails() {
        detailsVisible = !detailsVisible;
        detailsPanel.setVisible(detailsVisible);
        detailsButton.setText(detailsVisible ? "Hide technical details" : "Show technical details");
        revalidate();
        repaint();
    }

    private void showSlowConnectionMessage() {
        title.setText("Database is taking longer than usual");
        message.setText("Checking MySQL server availability, network access, and credentials. The app is still responsive and will continue automatically.");
        retryLabel.setText("No action is needed yet. If this becomes a connection issue, automatic retry will start.");
        revalidate();
        repaint();
    }

    private void showWindow() {
        if (!isVisible()) {
            setVisible(true);
        }
        toFront();
        requestFocus();
    }

    private void installFullScreenGuard() {
        setExtendedState(Frame.MAXIMIZED_BOTH);
        addWindowStateListener(event -> {
            if ((event.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
                SwingUtilities.invokeLater(() -> {
                    setState(Frame.NORMAL);
                    setExtendedState(Frame.MAXIMIZED_BOTH);
                });
            }
        });
    }

    private static JTextArea textArea(int size, Color color) {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, size));
        textArea.setForeground(color);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createEmptyBorder());
        return textArea;
    }

    private static GridBagConstraints centered() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(20, 20, 20, 20);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        return constraints;
    }

    private static String connectionInfo() {
        return DatabaseConfig.username()
                + "@"
                + DatabaseConfig.host()
                + ":"
                + DatabaseConfig.port()
                + "/"
                + DatabaseConfig.databaseName();
    }

    private static String detailsText(Throwable failure) {
        if (failure == null) {
            return "No technical details are available.";
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            if (!builder.isEmpty()) {
                builder.append("\nCaused by: ");
            }
            builder.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                builder.append(" - ").append(current.getMessage());
            }
            current = current.getCause();
        }
        return builder.toString();
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
