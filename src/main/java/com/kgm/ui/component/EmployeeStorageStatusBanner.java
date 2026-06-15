package com.kgm.ui.component;

import com.kgm.ui.EmployeeStorageConnectionDialog;
import com.kgm.ui.styling.UniversalDialogHelper;
import com.kgm.util.EmployeeStorageConnectionMonitor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

public class EmployeeStorageStatusBanner extends JPanel
        implements EmployeeStorageConnectionMonitor.StorageStatusListener {
    private static final int PAGE_SIDE_MARGIN = 28;
    private static final Color WARNING_BACKGROUND = new Color(255, 251, 235);
    private static final Color WARNING_BORDER = new Color(253, 230, 138);
    private static final Color WARNING_TEXT = new Color(120, 53, 15);
    private static final Color ACTION_TEXT = new Color(146, 64, 14);

    private final JFrame owner;
    private final JLabel messageLabel = new JLabel();
    private final JButton retryButton = textButton("Retry");
    private final JButton configButton = textButton("Config");

    public EmployeeStorageStatusBanner(JFrame owner) {
        super(new GridBagLayout());
        this.owner = owner;
        setOpaque(true);
        setBackground(WARNING_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                UniversalDialogHelper.roundedBorder(WARNING_BORDER, 8, 1),
                new EmptyBorder(10, 16, 10, 16)
        ));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        messageLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        messageLabel.setForeground(WARNING_TEXT);
        messageLabel.setHorizontalAlignment(JLabel.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        actionRow.setOpaque(false);
        actionRow.add(retryButton);
        // actionRow.add(Box.createHorizontalStrut(2));
        actionRow.add(configButton);

        GridBagConstraints messageConstraints = new GridBagConstraints();
        messageConstraints.gridx = 0;
        messageConstraints.gridy = 0;
        messageConstraints.weightx = 1.0;
        messageConstraints.anchor = GridBagConstraints.CENTER;
        messageConstraints.fill = GridBagConstraints.HORIZONTAL;
        messageConstraints.insets = new Insets(0, 0, 0, 18);
        add(messageLabel, messageConstraints);

        GridBagConstraints actionConstraints = new GridBagConstraints();
        actionConstraints.gridx = 1;
        actionConstraints.gridy = 0;
        actionConstraints.anchor = GridBagConstraints.CENTER;
        add(actionRow, actionConstraints);
        setVisible(false);

        retryButton.addActionListener(event -> EmployeeStorageConnectionMonitor.retrySilently());
        configButton.addActionListener(event -> showConfigurationDialog());

        EmployeeStorageConnectionMonitor.addListener(this);
        EmployeeStorageConnectionMonitor.Status currentStatus = EmployeeStorageConnectionMonitor.status();
        if (currentStatus == EmployeeStorageConnectionMonitor.Status.UNKNOWN
                || currentStatus == EmployeeStorageConnectionMonitor.Status.CONNECTED) {
            EmployeeStorageConnectionMonitor.startSilently();
        }
    }

    public static JPanel stickyRow(EmployeeStorageStatusBanner banner) {
        return stickyRow(banner, 12);
    }

    public static JPanel stickyRow(EmployeeStorageStatusBanner banner, int bottomMargin) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(12, PAGE_SIDE_MARGIN, Math.max(0, bottomMargin), PAGE_SIDE_MARGIN));
        row.setVisible(banner != null && banner.isVisible());
        row.add(banner, BorderLayout.CENTER);
        return row;
    }

    public void dispose() {
        EmployeeStorageConnectionMonitor.removeListener(this);
    }

    @Override
    public void statusChanged(EmployeeStorageConnectionMonitor.Status status) {
        switch (status) {
            case CHECKING -> {
                if (isVisible()) {
                    showRetrying();
                } else {
                    hideBanner();
                }
            }
            case DISCONNECTED -> showDisconnected();
            case CONNECTED, UNKNOWN -> hideBanner();
        }
    }

    private void showRetrying() {
        messageLabel.setText("Employee storage folder is unavailable. Document operations may not work.");
        retryButton.setText("Retrying..");
        retryButton.setEnabled(false);
        configButton.setEnabled(true);
        setVisible(true);
        refreshParent();
    }

    private void showDisconnected() {
        messageLabel.setText("Employee storage folder is unavailable. Document operations may not work");
        retryButton.setText("Retry");
        retryButton.setEnabled(true);
        configButton.setEnabled(true);
        setVisible(true);
        refreshParent();
    }

    private void hideBanner() {
        setVisible(false);
        refreshParent();
    }

    private void showConfigurationDialog() {
        boolean connected = EmployeeStorageConnectionDialog.showUntilConnected(owner);
        if (connected) {
            EmployeeStorageConnectionMonitor.retrySilently();
        } else {
            EmployeeStorageConnectionMonitor.markNeedsCheck();
            EmployeeStorageConnectionMonitor.retrySilently();
        }
    }

    private void refreshParent() {
        JComponent parent = (JComponent) getParent();
        if (parent != null) {
            parent.setVisible(isVisible());
            parent.revalidate();
            parent.repaint();
        }
    }

    private static JButton textButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(new EmptyBorder(3, 8, 3, 8));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setForeground(ACTION_TEXT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        SwingUtilities.updateComponentTreeUI(button);
        return button;
    }
}
