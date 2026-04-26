package com.kgm.ui.panel;
import com.kgm.ui.LoginView;
import com.kgm.util.SessionManager;
import com.kgm.util.SessionWatcher;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
public class HeaderPanel extends JPanel {
    public HeaderPanel(String title) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(10, 20, 10, 20)));
        // ================= LEFT (LOGO + TEXT) =================
        JLabel logo = new JLabel();
        ImageIcon logoIcon = new ImageIcon("images/Header.jpg");
        Image img = logoIcon.getImage().getScaledInstance(75, 60, Image.SCALE_SMOOTH);
        logo.setIcon(new ImageIcon(img));
        JLabel company = new JLabel("Koohinoor Textile Mills");
        company.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel screen = new JLabel(title);
        screen.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        screen.setForeground(new Color(90, 90, 90));
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(company);
        textPanel.add(screen);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(logo);
        left.add(textPanel);
        // force vertical center alignment
        left.setAlignmentY(Component.CENTER_ALIGNMENT);
        // ================= RIGHT (INFO + LOGOUT) =================
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(infoRow("Phone:", "0092-051-54955328"));
        right.add(infoRow("Export:", "0092-051-5473085"));
        right.add(Box.createVerticalStrut(4));
        right.add(logoutRow());
        JPanel rightAlign = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightAlign.setOpaque(false);
        rightAlign.add(right);
        rightAlign.setAlignmentY(Component.CENTER_ALIGNMENT);
        // ================= MAIN LAYOUT =================
        add(left, BorderLayout.WEST);
        add(rightAlign, BorderLayout.EAST);
    }
    // ================= INFO ROW =================
    private JPanel infoRow(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        p.setOpaque(false);
        JLabel l1 = new JLabel(label);
        l1.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l1.setForeground(new Color(80, 80, 80));
        JLabel l2 = new JLabel(value);
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l2.setForeground(new Color(50, 50, 50));
        p.add(l1);
        p.add(l2);
        return p;
    }
    // ================= LOGOUT =================
    private JPanel logoutRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        p.setOpaque(false);
        JLabel logout = new JLabel("Logout");
        logout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logout.setForeground(new Color(0, 102, 204));
        logout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                logout();
            }
        });
        p.add(logout);
        return p;
    }
    // ================= LOGOUT LOGIC =================
    private void logout() {
        try {
            SessionManager.clear();
            SessionWatcher.stop();
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null)
                w.dispose();
            new LoginView().setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Failure - Try Again",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}