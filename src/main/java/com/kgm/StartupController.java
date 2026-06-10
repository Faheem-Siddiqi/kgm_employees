package com.kgm;

import com.kgm.config.AppConfig;
import com.kgm.config.DatabaseConnection;
import com.kgm.ui.DatabaseSetupView;
import com.kgm.ui.EmployeeStorageConnectionDialog;
import com.kgm.ui.LoginView;
import com.kgm.util.ApplicationStartup;
import com.kgm.util.EmployeeStorageUtil;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import java.util.List;

public final class StartupController {
    private static final Object APPLICATION_LIFECYCLE_LOCK = new Object();

    private StartupController() {
    }

    public static void start() {
        DatabaseConnection.setConnectionFailureListener(null);
        RuntimeException startupFailure = initializeSafely();
        DatabaseConnection.setConnectionFailureListener(DatabaseSetupView::showConnectionFailure);
        showStartupResult(startupFailure);
        waitForApplicationExit();
    }

    public static void showLoginWindow() {
        new LoginView().setVisible(true);
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

    private static void waitForApplicationExit() {
        if (SwingUtilities.isEventDispatchThread()) {
            return;
        }
        synchronized (APPLICATION_LIFECYCLE_LOCK) {
            while (true) {
                try {
                    APPLICATION_LIFECYCLE_LOCK.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static RuntimeException initializeSafely() {
        RuntimeException configurationFailure = configurationFailure();
        if (configurationFailure != null) {
            return configurationFailure;
        }
        while (true) {
            try {
                ApplicationStartup.initializeBlocking();
                return null;
            } catch (RuntimeException exception) {
                if (!EmployeeStorageUtil.isLikelyStorageAccessFailure(exception)) {
                    return exception;
                }
                boolean connected = showEmployeeStorageConnectionDialog();
                if (!connected) {
                    return exception;
                }
            }
        }
    }

    private static boolean showEmployeeStorageConnectionDialog() {
        if (SwingUtilities.isEventDispatchThread()) {
            return EmployeeStorageConnectionDialog.showUntilConnected(null);
        }
        final boolean[] connected = {false};
        try {
            SwingUtilities.invokeAndWait(() -> connected[0] = EmployeeStorageConnectionDialog.showUntilConnected(null));
            return connected[0];
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Employee storage connection dialog could not be opened.", exception);
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
