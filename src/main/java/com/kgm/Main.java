package com.kgm;
import com.kgm.config.DatabaseConfig;
import javax.swing.UIManager;
import java.awt.Color;

public class Main {
    public static void main(String[] args) {
        applyButtonTextDefaults();

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

        StartupController.start();
    }

    private static void applyButtonTextDefaults() {
        Color grey = new Color(99, 115, 129);
        UIManager.put("Button.foreground", grey);
        UIManager.put("Button.disabledForeground", grey);
        UIManager.put("ToggleButton.foreground", grey);
        UIManager.put("ToggleButton.disabledForeground", grey);
    }
}
