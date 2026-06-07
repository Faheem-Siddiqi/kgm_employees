package com.kgm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Consumer;

public final class DatabaseConnection {
    private static volatile Consumer<RuntimeException> connectionFailureListener;

    private DatabaseConnection() {
    }

    public static void setConnectionFailureListener(Consumer<RuntimeException> listener) {
        connectionFailureListener = listener;
    }

    public static void reportConnectionFailure(Throwable failure) {
        Consumer<RuntimeException> listener = connectionFailureListener;
        if (listener == null || failure == null) {
            return;
        }
        RuntimeException runtimeFailure = failure instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException("Database connection failed.", failure);
        listener.accept(runtimeFailure);
    }

    public static boolean reportIfConnectionFailure(Throwable failure) {
        if (!isLikelyConnectionFailure(failure)) {
            return false;
        }
        reportConnectionFailure(failure);
        return true;
    }

    public static Connection getServerConnection() throws SQLException {
        loadDriver();
        return getConnection(DatabaseConfig.serverUrl());
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        return getConnection(DatabaseConfig.databaseUrl());
    }

    private static Connection getConnection(String url) throws SQLException {
        SQLException firstException = null;
        for (String password : candidatePasswords()) {
            try {
                return DriverManager.getConnection(url, DatabaseConfig.username(), password);
            } catch (SQLException exception) {
                if (!isAccessDenied(exception)) {
                    notifyConnectionFailure(url, exception);
                    throw exception;
                }
                if (firstException == null) {
                    firstException = exception;
                }
            }
        }

        SQLException failure = firstException == null
                ? new SQLException("Unable to connect to MySQL.")
                : firstException;
        notifyConnectionFailure(url, failure);
        throw failure;
    }

    private static void notifyConnectionFailure(String url, SQLException exception) {
        reportConnectionFailure(new IllegalStateException(
                "Unable to connect to " + readableConnection(url) + ".",
                exception
        ));
    }

    private static String readableConnection(String url) {
        return DatabaseConfig.username()
                + "@"
                + DatabaseConfig.host()
                + ":"
                + DatabaseConfig.port()
                + "/"
                + (url != null && url.contains("/" + DatabaseConfig.databaseName())
                ? DatabaseConfig.databaseName()
                : "server");
    }

    public static boolean isLikelyConnectionFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("mysql")
                        || lower.contains("database")
                        || lower.contains("jdbc")
                        || lower.contains("connection")
                        || lower.contains("communications link")
                        || lower.contains("socket")
                        || lower.contains("access denied")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String[] candidatePasswords() {
        String password = DatabaseConfig.password();
        if (password.endsWith("`")) {
            return new String[]{password, password.substring(0, password.length() - 1)};
        }
        return new String[]{password, password + "`"};
    }

    private static boolean isAccessDenied(SQLException exception) {
        return exception.getErrorCode() == 1045;
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL JDBC driver not found.", exception);
        }
    }
}
