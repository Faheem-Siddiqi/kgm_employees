package com.kgm.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class WindowsNetworkShareConnector {
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(6);

    private WindowsNetworkShareConnector() {
    }

    public static ConnectionResult connect(String uncPath, String username, char[] password) {
        if (uncPath == null || uncPath.isBlank()) {
            return ConnectionResult.failed("Expected folder is required.");
        }
        if (username == null || username.isBlank()) {
            return ConnectionResult.failed("Username is required.");
        }
        if (password == null || password.length == 0) {
            return ConnectionResult.failed("Password is required.");
        }
        if (!isWindows()) {
            return ConnectionResult.failed("Network share connection is only available on Windows.");
        }

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "net",
                "use",
                uncPath,
                "/user:" + username.trim(),
                new String(password),
                "/persistent:yes"
        );
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ConnectionResult.failed("Connection timed out after 6 seconds. Check the server address, LAN/VPN connection, shared folder name, username, and password.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() == 0 || isAlreadyConnected(output)) {
                return ConnectionResult.success();
            }
            return ConnectionResult.failed(cleanCommandOutput(output));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ConnectionResult.failed("Connection was interrupted.");
        } catch (IOException exception) {
            return ConnectionResult.failed(exception.getMessage());
        }
    }

    public static boolean canReadAndWrite(Path folder) {
        try {
            EmployeeStorageUtil.verifyReadWriteAccess(folder);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public static boolean exists(Path folder) {
        try {
            return folder != null && Files.isDirectory(folder) && Files.isReadable(folder);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isAlreadyConnected(String output) {
        String normalized = output == null ? "" : output.toLowerCase(Locale.ROOT);
        return normalized.contains("command completed successfully")
                || normalized.contains("multiple connections")
                || normalized.contains("already");
    }

    private static String cleanCommandOutput(String output) {
        if (output == null || output.isBlank()) {
            return "Network share connection failed.";
        }
        return output.trim();
    }

    public record ConnectionResult(boolean connected, String message) {
        public static ConnectionResult success() {
            return new ConnectionResult(true, "");
        }

        public static ConnectionResult failed(String message) {
            return new ConnectionResult(false, message == null ? "" : message);
        }
    }
}
