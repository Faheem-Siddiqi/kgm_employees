package com.kgm.util;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class EmployeeStorageConnectionMonitor {
    private static final int CHECK_TIMEOUT_SECONDS = 4;
    private static final Object LOCK = new Object();
    private static final List<StorageStatusListener> listeners = new ArrayList<>();
    private static SwingWorker<Status, Void> worker;
    private static Status status = Status.UNKNOWN;

    private EmployeeStorageConnectionMonitor() {
    }

    public static void startSilently() {
        check(false);
    }

    public static void retrySilently() {
        check(true);
    }

    public static Status status() {
        synchronized (LOCK) {
            return status;
        }
    }

    public static void addListener(StorageStatusListener listener) {
        if (listener == null) {
            return;
        }
        Status current;
        synchronized (LOCK) {
            listeners.add(listener);
            current = status;
        }
        notifyListener(listener, current);
    }

    public static void removeListener(StorageStatusListener listener) {
        synchronized (LOCK) {
            listeners.remove(listener);
        }
    }

    public static void markNeedsCheck() {
        updateStatus(Status.UNKNOWN);
    }

    private static void check(boolean userRetry) {
        synchronized (LOCK) {
            if (!userRetry && status != Status.UNKNOWN) {
                return;
            }
            if (worker != null && !worker.isDone()) {
                updateStatusLocked(Status.CHECKING);
                return;
            }
            updateStatusLocked(Status.CHECKING);
            worker = createWorker();
            worker.execute();
        }
    }

    private static SwingWorker<Status, Void> createWorker() {
        return new SwingWorker<>() {
            @Override
            protected Status doInBackground() {
                try {
                    verifyStorageWithTimeout();
                    return Status.CONNECTED;
                } catch (Exception exception) {
                    return Status.DISCONNECTED;
                }
            }

            @Override
            protected void done() {
                try {
                    updateStatus(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    updateStatus(Status.DISCONNECTED);
                } catch (ExecutionException exception) {
                    updateStatus(Status.DISCONNECTED);
                }
            }
        };
    }

    private static void verifyStorageWithTimeout() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            verifyStorage();
            return null;
        });
        Thread thread = new Thread(task, "KGM employee storage background check");
        thread.setDaemon(true);
        thread.start();
        try {
            task.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            task.cancel(true);
            throw new IOException("Employee storage folder check timed out.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = rootCause(exception);
            if (cause instanceof Exception checked) {
                throw checked;
            }
            throw new IllegalStateException(cause);
        }
    }

    private static void verifyStorage() throws IOException {
        Path root = EmployeeStorageUtil.storageRoot();
        if (isUncPath(root) && !WindowsNetworkShareConnector.exists(root)) {
            WindowsNetworkShareConnector.ConnectionResult reconnect =
                    WindowsNetworkShareConnector.reconnectStored(root.toString());
            if (!reconnect.connected()) {
                throw new IOException(reconnect.message().isBlank()
                        ? "Windows could not reconnect the employee storage folder."
                        : reconnect.message());
            }
        }
        EmployeeStorageUtil.ensureStorageRoot();
    }

    private static boolean isUncPath(Path path) {
        String value = path == null ? "" : path.toString();
        return value.startsWith("\\\\") || value.startsWith("//");
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static void updateStatus(Status nextStatus) {
        synchronized (LOCK) {
            updateStatusLocked(nextStatus);
        }
    }

    private static void updateStatusLocked(Status nextStatus) {
        status = nextStatus;
        List<StorageStatusListener> snapshot = new ArrayList<>(listeners);
        for (StorageStatusListener listener : snapshot) {
            notifyListener(listener, nextStatus);
        }
    }

    private static void notifyListener(StorageStatusListener listener, Status nextStatus) {
        SwingUtilities.invokeLater(() -> listener.statusChanged(nextStatus));
    }

    public enum Status {
        UNKNOWN,
        CHECKING,
        CONNECTED,
        DISCONNECTED
    }

    public interface StorageStatusListener {
        void statusChanged(Status status);
    }
}
