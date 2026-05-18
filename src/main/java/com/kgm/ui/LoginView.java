package com.kgm.ui;

import com.kgm.service.AuthService;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.LoginViewStyle;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;

import javax.swing.*;
import java.awt.*;

public class LoginView extends JFrame {

    public LoginView() {
        LoginViewStyle.applyFrame(this);

        JPanel root = LoginViewStyle.createRootPanel();
        add(root, BorderLayout.CENTER);

        root.add(LoginViewStyle.createImagePanel());
        root.add(createLoginPanel());

        setLocationRelativeTo(null);
    }

    private JPanel createLoginPanel() {
        JPanel outer = LoginViewStyle.createOuterPanel();
        JPanel form = LoginViewStyle.createFormPanel();

        JLabel eyebrow = LoginViewStyle.createEyebrowLabel("KGM EX-EMPLOYEES PORTAL");
        JLabel welcome = LoginViewStyle.createWelcomeLabel("Welcome Back");
        JLabel subtitle = LoginViewStyle.createSubtitleLabel("Sign in to continue to guest management.");

        JTextField userField = LoginViewStyle.createTextField("Enter username");
        JPasswordField passField = LoginViewStyle.createPasswordField("Enter password");
        JButton loginBtn = LoginViewStyle.createPrimaryButton("Sign In");

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(14));
        form.add(welcome);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(30));
        form.add(LoginViewStyle.createFieldBlock("Username", userField));
        form.add(Box.createVerticalStrut(14));
        form.add(LoginViewStyle.createFieldBlock("Password", passField));
        form.add(Box.createVerticalStrut(20));
        form.add(loginBtn);

        outer.add(form, new GridBagConstraints());
        getRootPane().setDefaultButton(loginBtn);

        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if (AuthService.login(user, pass)) {
                SessionManager.startSession(user);
                SessionWatcher.start();
                SessionWatcher.closeAllWindows();
                new HomeView();
            } else {
                DialogHelper.error(this, "Login Failed", "Invalid username or password.");
            }
        });

        return outer;
    }
}
