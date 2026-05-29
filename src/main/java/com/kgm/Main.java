package com.kgm;
import com.kgm.config.DatabaseConfig;
import com.kgm.ui.LoginView;
import com.kgm.util.ApplicationStartup;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Display connection info on startup
        System.out.println("========================================");
        System.out.println("  KGM Ex-Employees Management System");
        System.out.println("========================================");
        System.out.println("  MySQL Server Running On : " + DatabaseConfig.host() + ":" + DatabaseConfig.port());
        System.out.println("  IP Address : " + DatabaseConfig.host());
        System.out.println("  Username   : " + DatabaseConfig.username());
        System.out.println("  Port       : " + DatabaseConfig.port());
        System.out.println("  Database   : " + DatabaseConfig.databaseName());
        System.out.println("========================================");

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
            ApplicationStartup.startSilently();
        });
    }
}
