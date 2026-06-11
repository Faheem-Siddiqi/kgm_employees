package com.kgm.ui;

import com.kgm.StartupController;
import com.kgm.config.AppConfig;
import com.kgm.config.DatabaseConfig;
import com.kgm.config.DatabaseConnection;

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
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.sql.SQLException;

public class DatabaseSetupView extends JFrame {
    private static final String MYSQL_DOWNLOAD_URL = "https://dev.mysql.com/downloads/windows/installer/";
    private static final int AUTO_RETRY_INITIAL_DELAY_MS = 3500;
    private static final int AUTO_RETRY_INTERVAL_MS = 10000;
    private static final Color BACKGROUND = new Color(248, 250, 252);
    private static final Color CARD = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_SECONDARY = new Color(71, 85, 105);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color PRIMARY_DARK = new Color(29, 78, 216);
    private static final Color WARNING_BACKGROUND = new Color(255, 247, 237);
    private static final Color WARNING_BORDER = new Color(253, 186, 116);
    private static final Color WARNING_TEXT = new Color(154, 52, 18);

    private static DatabaseSetupView activeView;

    private final JLabel statusLabel = new JLabel("Waiting for setup");
    private final JTextArea errorText = plainText("", new Font("Segoe UI", Font.PLAIN, 13), WARNING_TEXT);
    private final JTextArea technicalText = codeArea("");
    private final JScrollPane technicalScroll = new JScrollPane(technicalText);
    private final JToggleButton technicalToggle = new JToggleButton("Show technical details");
    private final JProgressBar retryProgress = new JProgressBar();
    private final JButton retryButton = new ActionButton("Retry Connection", true);
    private Runnable onConnected;
    private SwingWorker<Boolean, Void> retryWorker;
    private Timer autoRetryTimer;

    public DatabaseSetupView(RuntimeException startupFailure) {
        this(startupFailure, StartupController::showLoginWindow, true);
    }

