package com.kgm.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class WindowsNetworkShareConnector {
    private static final Duration MANUAL_CONNECT_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration QUICK_RECONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CREDENTIAL_STORE_TIMEOUT = Duration.ofSeconds(3);

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

        ConnectionResult stored = storeCredential(uncPath, username, password);
        if (!stored.connected()) {
            return stored;
        }

        return runNetUse(MANUAL_CONNECT_TIMEOUT, uncPath, "/user:" + username.trim(), new String(password), "/persistent:yes");
    }

    public static ConnectionResult reconnectStored(String uncPath) {
        if (uncPath == null || uncPath.isBlank()) {
            return ConnectionResult.failed("Expected folder is required.");
        }
        if (!isWindows()) {
            return ConnectionResult.failed("Network share reconnect is only available on Windows.");
        }
        return runNetUse(QUICK_RECONNECT_TIMEOUT, uncPath, "/persistent:yes");
    }

    private static ConnectionResult storeCredential(String uncPath, String username, char[] password) {
        String target = credentialTarget(uncPath);
        if (target.isBlank()) {
            return ConnectionResult.failed("Shared folder server is required.");
        }
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "cmdkey",
                "/add:" + target,
                "/user:" + username.trim(),
                "/pass:" + new String(password)
        );
        builder.redirectErrorStream(true);
        return runCommand(builder, CREDENTIAL_STORE_TIMEOUT, "Windows credential could not be saved.");
    }

    private static ConnectionResult runNetUse(Duration timeout, String uncPath, String... arguments) {
        String[] command = new String[5 + arguments.length];
        command[0] = "cmd.exe";
        command[1] = "/c";
        command[2] = "net";
        command[3] = "use";
        command[4] = uncPath;
        System.arraycopy(arguments, 0, command, 5, arguments.length);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        return runCommand(builder, timeout, "Network share connection failed.");
    }

    private static ConnectionResult runCommand(ProcessBuilder builder, Duration timeout, String fallbackMessage) {
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ConnectionResult.failed("Connection timed out after " + timeout.toSeconds() + " seconds. Check the server address, LAN/VPN connection, shared folder name, username, and password.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() == 0 || isAlreadyConnected(output)) {
                return ConnectionResult.success();
            }
            return ConnectionResult.failed(cleanCommandOutput(output, fallbackMessage));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ConnectionResult.failed("Connection was interrupted.");
        } catch (IOException exception) {
            return ConnectionResult.failed(exception.getMessage());
        }
    }

    private static String credentialTarget(String uncPath) {
        String clean = uncPath == null ? "" : uncPath.trim();
        while (clean.startsWith("\\") || clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        int slash = clean.indexOf('\\');
        if (slash < 0) {
            slash = clean.indexOf('/');
        }
        return slash >= 0 ? clean.substring(0, slash).trim() : clean;
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

    private static String cleanCommandOutput(String output, String fallbackMessage) {
        if (output == null || output.isBlank()) {
            return fallbackMessage;
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
