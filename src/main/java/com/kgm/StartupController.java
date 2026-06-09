package com.kgm;

import com.kgm.config.AppConfig;
import com.kgm.config.DatabaseConnection;
import com.kgm.ui.DatabaseSetupView;
import com.kgm.ui.LoginView;
import com.kgm.util.ApplicationStartup;

import javax.swing.SwingUtilities;
import java.util.List;

public final class StartupController {
    private static final Object WINDOW_LIFECYCLE_LOCK = new Object();
    private static boolean windowLifecycleActive;

    private StartupController() {
    }

    public static void start() {
        DatabaseConnection.setConnectionFailureListener(null);
        RuntimeException startupFailure = initializeSafely();
        DatabaseConnection.setConnectionFailureListener(DatabaseSetupView::showConnectionFailure);
        showStartupResult(startupFailure);
    }

    public static void showLoginWindow() {
        new LoginView().setVisible(true);
        keepJvmAliveForApplication();
        System.out.println("KGM Ex-Employees app started");
    }

    public static void reconnectDatabase() {
        RuntimeException configurationFailure = configurationFailure();
        if (configurationFailure != null) {
            throw configurationFailure;
        }
        ApplicationStartup.initializeBlocking();
    }

    private static void showStartupResult(RuntimeException startupFailure) {
        Runnable openWindow = () -> {
            if (startupFailure == null) {
                showLoginWindow();
            } else {
                DatabaseSetupView.showStartupFailure(startupFailure);
                keepJvmAliveForApplication();
                System.out.println("KGM Ex-Employees app opened database setup guide");
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            openWindow.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(openWindow);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Application startup was interrupted.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Application window could not be opened.", exception);
        }
    }

    private static void keepJvmAliveForApplication() {
        synchronized (WINDOW_LIFECYCLE_LOCK) {
            if (windowLifecycleActive) {
                return;
            }
            windowLifecycleActive = true;
        }

        Thread lifecycleThread = new Thread(() -> {
            try {
                synchronized (WINDOW_LIFECYCLE_LOCK) {
                    while (windowLifecycleActive) {
                        WINDOW_LIFECYCLE_LOCK.wait();
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "KGM-Window-Lifecycle");
        lifecycleThread.setDaemon(false);
        lifecycleThread.start();
    }

    private static RuntimeException initializeSafely() {
        RuntimeException configurationFailure = configurationFailure();
        if (configurationFailure != null) {
            return configurationFailure;
        }
        try {
            ApplicationStartup.initializeBlocking();
            return null;
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private static RuntimeException configurationFailure() {
        List<String> issues = AppConfig.startupConfigurationIssues();
        if (issues.isEmpty()) {
            return null;
        }
        return new IllegalStateException("Startup configuration is incomplete. " + String.join(" ", issues));
    }

}