    private DatabaseSetupView(RuntimeException startupFailure, Runnable onConnected, boolean exitOnClose) {
        this.onConnected = onConnected;
        activeView = this;
        setTitle("KGM Ex-Employees Setup");
        setSize(1040, 720);
        setMinimumSize(new Dimension(780, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(exitOnClose ? EXIT_ON_CLOSE : DISPOSE_ON_CLOSE);
        installFullScreenGuard();
        setContentPane(createContent(startupFailure));
        updateError(startupFailure);
        startAutoRetry();
    }

    public static void showStartupFailure(RuntimeException failure) {
        showConnectionFailure(failure, StartupController::showLoginWindow, true);
    }

    public static void showConnectionFailure(Throwable failure) {
        showConnectionFailure(failure, null, false);
    }

    public static boolean showIfConnectionFailure(Throwable failure) {
        if (!DatabaseConnection.isLikelyConnectionFailure(failure)) {
            return false;
        }
        showConnectionFailure(failure);
        return true;
    }

    private static void showConnectionFailure(Throwable failure, Runnable onConnected, boolean exitOnClose) {
        Runnable show = () -> {
            RuntimeException runtimeFailure = runtimeFailure(failure);
            if (activeView != null && activeView.isDisplayable()) {
                activeView.setRecoveryAction(onConnected);
                activeView.updateError(runtimeFailure);
                activeView.setVisible(true);
                activeView.keepFullScreen();
                activeView.toFront();
                activeView.requestFocus();
                return;
            }
            new DatabaseSetupView(runtimeFailure, onConnected, exitOnClose).setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
    }

    private static RuntimeException runtimeFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Database or setup check failed.", failure);
    }

    private JScrollPane createContent(RuntimeException startupFailure) {
        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(28, 34, 28, 34));

        page.add(createHeader());
        page.add(Box.createVerticalStrut(18));
        page.add(createStatusPanel(startupFailure));
        page.add(Box.createVerticalStrut(14));
        page.add(createAutoCheckPanel());
        page.add(Box.createVerticalStrut(14));
        page.add(createChecklistPanel());
        page.add(Box.createVerticalStrut(14));
        page.add(createFooter());

        JScrollPane scrollPane = new JScrollPane(page);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(scrollPane, BorderLayout.CENTER);
        JScrollPane wrapper = new JScrollPane(root);
        wrapper.setBorder(null);
        wrapper.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return wrapper;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("KGM EX-EMPLOYEES");
        eyebrow.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        eyebrow.setForeground(PRIMARY);

        JLabel title = new JLabel("Setup required");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Check .env, MySQL, and employee storage. The app will continue to login when setup is ready.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);

        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(8));
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);

        header.add(copy, BorderLayout.CENTER);
        header.add(new StatusPill("Auto-checking"), BorderLayout.EAST);
        return header;
    }

    private JPanel createStatusPanel(RuntimeException startupFailure) {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 18));
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(22, 22, 22, 22)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Configured connection");
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        label.setForeground(MUTED);

        JTextArea connection = plainText(connectionInfo(), new Font("Segoe UI", Font.BOLD, 18), TEXT_PRIMARY);
        connection.setFocusable(false);

        statusLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        statusLabel.setForeground(WARNING_TEXT);

        top.add(label);
        top.add(Box.createVerticalStrut(8));
        top.add(connection);
        top.add(Box.createVerticalStrut(12));
        top.add(statusLabel);

        JPanel errorPanel = new JPanel(new BorderLayout(0, 10));
        errorPanel.setBackground(WARNING_BACKGROUND);
        errorPanel.setBorder(new CompoundBorder(new RoundedBorder(8, WARNING_BORDER), new EmptyBorder(16, 16, 16, 16)));

        JLabel errorTitle = new JLabel("What needs attention");
        errorTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        errorTitle.setForeground(WARNING_TEXT);
        errorText.setText(errorMessage(startupFailure));

        errorPanel.add(errorTitle, BorderLayout.NORTH);
        errorPanel.add(errorText, BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(errorPanel);
        center.add(Box.createVerticalStrut(10));
        center.add(createTechnicalDetailsPanel());

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        actions.add(retryButton, gbc);

        retryProgress.setIndeterminate(true);
        retryProgress.setVisible(false);
        retryProgress.setPreferredSize(new Dimension(0, 6));
        retryProgress.setBorder(BorderFactory.createEmptyBorder());
        retryProgress.setBackground(new Color(219, 234, 254));
        retryProgress.setForeground(PRIMARY);
        gbc.gridy = 1;
        actions.add(retryProgress, gbc);

        JButton mysqlButton = new ActionButton("Download MySQL", false);
        mysqlButton.addActionListener(event -> openUrl(MYSQL_DOWNLOAD_URL));
        gbc.gridy = 2;
        actions.add(mysqlButton, gbc);

        retryButton.addActionListener(event -> retryConnection(true));

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTechnicalDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        technicalToggle.setFocusPainted(false);
        technicalToggle.setContentAreaFilled(false);
        technicalToggle.setBorderPainted(false);
        technicalToggle.setOpaque(false);
        technicalToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        technicalToggle.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        technicalToggle.setForeground(PRIMARY_DARK);
        technicalToggle.setHorizontalAlignment(SwingConstants.LEFT);
        technicalToggle.setBorder(new EmptyBorder(2, 0, 2, 0));
        technicalToggle.addActionListener(event -> toggleTechnicalDetails());

        technicalScroll.setVisible(false);
        technicalScroll.setPreferredSize(new Dimension(0, 150));
        technicalScroll.setBorder(new RoundedBorder(8, BORDER));

        panel.add(technicalToggle, BorderLayout.NORTH);
        panel.add(technicalScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAutoCheckPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(14, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        panel.setBorder(new CompoundBorder(new RoundedBorder(8, new Color(191, 219, 254)), new EmptyBorder(16, 18, 16, 18)));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Automatic recovery is running");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_PRIMARY);
        JLabel detail = new JLabel("This screen checks setup quietly every few seconds and closes when the app can continue.");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detail.setForeground(TEXT_SECONDARY);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(detail);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setPreferredSize(new Dimension(180, 8));
        progress.setBorder(BorderFactory.createEmptyBorder());
        progress.setForeground(PRIMARY);
        progress.setBackground(new Color(232, 244, 255));

        panel.add(text, BorderLayout.CENTER);
        panel.add(progress, BorderLayout.EAST);
        return panel;
    }

    private JPanel createChecklistPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Setup checklist");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detail = new JLabel("Use these steps on a new PC, after a clone, or when MySQL credentials change.");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detail.setForeground(TEXT_SECONDARY);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(title);
        wrapper.add(Box.createVerticalStrut(4));
        wrapper.add(detail);
        wrapper.add(Box.createVerticalStrut(14));
        wrapper.add(stepCard("1", "Install and start MySQL Server", "Use MySQL Server 8.0 or newer. Keep port 3306 unless your server uses a custom port.", MYSQL_DOWNLOAD_URL, "Download MySQL", MYSQL_DOWNLOAD_URL));
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(stepCard("2", "Create database access", "Run this SQL in MySQL Workbench or MySQL command line if the user/database is missing.", sqlSetup(), "Copy SQL", sqlSetup()));
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(stepCard("3", "Create .env", "Copy .env.example to .env in the project root, or keep config\\.env beside the EXE after packaging.", envSetup(), "Copy .env values", envSetup()));
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(stepCard("4", "Build and run", "Run these commands from the cloned project folder after Java, Maven, MySQL, and .env are ready.", runCommands(), "Copy commands", runCommands()));
        return wrapper;
    }

    private JPanel stepCard(String number, String title, String detail, String codeText, String actionText, String actionValue) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(14, 12));
        card.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(18, 18, 18, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        heading.add(new StepNumber(number), BorderLayout.WEST);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        JTextArea detailText = plainText(detail, new Font("Segoe UI", Font.PLAIN, 13), TEXT_SECONDARY);
        copy.add(titleLabel);
        copy.add(Box.createVerticalStrut(3));
        copy.add(detailText);
        heading.add(copy, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        bottom.add(codeArea(codeText), BorderLayout.CENTER);
        JButton action = new SmallButton(actionText);
        action.addActionListener(event -> {
            if (actionValue.startsWith("https://")) {
                openUrl(actionValue);
            } else {
                copyToClipboard(actionValue);
            }
        });
        bottom.add(action, BorderLayout.EAST);

        card.add(heading, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel note = new JLabel("After setup is ready, click Retry Connection or wait for the automatic check.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        note.setForeground(TEXT_SECONDARY);

        JButton close = new SmallButton("Close App");
        close.addActionListener(event -> System.exit(0));

        footer.add(note, BorderLayout.WEST);
        footer.add(close, BorderLayout.EAST);
        return footer;
    }

    private void startAutoRetry() {
        autoRetryTimer = new Timer(AUTO_RETRY_INTERVAL_MS, event -> retryConnection(false));
        autoRetryTimer.setInitialDelay(AUTO_RETRY_INITIAL_DELAY_MS);
        autoRetryTimer.start();
    }

    private void retryConnection(boolean userInitiated) {
        if (retryWorker != null && !retryWorker.isDone()) {
            return;
        }

        statusLabel.setText(userInitiated ? "Checking setup and MySQL connection..." : "Checking setup quietly...");
        if (userInitiated) {
            retryButton.setEnabled(false);
            retryButton.setText("Checking...");
            retryProgress.setVisible(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        retryWorker = new SwingWorker<>() {
            private RuntimeException failure;

            @Override
            protected Boolean doInBackground() {
                try {
                    StartupController.reconnectDatabase();
                    return true;
                } catch (RuntimeException exception) {
                    failure = exception;
                    return false;
                }
            }

            @Override
            protected void done() {
                if (userInitiated) {
                    retryButton.setEnabled(true);
                    retryButton.setText("Retry Connection");
                    retryProgress.setVisible(false);
                    setCursor(Cursor.getDefaultCursor());
                }
                if (Boolean.TRUE.equals(result())) {
                    statusLabel.setText("Setup complete");
                    if (autoRetryTimer != null) {
                        autoRetryTimer.stop();
                    }
                    Runnable recovery = onConnected;
                    dispose();
                    if (recovery != null) {
                        recovery.run();
                    }
                } else {
                    statusLabel.setText("Setup still needs attention. Automatic checks continue.");
                    if (userInitiated) {
                        updateError(failure);
                    }
                }
                retryWorker = null;
            }

            private Boolean result() {
                try {
                    return get();
                } catch (Exception exception) {
                    return false;
                }
            }
        };
        retryWorker.execute();
    }

    private void updateError(RuntimeException failure) {
        errorText.setText(errorMessage(failure));
        errorText.setCaretPosition(0);
        technicalText.setText(technicalDetails(failure));
        technicalText.setCaretPosition(0);
    }

    private String errorMessage(Throwable failure) {
        return "Current .env path checked: " + AppConfig.activeDotEnvPath()
                + "\n" + userFriendlyHint(failure);
    }

    private String userFriendlyHint(Throwable failure) {
        Throwable root = rootCause(failure);
        String message = root == null ? "" : String.valueOf(root.getMessage());
        String lower = message.toLowerCase();
        if (lower.contains(".env") || lower.contains("kgm_admin")) {
            return "Create .env from .env.example and set database plus login values, then retry.";
        }
        if (root instanceof SQLException sqlException && sqlException.getErrorCode() == 1045) {
            return "MySQL rejected the username or password. Check KGM_DB_USER and KGM_DB_PASSWORD, then retry.";
        }
        if (lower.contains("communications link") || lower.contains("connection") || lower.contains("socket")) {
            return "Start MySQL Server and confirm this PC can reach " + DatabaseConfig.host() + ":" + DatabaseConfig.port() + ".";
        }
        if (lower.contains("employee storage")) {
            return "Check KGM_EMPLOYEE_STORAGE_DIR. It may be a local folder or a LAN path that this Windows user can access.";
        }
        return "Follow the setup checklist below, then retry the connection.";
    }

    private String technicalDetails(Throwable failure) {
        if (failure == null) {
            return "No technical details were provided.";
        }
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        failure.printStackTrace(printer);
        printer.flush();
        return writer.toString();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void toggleTechnicalDetails() {
        boolean showing = technicalToggle.isSelected();
        technicalToggle.setText(showing ? "Hide technical details" : "Show technical details");
        technicalScroll.setVisible(showing);
        revalidate();
        repaint();
    }

    private void setRecoveryAction(Runnable recoveryAction) {
        if (recoveryAction != null) {
            this.onConnected = recoveryAction;
        }
    }

    private void installFullScreenGuard() {
        setExtendedState(Frame.MAXIMIZED_BOTH);
        addWindowStateListener(event -> {
            if ((event.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
                SwingUtilities.invokeLater(this::keepFullScreen);
            }
        });
    }

    private void keepFullScreen() {
        setState(Frame.NORMAL);
        setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    @Override
    public void dispose() {
        if (autoRetryTimer != null) {
            autoRetryTimer.stop();
        }
        if (activeView == this) {
            activeView = null;
        }
        super.dispose();
    }

    private String connectionInfo() {
        return DatabaseConfig.username()
                + "@"
                + DatabaseConfig.host()
                + ":"
                + DatabaseConfig.port()
                + "/"
                + DatabaseConfig.databaseName();
    }

    private static String sqlSetup() {
        String database = DatabaseConfig.escapedDatabaseName();
        return """
                CREATE USER IF NOT EXISTS 'kgm_ex_user'@'localhost' IDENTIFIED BY 'change_me';
                GRANT CREATE ON *.* TO 'kgm_ex_user'@'localhost';
                GRANT ALL PRIVILEGES ON `%s`.* TO 'kgm_ex_user'@'localhost';
                FLUSH PRIVILEGES;
                """.formatted(database);
    }

    private static String envSetup() {
        return """
                KGM_DB_HOST=127.0.0.1
                KGM_DB_PORT=3306
                KGM_DB_NAME=kgm_ex_employees
                KGM_DB_USER=kgm_ex_user
                KGM_DB_PASSWORD=change_me

                KGM_ADMIN_USER=admin
                KGM_ADMIN_PASSWORD=change_this_password
                FIELD_SETTINGS=change_this_field_settings_password

                KGM_EMPLOYEE_STORAGE_ON_SERVER=false
                KGM_EMPLOYEE_STORAGE_DIR=resources/employees
                KGM_DOCUMENT_UPLOAD_MAX_BYTES=600000
                BULK_IMPORT_COMPRESSION=true
                KGM_LONG_SERVICE_TIMEOUT_MINUTES=15
                """;
    }

    private static String runCommands() {
        return """
                Copy-Item .env.example .env
                mvn exec:java
                mvn package
                powershell -ExecutionPolicy Bypass -File .\\build-exe.ps1 -OutputDir "D:\\KGM-eX-Employees-App" -CleanTarget
                """;
    }

    private static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(CARD);
        return panel;
    }

    private static JTextArea plainText(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(font);
        area.setForeground(color);
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    private static JTextArea codeArea(String text) {
        JTextArea area = new JTextArea(text == null ? "" : text.trim());
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setForeground(new Color(49, 58, 70));
        area.setBackground(new Color(248, 250, 252));
        area.setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(10, 10, 10, 10)));
        return area;
    }

    private void openUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                copyToClipboard(url);
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception exception) {
            copyToClipboard(url);
        }
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.trim()), null);
        statusLabel.setText("Copied to clipboard");
    }

    private static class ActionButton extends JButton {
        private final boolean primary;
        private boolean hovered;

        ActionButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
            setForeground(primary ? Color.WHITE : PRIMARY);
            setBorder(new EmptyBorder(12, 16, 12, 16));
            setHorizontalAlignment(SwingConstants.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary
                    ? (hovered && isEnabled() ? PRIMARY_DARK : PRIMARY)
                    : (hovered && isEnabled() ? new Color(239, 246, 255) : Color.WHITE);
            g2.setColor(isEnabled() ? fill : new Color(229, 231, 235));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(primary ? fill : BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class SmallButton extends JButton {
        SmallButton(String text) {
            super(text);
            setFocusPainted(false);
            setForeground(PRIMARY);
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(new RoundedBorder(8, BORDER), new EmptyBorder(8, 12, 8, 12)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static class StatusPill extends JLabel {
        StatusPill(String text) {
            super(text);
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            setForeground(PRIMARY_DARK);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(8, 14, 8, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(219, 234, 254));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class StepNumber extends JLabel {
        StepNumber(String number) {
            super(number, SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(PRIMARY);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(graphics);
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
