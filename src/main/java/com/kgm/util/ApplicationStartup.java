package com.kgm.util;

import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class ApplicationStartup {
    private static final Object LOCK = new Object();
    private static SwingWorker<Void, Void> startupWorker;
    private static boolean ready;

    private ApplicationStartup() {
    }

    public static void startSilently() {
        ensureStarted();
    }

    public static void prepareThen(Component parent, Runnable onReady, Runnable onFailure) {
        SwingWorker<Void, Void> worker = ensureStarted();
        if (isReady()) {
            runOnEdt(onReady);
            return;
        }

        LoadingOverlay.Handle loader = LoadingOverlay.show(
                parent,
                "Login Successful",
                "Starting application and preparing database and field settings..."
        );

        SwingWorker<Void, Void> waiter = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                worker.get();
                return null;
            }

            @Override
            protected void done() {
                loader.close();
                try {
                    get();
                    runOnEdt(onReady);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    handleFailure(parent, "Application startup was interrupted.", onFailure);
                } catch (CancellationException exception) {
                    handleFailure(parent, "Application startup was cancelled.", onFailure);
                } catch (ExecutionException exception) {
                    handleFailure(
                            parent,
                            "Database and field settings could not be prepared.\n\n" + rootMessage(exception),
                            onFailure
                    );
                }
            }
        };
        waiter.execute();
    }

    private static SwingWorker<Void, Void> ensureStarted() {
        synchronized (LOCK) {
            if (ready) {
                return startupWorker;
            }
            if (startupWorker == null || startupWorker.isDone()) {
                startupWorker = createStartupWorker();
                startupWorker.execute();
            }
            return startupWorker;
        }
    }

    private static SwingWorker<Void, Void> createStartupWorker() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                DatabaseInitializer.init();
                EmployeeFieldDefinitionCache.refreshFromDatabase();
                EmployeeDocumentUtil.documentTypes();
                EmployeeDocumentUtil.requiredDocumentFlags();
                synchronized (LOCK) {
                    ready = true;
                }
                return null;
            }
        };
    }

    private static boolean isReady() {
        synchronized (LOCK) {
            return ready;
        }
    }

    private static void handleFailure(Component parent, String message, Runnable onFailure) {
        synchronized (LOCK) {
            ready = false;
        }
        DialogHelper.error(parent, "Startup Failed", message);
        runOnEdt(onFailure);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private static void runOnEdt(Runnable action) {
        if (action == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
