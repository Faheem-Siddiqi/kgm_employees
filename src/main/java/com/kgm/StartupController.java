package com.kgm;

import com.kgm.config.AppConfig;
import com.kgm.config.DatabaseConnection;
import com.kgm.ui.DatabaseSetupView;
import com.kgm.ui.LoginView;
import com.kgm.util.ApplicationStartup;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class StartupController {
    private StartupController() {
    }

    public static void start() {
        DatabaseConnection.setConnectionFailureListener(DatabaseSetupView::showConnectionFailure);
        SwingUtilities.invokeLater(StartupController::startApplicationCheck);
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

    private static void startApplicationCheck() {
        new SwingWorker<RuntimeException, Void>() {
            @Override
            protected RuntimeException doInBackground() {
                return initializeSafely();
            }

            @Override
            protected void done() {
                RuntimeException startupFailure = startupResult();
                if (startupFailure == null) {
                    showLoginWindow();
                } else {
                    DatabaseSetupView.showStartupFailure(startupFailure);
                    System.out.println("KGM Ex-Employees app opened database setup guide");
                }
            }

            private RuntimeException startupResult() {
                try {
                    return get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return new IllegalStateException("Application startup was interrupted.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof RuntimeException runtimeException) {
                        return runtimeException;
                    }
                    return new IllegalStateException("Application startup failed.", cause);
                }
            }
        }.execute();
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
