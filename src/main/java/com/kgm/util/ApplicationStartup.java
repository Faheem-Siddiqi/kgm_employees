package com.kgm.util;

import com.kgm.config.DatabaseConnection;
import com.kgm.database.DatabaseInitializer;
import com.kgm.ui.DatabaseConnectionStatusView;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.styling.DialogHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class ApplicationStartup {
    private static final int STARTUP_NOTICE_DELAY_MS = 850;
    private static final int AUTO_RETRY_DELAY_MS = 4_000;
    private static final Object LOCK = new Object();
    private static final List<ReadyWaiter> readyWaiters = new ArrayList<>();
    private static SwingWorker<Void, Void> startupWorker;
    private static DatabaseConnectionStatusView statusView;
    private static Timer delayedNoticeTimer;
    private static Timer autoRetryTimer;
    private static StartupPhase startupPhase = StartupPhase.IDLE;
    private static boolean ready;

    private ApplicationStartup() {
    }

    public static void startSilently() {
        DatabaseConnection.setConnectionFailureListener(ApplicationStartup::showConnectionFailure);
        ensureStarted();
    }

    public static void initializeBlocking() {
        synchronized (LOCK) {
            if (ready) {
                return;
            }
            if (startupWorker != null && !startupWorker.isDone()) {
                throw new IllegalStateException("Application startup is already running.");
            }
        }
        performStartupWork();
        markReady();
    }

    public static void prepareThen(Component parent, Runnable onReady, Runnable onFailure) {
        boolean runNow;
        synchronized (LOCK) {
            runNow = ready;
        }
        if (runNow) {
            runOnEdt(onReady);
            return;
        }

        LoadingOverlay.Handle loader = LoadingOverlay.show(
                parent,
                "Login Successful",
                "Starting application and preparing database and field settings..."
        );
        synchronized (LOCK) {
            runNow = ready;
            if (!runNow) {
                readyWaiters.add(new ReadyWaiter(parent, onReady, onFailure, loader));
            }
        }
        if (runNow) {
            loader.close();
            runOnEdt(onReady);
            return;
        }
        ensureStarted();
    }

    private static SwingWorker<Void, Void> ensureStarted() {
        synchronized (LOCK) {
            if (ready) {
                return startupWorker;
            }
            if (startupWorker == null || startupWorker.isDone()) {
                startupWorker = createStartupWorker();
                startupWorker.execute();
                scheduleDelayedNotice();
            }
            return startupWorker;
        }
    }

    private static SwingWorker<Void, Void> createStartupWorker() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                performStartupWork();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    markReady();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showConnectionFailure(new IllegalStateException("Database startup was interrupted.", exception));
                } catch (CancellationException exception) {
                    showConnectionFailure(new IllegalStateException("Database startup was cancelled.", exception));
                } catch (ExecutionException exception) {
                    Throwable failure = rootCause(exception);
                    if (DatabaseConnection.isLikelyConnectionFailure(failure)) {
                        showConnectionFailure(new IllegalStateException(
                                "Database and field settings could not be prepared.",
                                failure
                        ));
                    } else {
                        showStartupFailure(failure);
                    }
                }
            }
        };
    }

    private static void performStartupWork() {
        setStartupPhase(StartupPhase.PREPARING_STORAGE);
        try {
            EmployeeStorageUtil.ensureStorageRoot();
        } catch (Exception exception) {
            throw new IllegalStateException("Employee storage folder could not be prepared: " + exception.getMessage(), exception);
        }
        setStartupPhase(StartupPhase.CONNECTING_DATABASE);
        DatabaseInitializer.init();
        setStartupPhase(StartupPhase.LOADING_METADATA);
        EmployeeFieldDefinitionCache.refreshFromDatabase();
        EmployeeDocumentUtil.documentTypes();
        EmployeeDocumentUtil.requiredDocumentFlags();
    }

    public static void showConnectionFailure(Throwable failure) {
        RuntimeException runtimeFailure = failure instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Database connection failed.", failure);
        synchronized (LOCK) {
            ready = false;
            startupPhase = StartupPhase.CONNECTING_DATABASE;
        }
        runOnEdt(() -> {
            cancelDelayedNotice();
            statusView().showFailure(runtimeFailure);
            startAutoRetry();
        });
    }

    public static void retryNow() {
        synchronized (LOCK) {
            ready = false;
            if (startupWorker != null && !startupWorker.isDone()) {
                showCheckingStatus(true);
                return;
            }
        }
        stopAutoRetry();
        showCheckingStatus(true);
        ensureStarted();
    }

    private static void markReady() {
        List<ReadyWaiter> waiters;
        synchronized (LOCK) {
            ready = true;
            startupPhase = StartupPhase.READY;
            waiters = new ArrayList<>(readyWaiters);
            readyWaiters.clear();
        }
        runOnEdt(() -> {
            cancelDelayedNotice();
            stopAutoRetry();
            if (statusView != null) {
                statusView.closeView();
                statusView = null;
            }
            for (ReadyWaiter waiter : waiters) {
                waiter.closeLoader();
                runOnEdt(waiter.onReady());
            }
        });
    }

    private static void showStartupFailure(Throwable failure) {
        List<ReadyWaiter> waiters;
        synchronized (LOCK) {
            ready = false;
            waiters = new ArrayList<>(readyWaiters);
            readyWaiters.clear();
            startupPhase = StartupPhase.IDLE;
        }
        runOnEdt(() -> {
            cancelDelayedNotice();
            stopAutoRetry();
            if (statusView != null) {
                statusView.closeView();
                statusView = null;
            }
            if (waiters.isEmpty()) {
                return;
            }
            String message = "Database and field settings could not be prepared.\n\n" + rootMessage(failure);
            for (ReadyWaiter waiter : waiters) {
                waiter.closeLoader();
                DialogHelper.error(waiter.parent(), "Startup Failed", message);
                runOnEdt(waiter.onFailure());
            }
        });
    }

    private static void showCheckingStatus(boolean force) {
        if (!force && currentPhase() != StartupPhase.CONNECTING_DATABASE) {
            return;
        }
        runOnEdt(() -> {
            stopAutoRetry();
            statusView().showChecking();
        });
    }

    private static void scheduleDelayedNotice() {
        runOnEdt(() -> {
            cancelDelayedNotice();
            delayedNoticeTimer = new Timer(STARTUP_NOTICE_DELAY_MS, event -> {
                synchronized (LOCK) {
                    if (ready || startupWorker == null || startupWorker.isDone()) {
                        return;
                    }
                    if (startupPhase != StartupPhase.CONNECTING_DATABASE) {
                        return;
                    }
                }
                showCheckingStatus(false);
            });
            delayedNoticeTimer.setRepeats(false);
            delayedNoticeTimer.start();
        });
    }

    private static void cancelDelayedNotice() {
        if (delayedNoticeTimer != null) {
            delayedNoticeTimer.stop();
            delayedNoticeTimer = null;
        }
    }

    private static void startAutoRetry() {
        stopAutoRetry();
        autoRetryTimer = new Timer(AUTO_RETRY_DELAY_MS, event -> retryNow());
        autoRetryTimer.setRepeats(true);
        autoRetryTimer.start();
    }

    private static void stopAutoRetry() {
        if (autoRetryTimer != null) {
            autoRetryTimer.stop();
            autoRetryTimer = null;
        }
    }

    private static DatabaseConnectionStatusView statusView() {
        if (statusView == null || !statusView.isDisplayable()) {
            statusView = new DatabaseConnectionStatusView(ApplicationStartup::retryNow);
        }
        return statusView;
    }

    private static StartupPhase currentPhase() {
        synchronized (LOCK) {
            return startupPhase;
        }
    }

    private static void setStartupPhase(StartupPhase phase) {
        synchronized (LOCK) {
            startupPhase = phase;
        }
        if (phase == StartupPhase.LOADING_METADATA) {
            closeDatabaseStatusForMetadata();
        }
    }

    private static void closeDatabaseStatusForMetadata() {
        runOnEdt(() -> {
            cancelDelayedNotice();
            stopAutoRetry();
            if (statusView != null) {
                statusView.closeView();
                statusView = null;
            }
        });
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        if (current == null) {
            return "Unknown startup error.";
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

    private record ReadyWaiter(Component parent, Runnable onReady, Runnable onFailure, LoadingOverlay.Handle loader) {
        private void closeLoader() {
            if (loader != null) {
                loader.close();
            }
        }
    }

    private enum StartupPhase {
        IDLE,
        PREPARING_STORAGE,
        CONNECTING_DATABASE,
        LOADING_METADATA,
        READY
    }
}
