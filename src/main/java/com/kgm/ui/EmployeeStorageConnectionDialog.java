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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class EmployeeStorageConnectionDialog {
    private static final String TITLE = "Employee Storage Folder Not Connected";
    private static final String DEFAULT_SERVER = "192.168.2.93";
    private static final String DEFAULT_SHARE = "employees";
    private static final Color ERROR_TEXT = new Color(185, 28, 28);
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
        private final JTextField serverField = new JTextField(DEFAULT_SERVER);
        private final JTextField shareField = new JTextField(DEFAULT_SHARE);
        private final JTextField usernameField = new JTextField();
        private final JPasswordField passwordField = new JPasswordField();
        private final JLabel statusLabel = new JLabel(" ");
        private JButton exitButton;
        private JButton retryButton;
        private JButton connectButton;
        private boolean connected;

        private DialogController(Component parent) {
            Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
            dialog = new JDialog(owner, TITLE, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setContentPane(content());
            dialog.pack();
            dialog.setMinimumSize(new Dimension(520, 460));
            dialog.setLocationRelativeTo(owner);
            prefillFromConfiguredPath();
        }

        private void show() {
            dialog.setVisible(true);
        }

        private JPanel content() {
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                    UniversalDialogHelper.roundedBorder(UniversalDialogHelper.CARD_BORDER, 8, 1),
                    new EmptyBorder(24, 24, 20, 24)
            ));

            root.add(header(), BorderLayout.NORTH);
            root.add(form(), BorderLayout.CENTER);
            root.add(actions(), BorderLayout.SOUTH);
            return root;
        }

        private JPanel header() {
            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

            JLabel title = new JLabel(TITLE);
            title.setFont(UniversalDialogHelper.mediumFont(18));
            title.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JTextArea message = readOnlyText("""
                    The application cannot access the employee storage folder on the LAN.

                    This folder is required to load and save employee images/documents.

                    Please confirm the shared folder details below. The app will try to connect automatically.
                    """);
            message.setAlignmentX(Component.LEFT_ALIGNMENT);

            header.add(title);
            header.add(Box.createVerticalStrut(8));
            header.add(message);
            header.add(Box.createVerticalStrut(16));
            return header;
        }

        private JPanel form() {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);

            JPanel grid = new JPanel(new GridBagLayout());
            grid.setOpaque(false);
            grid.setBorder(new EmptyBorder(0, 0, 12, 0));

            styleField(serverField);
            styleField(shareField);
            styleField(usernameField);
            styleField(passwordField);

            addRow(grid, 0, "Server IP Address", serverField, "Example: 192.168.2.93");
            addRow(grid, 1, "Shared Folder Name", shareField, "Example: employees");
            addRow(grid, 2, "Username", usernameField, "Example: KMLGPK\\Attiq.Mughal or kgm534");
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

            exitButton = secondaryButton("Exit Application");
            retryButton = secondaryButton("Retry");
            connectButton = primaryButton("Connect");

            exitButton.addActionListener(event -> System.exit(0));
            retryButton.addActionListener(event -> retryExistingPath());
            connectButton.addActionListener(event -> connect());

            actions.add(exitButton);
            actions.add(retryButton);
            actions.add(connectButton);
            return actions;
        }

        private void addRow(JPanel panel, int row, String labelText, JTextField field, String hintText) {
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = GridBagConstraints.NORTHWEST;
            labelConstraints.insets = new Insets(0, 0, 14, 14);

            JLabel label = new JLabel(labelText);
            label.setFont(UniversalDialogHelper.mediumFont(13));
            label.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            panel.add(label, labelConstraints);

            JPanel fieldBlock = new JPanel();
            fieldBlock.setOpaque(false);
            fieldBlock.setLayout(new BoxLayout(fieldBlock, BoxLayout.Y_AXIS));
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            fieldBlock.add(field);
            if (hintText != null && !hintText.isBlank()) {
                JLabel hint = new JLabel(hintText);
                hint.setFont(UniversalDialogHelper.regularFont(11));
                hint.setForeground(UniversalDialogHelper.MUTED_TEXT);
                hint.setBorder(new EmptyBorder(4, 2, 0, 0));
                fieldBlock.add(hint);
            }

            GridBagConstraints fieldConstraints = new GridBagConstraints();
            fieldConstraints.gridx = 1;
            fieldConstraints.gridy = row;
            fieldConstraints.weightx = 1;
            fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
            fieldConstraints.insets = new Insets(0, 0, 14, 0);
            panel.add(fieldBlock, fieldConstraints);
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
                    () -> WindowsNetworkShareConnector.connect(expected, username, password),
                    expected,
                    password
            );
        }

        private void retryExistingPath() {
            String expected = expectedFolder();
            if (expected.isBlank()) {
                showError("Server IP and shared folder name are required before retry.");
                return;
            }
            runConnectionWorker(
                    "Checking employee storage folder...",
                    () -> WindowsNetworkShareConnector.exists(Path.of(expected))
                            ? WindowsNetworkShareConnector.ConnectionResult.success()
                            : WindowsNetworkShareConnector.ConnectionResult.failed("Folder is not accessible."),
                    expected,
                    null
            );
        }

        private void runConnectionWorker(String busyMessage, ConnectionTask task, String expected, char[] password) {
            setBusy(true, busyMessage);
            new SwingWorker<WindowsNetworkShareConnector.ConnectionResult, Void>() {
                @Override
                protected WindowsNetworkShareConnector.ConnectionResult doInBackground() {
                    return task.run();
                }

                @Override
                protected void done() {
                    try {
                        WindowsNetworkShareConnector.ConnectionResult result = get();
                        if (!result.connected()) {
                            showConnectionFailed(expected);
                            return;
                        }
                        finishIfAccessible(expected);
                    } catch (Exception exception) {
                        showConnectionFailed(expected);
                    } finally {
                        if (password != null) {
                            Arrays.fill(password, '\0');
                            passwordField.setText("");
                        }
                        setBusy(false, null);
                    }
                }
            }.execute();
        }

        private boolean finishIfAccessible(String expected) {
            Path folder = Path.of(expected);
            try {
                EmployeeStorageUtil.verifyReadWriteAccess(folder);
                AppConfig.saveEmployeeServerStorageDirectory(expected);
                connected = true;
                showSuccess("Connected. The app will continue now.");
                dialog.dispose();
                return true;
            } catch (IOException exception) {
                String message = exception.getMessage();
                if (message != null && message.contains("does not have write permission")) {
                    showError("The folder is connected, but the app does not have write permission. Please contact IT or check shared folder permissions.\n\nExpected folder:\n" + expected);
                } else {
                    showConnectionFailed(expected);
                }
                return false;
            }
        }

        private String validationMessage() {
            if (serverField.getText().trim().isEmpty()) {
                return "Server IP cannot be empty.";
            }
            if (shareField.getText().trim().isEmpty()) {
                return "Shared folder name cannot be empty.";
            }
            if (usernameField.getText().trim().isEmpty()) {
                return "Username cannot be empty.";
            }
            if (passwordField.getPassword().length == 0) {
                return "Password cannot be empty.";
            }
            return "";
        }

        private String expectedFolder() {
            return uncPath(serverField.getText(), shareField.getText());
        }

        private void showConnectionFailed(String expected) {
            showError("Connection failed. Please check the server IP, shared folder name, username, password, and LAN connection.\n\nExpected folder:\n" + expected);
        }

        private void showError(String message) {
            statusLabel.setForeground(ERROR_TEXT);
            statusLabel.setText("<html>" + escape(message).replace("\n", "<br>") + "</html>");
        }

        private void showSuccess(String message) {
            statusLabel.setForeground(SUCCESS_TEXT);
            statusLabel.setText(message);
        }

        private void setBusy(boolean busy, String message) {
            dialog.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
            if (connectButton != null) {
                connectButton.setEnabled(!busy);
            }
            if (retryButton != null) {
                retryButton.setEnabled(!busy);
            }
            if (exitButton != null) {
                exitButton.setEnabled(!busy);
            }
            if (message != null) {
                statusLabel.setForeground(UniversalDialogHelper.MUTED_TEXT);
                statusLabel.setText(message);
            }
        }

        private void prefillFromConfiguredPath() {
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
        }

        private JTextArea readOnlyText(String text) {
            JTextArea area = new JTextArea(text.trim());
            area.setEditable(false);
            area.setFocusable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setOpaque(false);
            area.setFont(UniversalDialogHelper.regularFont(13));
            area.setForeground(UniversalDialogHelper.TEXT_SECONDARY);
            area.setBorder(null);
            return area;
        }

        private void styleField(JTextField field) {
            field.setFont(UniversalDialogHelper.regularFont(13));
            field.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
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

        @FunctionalInterface
        private interface ConnectionTask {
            WindowsNetworkShareConnector.ConnectionResult run();
        }
    }
}
