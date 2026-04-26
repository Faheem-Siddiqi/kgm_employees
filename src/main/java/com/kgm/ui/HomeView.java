package com.kgm.ui;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;
import com.kgm.ui.panel.HeaderPanel;
import javax.swing.*;
import java.awt.*;
public class HomeView extends JFrame {
    public HomeView() {
        if (!SessionManager.isValid()) {
            JOptionPane.showMessageDialog(this, "Session expired");
            SessionManager.clear();
            SessionWatcher.closeAllWindows();
            new LoginView().setVisible(true);
            return;
        }
        setTitle("Dashboard");
        setSize(800, 600);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        // Add universal header
        add(new HeaderPanel("Dashboard"), BorderLayout.NORTH);
        JLabel label = new JLabel(
                "Welcome " + SessionManager.getUser(),
                SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}