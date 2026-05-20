package com.kgm;
import com.kgm.config.DatabaseConfig;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.LoginView;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Display connection info on startup
        System.out.println("========================================");
        System.out.println("  KGM Ex-Employees Management System");
        System.out.println("========================================");
        System.out.println("  IP Address : " + DatabaseConfig.host());
        System.out.println("  Username   : " + DatabaseConfig.username());
        System.out.println("  Port       : " + DatabaseConfig.port());
        System.out.println("========================================");

        SwingUtilities.invokeLater(() -> {
            DatabaseInitializer.init();
            new LoginView().setVisible(true);
            System.out.println("App started");
        });
    }
}


