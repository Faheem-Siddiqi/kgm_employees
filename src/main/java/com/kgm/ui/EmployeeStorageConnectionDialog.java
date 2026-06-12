package com.kgm.ui;

import com.kgm.config.AppConfig;
import com.kgm.ui.styling.UniversalDialogHelper;
import com.kgm.util.EmployeeStorageUtil;
import com.kgm.util.WindowsNetworkShareConnector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class EmployeeStorageConnectionDialog {
    private static final String TITLE = "Storage Folder Needs Connection";
    private static final Color ERROR_TEXT = new Color(185, 28, 28);
    private static final Color ERROR_SURFACE = new Color(254, 242, 242);
    private static final Color ERROR_BORDER = new Color(254, 202, 202);
    private static final Color ERROR_BADGE = new Color(254, 226, 226);
    private static final Color SUCCESS_TEXT = new Color(21, 128, 61);

    private EmployeeStorageConnectionDialog() {
    }

    public static boolean showUntilConnected(Component parent) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Storage connection dialog must be shown on the event dispatch thread.");
        }
        DialogController controller = new DialogController(parent);
        controller.show();
        return controller.connected;
    }

    public static String uncPath(String serverIp, String shareName) {
        String server = sanitizeServer(serverIp);
        String share = sanitizeShare(shareName);
        if (server.isBlank() || share.isBlank()) {
            return "";
        }
        return "\\\\" + server + "\\" + share;
    }

    private static String sanitizeServer(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.startsWith("\\") || clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        int slash = firstSlash(clean);
        return slash >= 0 ? clean.substring(0, slash).trim() : clean;
    }

    private static String sanitizeShare(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.startsWith("\\") || clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        int slash = firstSlash(clean);
        return slash >= 0 ? clean.substring(0, slash).trim() : clean;
    }

    private static int firstSlash(String value) {
        int backslash = value.indexOf('\\');
        int forward = value.indexOf('/');
        if (backslash < 0) {
            return forward;
        }
        if (forward < 0) {
            return backslash;
        }
        return Math.min(backslash, forward);
    }

    private static final class DialogController {
        private final JDialog dialog;
        private final JTextField serverField = new JTextField(AppConfig.employeeStorageDefaultServer());
        private final JTextField shareField = new JTextField(AppConfig.employeeStorageDefaultShare());
        private final JTextField usernameField = new JTextField(AppConfig.employeeStorageDefaultUsername());
        private final JPasswordField passwordField = new JPasswordField(AppConfig.employeeStorageDefaultPassword());
        private final JLabel statusLabel = new JLabel(" ");
        private JLabel guidanceHeading;
        private JTextArea guidanceDetail;
        private JButton exitButton;
        private JButton retryButton;
        private JButton connectButton;
        private Timer progressTimer;
        private long progressStartedAtMillis;
        private int currentProgress;
        private boolean connected;

        private DialogController(Component parent) {
            Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
            dialog = new JDialog(owner, TITLE, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setResizable(true);
            dialog.setContentPane(content());
            dialog.setPreferredSize(responsiveDialogSize(owner));
            dialog.pack();
            dialog.setMinimumSize(new Dimension(380, 360));
            dialog.setLocationRelativeTo(owner);
            prefillFromConfiguredPath();
        }

        private void show() {
            dialog.setVisible(true);
        }

        private JPanel content() {
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(UniversalDialogHelper.BACKGROUND);
            root.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(UniversalDialogHelper.CARD_BORDER, 8, 1),
                    new EmptyBorder(24, 24, 20, 24)
            ));

            JPanel body = new ViewportWidthPanel();
            body.setOpaque(true);
            body.setBackground(UniversalDialogHelper.BACKGROUND);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(new EmptyBorder(0, 0, 0, 14));
            body.add(header());
            body.add(form());

            JScrollPane scroll = new JScrollPane(body);
            scroll.setBorder(null);
            scroll.setOpaque(true);
            scroll.getViewport().setOpaque(true);
            scroll.getViewport().setBackground(UniversalDialogHelper.BACKGROUND);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

            root.add(scroll, BorderLayout.CENTER);
            root.add(actions(), BorderLayout.SOUTH);
            return root;
        }

        private JPanel header() {
            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel titleRow = new JPanel(new BorderLayout(12, 0));
            titleRow.setOpaque(false);
            titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel title = new JLabel(TITLE);
            title.setFont(UniversalDialogHelper.mediumFont(20));
            title.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton closeButton = closeButton();
            titleRow.add(title, BorderLayout.CENTER);
            titleRow.add(closeButton, BorderLayout.EAST);

            JTextArea message = readOnlyText("""
                    KGM cannot reach the shared employee storage folder right now. Connect it here so employee photos and documents can load and save normally.
                    """);
            message.setAlignmentX(Component.LEFT_ALIGNMENT);

            header.add(titleRow);
            header.add(Box.createVerticalStrut(8));
            header.add(message);
            header.add(Box.createVerticalStrut(14));
            header.add(guidanceBox());
            header.add(Box.createVerticalStrut(18));
            return header;
        }

        private JPanel guidanceBox() {
            JPanel box = new JPanel(new BorderLayout(10, 0));
            box.setOpaque(true);
            box.setBackground(ERROR_SURFACE);
            box.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(ERROR_BORDER, 8, 1),
                    new EmptyBorder(12, 14, 12, 14)
            ));
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            JLabel marker = new JLabel("!", JLabel.CENTER);
            marker.setOpaque(true);
            marker.setBackground(ERROR_BADGE);
            marker.setForeground(ERROR_TEXT);
            marker.setFont(UniversalDialogHelper.mediumFont(13));
            marker.setPreferredSize(new Dimension(22, 22));

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            guidanceHeading = new JLabel("Shared folder details need attention");
            guidanceHeading.setFont(UniversalDialogHelper.mediumFont(13));
            guidanceHeading.setForeground(ERROR_TEXT);
            guidanceHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

            guidanceDetail = readOnlyText(
                    "Confirm the server, shared folder, username, and password, then choose Connect. For error details, scroll below the form."
            );
            guidanceDetail.setFont(UniversalDialogHelper.regularFont(12));
            guidanceDetail.setForeground(ERROR_TEXT);
            guidanceDetail.setBackground(ERROR_SURFACE);
            guidanceDetail.setAlignmentX(Component.LEFT_ALIGNMENT);

            text.add(guidanceHeading);
            text.add(Box.createVerticalStrut(3));
            text.add(guidanceDetail);

            box.add(marker, BorderLayout.WEST);
            box.add(text, BorderLayout.CENTER);
            return box;
        }

        private JPanel form() {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel grid = new JPanel();
            grid.setOpaque(false);
            grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
            grid.setBorder(new EmptyBorder(0, 0, 12, 0));
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);

            styleField(serverField);
            styleField(shareField);
            styleField(usernameField);
            styleField(passwordField);

            addRow(grid, 0, "Server address", serverField, "cmd: ipconfig");
            addRow(grid, 1, "Shared folder", shareField, "");
            addRow(grid, 2, "Username", usernameField, "cmd: whoami");
            addRow(grid, 3, "Password", passwordField, "");

            statusLabel.setFont(UniversalDialogHelper.mediumFont(12));
            statusLabel.setForeground(ERROR_TEXT);

            wrapper.add(grid, BorderLayout.CENTER);
            wrapper.add(statusLabel, BorderLayout.SOUTH);
            return wrapper;
        }

        private JPanel actions() {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.setOpaque(false);

            exitButton = secondaryButton("Close");
            retryButton = secondaryButton("Retry");
            connectButton = primaryButton("Connect");

            exitButton.addActionListener(event -> closeDialog());
            retryButton.addActionListener(event -> retryExistingPath());
            connectButton.addActionListener(event -> connect());

            actions.add(exitButton);
            actions.add(retryButton);
            actions.add(connectButton);
            return actions;
        }

        private void addRow(JPanel panel, int row, String labelText, JTextField field, String hintText) {
            JPanel rowPanel = new JPanel();
            rowPanel.setOpaque(false);
            rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setBorder(new EmptyBorder(0, 0, 14, 0));

            JLabel label = new JLabel(labelText);
            label.setFont(UniversalDialogHelper.mediumFont(13));
            label.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            label.setHorizontalAlignment(JLabel.LEFT);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel fieldBlock = new JPanel();
            fieldBlock.setOpaque(false);
            fieldBlock.setLayout(new BoxLayout(fieldBlock, BoxLayout.Y_AXIS));
            fieldBlock.setAlignmentX(Component.LEFT_ALIGNMENT);
            fieldBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            field.setMinimumSize(new Dimension(0, 34));
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            fieldBlock.add(field);
            if (hintText != null && !hintText.isBlank()) {
                JTextArea hint = readOnlyText(hintText);
                hint.setFont(UniversalDialogHelper.regularFont(11));
                hint.setForeground(UniversalDialogHelper.MUTED_TEXT);
                hint.setBorder(new EmptyBorder(4, 2, 0, 0));
                hint.setAlignmentX(Component.LEFT_ALIGNMENT);
                fieldBlock.add(hint);
            }

            rowPanel.add(label);
            rowPanel.add(Box.createVerticalStrut(5));
            rowPanel.add(fieldBlock);
            panel.add(rowPanel);
        }

        private void connect() {
            String validation = validationMessage();
            if (!validation.isBlank()) {
                showError(validation);
                return;
            }

            char[] password = passwordField.getPassword();
            String expected = expectedFolder();
            String username = usernameField.getText();
            runConnectionWorker(
                    "Connecting to employee storage folder...",
                    "Connecting...",
                    () -> WindowsNetworkShareConnector.connect(expected, username, password),
                    expected,
                    password
            );
        }

        private void retryExistingPath() {
            String expected = expectedFolder();
            if (expected.isBlank()) {
                showError("Enter the server address and shared folder before retrying.");
                return;
            }
            runConnectionWorker(
                    "Checking employee storage folder...",
                    "Checking...",
                    () -> {
                        Path folder = Path.of(expected);
                        if (WindowsNetworkShareConnector.exists(folder)) {
                            return WindowsNetworkShareConnector.ConnectionResult.success();
                        }
                        return WindowsNetworkShareConnector.reconnectStored(expected);
                    },
                    expected,
                    null
            );
        }

        private void runConnectionWorker(String busyMessage, String activeButtonText, ConnectionTask task, String expected, char[] password) {
            setBusy(true, busyMessage, activeButtonText);
            startProgressTimer();
            SwingWorker<WindowsNetworkShareConnector.ConnectionResult, Void> worker = new SwingWorker<>() {
                @Override
                protected WindowsNetworkShareConnector.ConnectionResult doInBackground() {
                    try {
                        setProgress(10);
                        WindowsNetworkShareConnector.ConnectionResult result = task.run();
                        if (!result.connected()) {
                            return result;
                        }
                        setProgress(65);
                        return verifyAndSave(expected);
                    } catch (RuntimeException exception) {
                        return WindowsNetworkShareConnector.ConnectionResult.failed(userMessage(exception));
                    }
                }

                @Override
                protected void done() {
                    try {
                        WindowsNetworkShareConnector.ConnectionResult result = get();
                        if (!result.connected()) {
                            showConnectionFailed(expected, result.message());
                            return;
                        }
                        setProgressStatus(100, "Connected. The app will continue now.");
                        connected = true;
                        showSuccess("Connected. The app will continue now.");
                        dialog.dispose();
                    } catch (Exception exception) {
                        showConnectionFailed(expected, userMessage(exception));
                    } finally {
                        stopProgressTimer();
                        if (password != null) {
                            Arrays.fill(password, '\0');
                            passwordField.setText("");
                        }
                        setBusy(false, null, null);
                    }
                }
            };
            worker.addPropertyChangeListener(event -> {
                if ("progress".equals(event.getPropertyName())) {
                    int progress = (int) event.getNewValue();
                    String label = progress < 65
                            ? "Connecting to employee storage folder..."
                            : "Verifying and saving employee storage folder...";
                    setProgressStatus(progress, label);
                }
            });
            worker.execute();
        }

        private WindowsNetworkShareConnector.ConnectionResult verifyAndSave(String expected) {
            try {
                Path folder = Path.of(expected);
                EmployeeStorageUtil.verifyReadWriteAccess(folder);
                AppConfig.saveEmployeeServerStorageDirectory(expected);
                return WindowsNetworkShareConnector.ConnectionResult.success();
            } catch (IOException exception) {
                String message = exception.getMessage();
                if (message != null && message.contains("does not have write permission")) {
                    return WindowsNetworkShareConnector.ConnectionResult.failed(
                            "KGM reached the folder, but cannot save files there. Ask IT to allow write access for this account, then try again."
                    );
                }
                return WindowsNetworkShareConnector.ConnectionResult.failed(message);
            } catch (RuntimeException exception) {
                return WindowsNetworkShareConnector.ConnectionResult.failed(userMessage(exception));
            }
        }

        private String validationMessage() {
            if (serverField.getText().trim().isEmpty()) {
                return "Enter the server address.";
            }
            if (shareField.getText().trim().isEmpty()) {
                return "Enter the shared folder name.";
            }
            if (usernameField.getText().trim().isEmpty()) {
                return "Enter the username for this shared folder.";
            }
            if (passwordField.getPassword().length == 0) {
                return "Enter the password for this shared folder.";
            }
            return "";
        }

        private String expectedFolder() {
            return uncPath(serverField.getText(), shareField.getText());
        }

        private void showConnectionFailed(String expected) {
            showConnectionFailed(expected, "");
        }

        private void showConnectionFailed(String expected, String commandMessage) {
            showGuidanceError();
            StringBuilder message = new StringBuilder();
            message.append("We could not connect to the shared folder.\n");
            message.append("Check the server address, shared folder, username, password, and office network connection.\n\n");
            message.append("Folder checked:\n").append(expected);

            String cleanCommandMessage = cleanStatusDetails(commandMessage);
            if (!cleanCommandMessage.isBlank()) {
                message.append("\n\nWindows message:\n").append(cleanCommandMessage);
            }

            showError(message.toString());
        }

        private void showError(String message) {
            statusLabel.setForeground(ERROR_TEXT);
            statusLabel.setText("<html><body style='width: " + statusTextWidth() + "px;'>"
                    + "<b>Connection needs attention.</b><br>"
                    + escape(message).replace("\n", "<br>")
                    + "</body></html>");
        }

        private void showSuccess(String message) {
            showGuidanceProgress(100, message);
            statusLabel.setForeground(SUCCESS_TEXT);
            statusLabel.setText(message);
        }

        private void setBusy(boolean busy, String message, String activeButtonText) {
            dialog.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
            if (connectButton != null) {
                connectButton.setEnabled(!busy);
                connectButton.setText(busy && activeButtonText != null ? activeButtonText : "Connect");
            }
            if (retryButton != null) {
                retryButton.setEnabled(!busy);
            }
            if (exitButton != null) {
                exitButton.setEnabled(!busy);
            }
            if (message != null) {
                statusLabel.setText(" ");
                setProgressStatus(0, message);
            }
        }

        private void setProgressStatus(int progress, String message) {
            int boundedProgress = Math.max(0, Math.min(100, progress));
            currentProgress = boundedProgress;
            showGuidanceProgress(boundedProgress, message);
        }

        private void startProgressTimer() {
            stopProgressTimer();
            progressStartedAtMillis = System.currentTimeMillis();
            progressTimer = new Timer(1000, event -> updateElapsedProgress());
            progressTimer.setInitialDelay(1000);
            progressTimer.start();
        }

        private void stopProgressTimer() {
            if (progressTimer != null) {
                progressTimer.stop();
                progressTimer = null;
            }
        }

        private void updateElapsedProgress() {
            if (currentProgress >= 65) {
                return;
            }

            long elapsedSeconds = Math.max(1, (System.currentTimeMillis() - progressStartedAtMillis) / 1000L);
            if (elapsedSeconds >= 5) {
                setProgressStatus(80, "Still waiting for Windows; this will stop soon if the server does not respond.");
            } else if (elapsedSeconds >= 3) {
                setProgressStatus(45, "Waiting for the shared folder server to respond...");
            } else if (elapsedSeconds >= 2) {
                setProgressStatus(30, "Sending username and password to Windows...");
            } else {
                setProgressStatus(15, "Starting Windows shared-folder connection...");
            }
        }

        private void showGuidanceProgress(int progress, String message) {
            if (guidanceHeading != null) {
                guidanceHeading.setText(progress + "% - " + message);
            }
            if (guidanceDetail != null) {
                guidanceDetail.setText("Please wait while KGM checks the shared storage folder. If it fails, the real error will appear below the form.");
            }
        }

        private void showGuidanceError() {
            if (guidanceHeading != null) {
                guidanceHeading.setText("Shared folder details need attention");
            }
            if (guidanceDetail != null) {
                guidanceDetail.setText("Confirm the server, shared folder, username, and password, then choose Connect. For error details, scroll below the form.");
            }
        }

        private void closeDialog() {
            dialog.dispose();
        }

        private void prefillFromConfiguredPath() {
            try {
                String configured = AppConfig.employeeStorageDirectory().toString();
                if (!configured.startsWith("\\\\")) {
                    return;
                }
                String withoutPrefix = configured.substring(2);
                int slash = withoutPrefix.indexOf('\\');
                if (slash <= 0 || slash >= withoutPrefix.length() - 1) {
                    return;
                }
                serverField.setText(withoutPrefix.substring(0, slash));
                shareField.setText(withoutPrefix.substring(slash + 1));
            } catch (RuntimeException exception) {
                showError("Could not read the saved employee storage path.\n" + userMessage(exception));
            }
        }

        private JTextArea readOnlyText(String text) {
            JTextArea area = new JTextArea(text.trim());
            area.setEditable(false);
            area.setFocusable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setColumns(0);
            area.setOpaque(false);
            area.setFont(UniversalDialogHelper.regularFont(13));
            area.setForeground(UniversalDialogHelper.TEXT_SECONDARY);
            area.setBorder(null);
            area.setAlignmentX(Component.LEFT_ALIGNMENT);
            return area;
        }

        private void styleField(JTextField field) {
            field.setFont(UniversalDialogHelper.regularFont(13));
            field.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            field.setBackground(Color.WHITE);
            field.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(UniversalDialogHelper.CARD_BORDER, 8, 1),
                    new EmptyBorder(7, 10, 7, 10)
            ));
        }

        private JButton primaryButton(String text) {
            JButton button = baseButton(text);
            button.setBackground(UniversalDialogHelper.PRIMARY);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(UniversalDialogHelper.PRIMARY, 8, 1),
                    new EmptyBorder(8, 16, 8, 16)
            ));
            return button;
        }

        private JButton secondaryButton(String text) {
            JButton button = baseButton(text);
            button.setBackground(Color.WHITE);
            button.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(UniversalDialogHelper.CARD_BORDER, 8, 1),
                    new EmptyBorder(8, 14, 8, 14)
            ));
            return button;
        }

        private JButton closeButton() {
            JButton button = new JButton("X");
            button.setPreferredSize(new Dimension(32, 32));
            button.setMaximumSize(new Dimension(32, 32));
            button.setToolTipText("Close dialog");
            button.setBackground(UniversalDialogHelper.BACKGROUND);
            button.setForeground(UniversalDialogHelper.MUTED_TEXT);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(UniversalDialogHelper.CARD_BORDER, 8, 1),
                    new EmptyBorder(4, 10, 5, 10)
            ));
            button.setFont(new Font(UniversalDialogHelper.FONT_FAMILY, Font.BOLD, 12));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(event -> closeDialog());
            return button;
        }

        private JButton baseButton(String text) {
            JButton button = new JButton(text);
            button.setFocusPainted(false);
            button.setFont(new Font(UniversalDialogHelper.FONT_FAMILY, Font.BOLD, 12));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return button;
        }

        private String escape(String value) {
            return value == null ? "" : value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }

        private int statusTextWidth() {
            int currentWidth = dialog.getWidth() > 0 ? dialog.getWidth() : 460;
            return Math.max(260, Math.min(480, currentWidth - 120));
        }

        private Dimension responsiveDialogSize(Window owner) {
            Dimension screen = owner == null
                    ? java.awt.Toolkit.getDefaultToolkit().getScreenSize()
                    : owner.getGraphicsConfiguration().getBounds().getSize();
            int width = Math.min(620, Math.max(380, screen.width - 96));
            int height = Math.min(560, Math.max(360, screen.height - 120));
            return new Dimension(width, height);
        }

        private String cleanStatusDetails(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            StringBuilder clean = new StringBuilder();
            for (String line : value.replace("\r", "\n").split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) {
                    continue;
                }
                if (!clean.isEmpty()) {
                    clean.append("\n");
                }
                clean.append(trimmed);
            }
            return clean.toString();
        }

        private String userMessage(Exception exception) {
            if (exception == null) {
                return "Unknown error.";
            }
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            Throwable cause = exception.getCause();
            if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
                    && !message.contains(cause.getMessage())) {
                message = message + "\n" + cause.getMessage();
            }
            return message;
        }

        private static final class ViewportWidthPanel extends JPanel implements javax.swing.Scrollable {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return getPreferredSize();
            }

            @Override
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 16;
            }

            @Override
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
                return Math.max(16, visibleRect.height - 16);
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return false;
            }
        }

        @FunctionalInterface
        private interface ConnectionTask {
            WindowsNetworkShareConnector.ConnectionResult run();
        }
    }
}
