package com.kgm.ui.panel;

import com.kgm.ui.LoginView;
import com.kgm.ui.navigation.BackNavigationHelper;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Window;
import java.net.URL;

public class HeaderPanel extends JPanel {
    private static final Color TEXT_PRIMARY = new Color(28, 36, 46);
    private static final Color TEXT_SECONDARY = new Color(98, 111, 125);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color LOGOUT = new Color(0, 112, 210);
    private static final Color LOGOUT_HOVER = new Color(0, 88, 168);

    public HeaderPanel(String title) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(10, 22, 14, 22)
        ));

        JPanel center = createBrandBlock(title);
        JButton backButton = BackNavigationHelper.createBackButton(title);
        JButton logoutButton = createLogoutButton();

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(132, 40));
        GridBagConstraints backButtonConstraints = new GridBagConstraints();
        backButtonConstraints.anchor = GridBagConstraints.WEST;
        backButtonConstraints.weightx = 1;
        left.add(backButton, backButtonConstraints);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(132, 40));
        right.add(logoutButton);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                BackNavigationHelper.registerWindow(HeaderPanel.this);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    private JPanel createBrandBlock(String title) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        wrapper.setOpaque(false);

        JLabel logo = new JLabel(loadLogo());
        logo.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel company = new JLabel("Kohinor Textile Mills. Gujar Khan");
        company.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        company.setForeground(TEXT_PRIMARY);
        company.setAlignmentX(CENTER_ALIGNMENT);

        JLabel screen = new JLabel(title == null || title.isBlank() ? "Dashboard" : title);
        screen.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        screen.setForeground(TEXT_SECONDARY);
        screen.setAlignmentX(CENTER_ALIGNMENT);

        textPanel.add(company);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(screen);

        wrapper.add(logo);
        wrapper.add(textPanel);
        return wrapper;
    }

    private ImageIcon loadLogo() {
        ImageIcon icon = loadClasspathIcon("/images/Logo.jpg");
        if (icon.getIconWidth() <= 0) {
            icon = loadClasspathIcon("/images/Header.jpg");
        }
        if (icon.getIconWidth() <= 0) {
            icon = new ImageIcon("images/Logo.jpg");
        }
        if (icon.getIconWidth() <= 0) {
            icon = new ImageIcon("images/Header.jpg");
        }
        if (icon.getIconWidth() <= 0) {
            return new ImageIcon();
        }
        int width = 52;
        int height = Math.max(34, Math.round(width * (icon.getIconHeight() / (float) icon.getIconWidth())));
        Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private ImageIcon loadClasspathIcon(String resourcePath) {
        URL resource = HeaderPanel.class.getResource(resourcePath);
        return resource == null ? new ImageIcon() : new ImageIcon(resource);
    }

    private JButton createLogoutButton() {
        JButton button = new JButton("Logout");
        button.setPreferredSize(new Dimension(92, 34));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setForeground(LOGOUT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                button.setForeground(LOGOUT_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                button.setForeground(LOGOUT);
            }
        });
        button.addActionListener(event -> logout());
        return button;
    }

    private void logout() {
        try {
            SessionManager.clear();
            SessionWatcher.stop();
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
            new LoginView().setVisible(true);
        } catch (Exception exception) {
            DialogHelper.error(
                    SwingUtilities.getWindowAncestor(this),
                    "Error",
                    "Failure - Try Again."
            );
        }
    }
}
