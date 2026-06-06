package com.kgm.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppConfig {
    private static final long DEFAULT_DOCUMENT_UPLOAD_MAX_BYTES = 400L * 1024L;
    private static final int DEFAULT_LONG_SERVICE_TIMEOUT_MINUTES = 15;
    private static final Pattern WINDOWS_ENV_TOKEN = Pattern.compile("%([A-Za-z0-9_]+)%");
    private static final Pattern UNIX_ENV_TOKEN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    private AppConfig() {
    }

    public static String adminUsername() {
        return setting("kgm.admin.user", "KGM_ADMIN_USER", "");
    }

    public static String adminPassword() {
        return setting("kgm.admin.password", "KGM_ADMIN_PASSWORD", "");
    }

    public static String fieldSettingsPassword() {
        String configured = setting("kgm.field.settings.password", "FIELD_SETTINGS", "");
        if (configured == null || configured.isBlank()) {
            configured = setting("kgm.field.settings.password", "KGM_FIELD_SETTINGS_PASSWORD", "");
        }
        return configured == null || configured.isBlank() ? adminPassword() : configured;
    }

    public static Path employeeStorageDirectory() {
        String configured = setting(
                "kgm.employee.storage.dir",
                "KGM_EMPLOYEE_STORAGE_DIR",
                "resources/employees"
        );
        Path path = Path.of(expandPath(configured));
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize();
    }

    public static long documentUploadMaxBytes() {
        return positiveLongSetting(
                "kgm.document.upload.max.bytes",
                "KGM_DOCUMENT_UPLOAD_MAX_BYTES",
                DEFAULT_DOCUMENT_UPLOAD_MAX_BYTES
        );
    }

    public static int longServiceTimeoutMinutes() {
        long minutes = positiveLongSetting(
                "kgm.long.service.timeout.minutes",
                "KGM_LONG_SERVICE_TIMEOUT_MINUTES",
                DEFAULT_LONG_SERVICE_TIMEOUT_MINUTES
        );
        return minutes > Integer.MAX_VALUE ? DEFAULT_LONG_SERVICE_TIMEOUT_MINUTES : (int) minutes;
    }

    public static String setting(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String dotEnvValue = loadDotEnv().get(envName);
        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue.trim();
        }

        return defaultValue;
    }

    private static long positiveLongSetting(String propertyName, String envName, long defaultValue) {
        String configured = setting(propertyName, envName, Long.toString(defaultValue));
        try {
            long parsed = Long.parseLong(normalizeNumber(configured));
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String normalizeNumber(String value) {
        return value == null ? "" : value.trim().replace(",", "").replace("_", "");
    }

    private static Map<String, String> loadDotEnv() {
        Path path = Path.of(System.getProperty("user.dir"), ".env").toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String rawLine : lines) {
                parseLine(rawLine, values);
            }
        } catch (IOException exception) {
            System.err.println("Could not read .env file: " + exception.getMessage());
        }
        return Collections.unmodifiableMap(values);
    }

    private static String expandPath(String value) {
        String expanded = value == null ? "" : value.trim();
        if (expanded.equals("~")) {
            expanded = System.getProperty("user.home");
        } else if (expanded.startsWith("~/") || expanded.startsWith("~\\")) {
            expanded = System.getProperty("user.home") + expanded.substring(1);
        }
        expanded = expandEnvTokens(expanded, WINDOWS_ENV_TOKEN);
        expanded = expandEnvTokens(expanded, UNIX_ENV_TOKEN);
        return expanded;
    }

    private static String expandEnvTokens(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = System.getenv(matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement == null ? matcher.group(0) : replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static void parseLine(String rawLine, Map<String, String> values) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).strip();
        }

        int separator = line.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        if (key.isEmpty()) {
            return;
        }

        values.put(key, unquote(value));
    }

    private static String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            String unquoted = value.substring(1, value.length() - 1);
            return first == '"' ? unescapeDoubleQuoted(unquoted) : unquoted;
        }
        return value;
    }

    private static String unescapeDoubleQuoted(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!escaping) {
                if (current == '\\') {
                    escaping = true;
                } else {
                    builder.append(current);
                }
                continue;
            }

            switch (current) {
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case '\\' -> builder.append('\\');
                case '"' -> builder.append('"');
                default -> {
                    builder.append('\\');
                    builder.append(current);
                }
            }
            escaping = false;
        }
        if (escaping) {
            builder.append('\\');
        }
        return builder.toString();
    }
}
