package com.kgm.util;

import com.kgm.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EmployeeStorageUtil {
    private static final String LOGICAL_ROOT = "employees";
    private static final String DOCUMENTS_DIR = "documents";
    private static final String PROFILE_IMAGE = "EMP_IMG.jpg";

    private EmployeeStorageUtil() {
    }

    public static Path storageRoot() {
        return AppConfig.employeeStorageDirectory();
    }

    public static Path ensureEmployeeDirectory(String employeeCode) throws IOException {
        Path directory = employeeDirectory(employeeCode);
        Files.createDirectories(directory);
        return directory;
    }

    public static Path ensureDocumentDirectory(String employeeCode) throws IOException {
        Path directory = documentDirectory(employeeCode);
        Files.createDirectories(directory);
        return directory;
    }

    public static Path employeeDirectory(String employeeCode) {
        return storageRoot().resolve(pathSegment(employeeCode)).normalize();
    }

    public static Path documentDirectory(String employeeCode) {
        return employeeDirectory(employeeCode).resolve(DOCUMENTS_DIR).normalize();
    }

    public static String profileImagePath(String employeeCode) {
        return logicalEmployeePath(employeeCode, PROFILE_IMAGE);
    }

    public static String documentPath(String employeeCode, String storageName) {
        return logicalEmployeePath(employeeCode, DOCUMENTS_DIR + "/" + fileNameSegment(storageName));
    }

    public static File resolveStoredFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return new File("");
        }

        String trimmed = storedPath.trim();
        Path raw = Path.of(trimmed);
        if (raw.isAbsolute()) {
            return raw.normalize().toFile();
        }

        Path configured = resolveConfiguredPath(trimmed);
        if (Files.exists(configured)) {
            return configured.toFile();
        }

        Path legacy = Path.of(System.getProperty("user.dir")).resolve(trimmed).normalize();
        if (Files.exists(legacy)) {
            return legacy.toFile();
        }

        return configured.toFile();
    }

    private static String logicalEmployeePath(String employeeCode, String relativePath) {
        return LOGICAL_ROOT + "/" + pathSegment(employeeCode) + "/" + relativePath.replace('\\', '/');
    }

    private static Path resolveConfiguredPath(String storedPath) {
        String normalized = storedPath.replace('\\', '/');
        String prefix = LOGICAL_ROOT + "/";
        if (normalized.equals(LOGICAL_ROOT)) {
            return storageRoot();
        }
        if (normalized.startsWith(prefix)) {
            return resolveUnderStorage(normalized.substring(prefix.length()));
        }
        return Path.of(System.getProperty("user.dir")).resolve(storedPath).normalize();
    }

    private static Path resolveUnderStorage(String relativePath) {
        Path root = storageRoot();
        Path resolved = root.resolve(relativePath).normalize();
        return resolved.startsWith(root) ? resolved : root;
    }

    private static String pathSegment(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            clean = "unknown";
        }
        return clean
                .replace('\\', '_')
                .replace('/', '_')
                .replace(':', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace('"', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('|', '_');
    }

    private static String fileNameSegment(String value) {
        String clean = pathSegment(value);
        return clean.equals("unknown") ? "document.jpg" : clean;
    }
}
