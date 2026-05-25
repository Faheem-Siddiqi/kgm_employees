package com.kgm.util;

import com.kgm.model.EmployeeFieldDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EmployeeFieldMetadataStore {
    public static final int SCHEMA_VERSION = 1;
    private static final String KIND = "kgm.employeeFieldMetadata";
    private static final String DEFAULT_RESOURCE = "/employee_field_metadata_default.json";
    private static final String APP_DIR_NAME = "KGM Ex-Employee Management";
    private static final String BACKUP_FILE = "employee_field_metadata.json";
    private static final String BACKUP_COPY_FILE = "employee_field_metadata.json.bak";
    private static final String CACHE_FILE = "employee_field_metadata.cache.json";
    private static final Object LOCK = new Object();

    private EmployeeFieldMetadataStore() {
    }

    public static Optional<Snapshot> loadFreshCache(String dbChecksum) {
        synchronized (LOCK) {
            try {
                Snapshot snapshot = readFile(cachePath(), false);
                if (!SCHEMA_VERSION_EQUALS(snapshot.schemaVersion())
                        || !safeEquals(snapshot.dbChecksum(), dbChecksum)) {
                    return Optional.empty();
                }
                return Optional.of(snapshot);
            } catch (IOException | RuntimeException exception) {
                return Optional.empty();
            }
        }
    }

    public static Snapshot loadRestoreSnapshot() throws IOException {
        synchronized (LOCK) {
            Optional<Snapshot> external = loadBestExternalSnapshot();
            if (external.isPresent()) {
                return external.get();
            }
            return loadBundledDefault();
        }
    }

    public static Optional<Snapshot> loadBestExternalSnapshot() {
        synchronized (LOCK) {
            Snapshot primary = null;
            Snapshot backup = null;
            try {
                primary = readFile(externalPath(), false);
            } catch (IOException | RuntimeException ignored) {
            }
            try {
                backup = readFile(externalBackupPath(), false);
            } catch (IOException | RuntimeException ignored) {
            }

            if (primary == null && backup == null) {
                return Optional.empty();
            }
            if (primary == null) {
                recoverExternalFromBackup(backup);
                return Optional.of(backup);
            }
            if (backup == null || !backup.updatedAt().isAfter(primary.updatedAt())) {
                return Optional.of(primary);
            }
            recoverExternalFromBackup(backup);
            return Optional.of(backup);
        }
    }

    public static Snapshot loadBundledDefault() throws IOException {
        synchronized (LOCK) {
            try (InputStream input = EmployeeFieldMetadataStore.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                if (input == null) {
                    throw new IOException("Bundled metadata default resource was not found: " + DEFAULT_RESOURCE);
                }
                String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                return readJson(json, "bundled-resource", null);
            }
        }
    }

    public static void saveExternalAndCache(
            List<EmployeeFieldDefinition> definitions,
            String dbChecksum
    ) throws IOException {
        synchronized (LOCK) {
            SnapshotPayload payload = payload(definitions, dbChecksum, Instant.now(), "external-backup");
            String json = toJson(payload);
            atomicWrite(externalPath(), externalBackupPath(), json, true);
            atomicWrite(cachePath(), null, toJson(payload.withSource("cache")), false);
        }
    }

    public static void saveCache(
            List<EmployeeFieldDefinition> definitions,
            String dbChecksum
    ) throws IOException {
        synchronized (LOCK) {
            SnapshotPayload payload = payload(definitions, dbChecksum, Instant.now(), "cache");
            atomicWrite(cachePath(), null, toJson(payload), false);
        }
    }

    public static void repairExternalAndCacheIfNeeded(
            List<EmployeeFieldDefinition> definitions,
            String dbChecksum
    ) throws IOException {
        synchronized (LOCK) {
            boolean cacheOk = loadFreshCache(dbChecksum).isPresent();
            boolean externalOk = loadBestExternalSnapshot()
                    .map(snapshot -> safeEquals(snapshot.checksum(), metadataChecksum(definitions)))
                    .orElse(false);
            if (!cacheOk || !externalOk) {
                saveExternalAndCache(definitions, dbChecksum);
            }
        }
    }

    public static void writeBundledDefault(Path target, List<EmployeeFieldDefinition> definitions) throws IOException {
        synchronized (LOCK) {
            SnapshotPayload payload = payload(definitions, "", Instant.now(), "bundled-default");
            Path parent = target.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, toJson(payload), StandardCharsets.UTF_8);
        }
    }

    public static String metadataChecksum(List<EmployeeFieldDefinition> definitions) {
        List<EmployeeFieldDefinition> normalized = normalizeDefinitions(definitions, true);
        StringBuilder canonical = new StringBuilder();
        canonical.append("schema=").append(SCHEMA_VERSION).append('\n');
        for (EmployeeFieldDefinition definition : normalized) {
            appendCanonical(canonical, definition.columnName());
            appendCanonical(canonical, definition.label());
            appendCanonical(canonical, definition.heading());
            canonical.append(definition.documentField()).append('|')
                    .append(definition.customField()).append('|')
                    .append(definition.protectedField()).append('|')
                    .append(definition.detailField()).append('|')
                    .append(definition.dateField()).append('|')
                    .append(definition.sortOrder()).append('|')
                    .append(definition.coreField()).append('|')
                    .append(definition.dropdownField()).append('|')
                    .append(definition.variableOptionField()).append('|')
                    .append(definition.textAreaField()).append('|')
                    .append(definition.requiredField()).append('|');
            appendCanonical(canonical, definition.dropdownOptions());
            canonical.append('\n');
        }
        return sha256(canonical.toString());
    }

    public static Path externalPath() {
        return appDataDir().resolve(BACKUP_FILE);
    }

    public static Path externalBackupPath() {
        return appDataDir().resolve(BACKUP_COPY_FILE);
    }

    public static Path cachePath() {
        return cacheDir().resolve(CACHE_FILE);
    }

    public record Snapshot(
            int schemaVersion,
            Instant updatedAt,
            String checksum,
            String dbChecksum,
            String source,
            List<EmployeeFieldDefinition> definitions,
            String location
    ) {
    }

    private record SnapshotPayload(
            int schemaVersion,
            Instant updatedAt,
            String checksum,
            String dbChecksum,
            String source,
            List<EmployeeFieldDefinition> definitions
    ) {
        private SnapshotPayload withSource(String newSource) {
            return new SnapshotPayload(schemaVersion, updatedAt, checksum, dbChecksum, newSource, definitions);
        }
    }

    private static SnapshotPayload payload(
            List<EmployeeFieldDefinition> definitions,
            String dbChecksum,
            Instant updatedAt,
            String source
    ) {
        List<EmployeeFieldDefinition> normalized = normalizeDefinitions(definitions, true);
        return new SnapshotPayload(
                SCHEMA_VERSION,
                updatedAt,
                metadataChecksum(normalized),
                dbChecksum == null ? "" : dbChecksum,
                source,
                normalized
        );
    }

    private static Snapshot readFile(Path path, boolean allowMissing) throws IOException {
        if (!Files.isRegularFile(path)) {
            if (allowMissing) {
                return null;
            }
            throw new IOException("Metadata JSON file was not found: " + path);
        }
        return readJson(Files.readString(path, StandardCharsets.UTF_8), path.toString(), path);
    }

    private static Snapshot readJson(String json, String location, Path path) throws IOException {
        Object rootValue = new JsonParser(json).parse();
        if (!(rootValue instanceof Map<?, ?> rawRoot)) {
            throw new IOException("Metadata JSON root must be an object: " + location);
        }
        Map<String, Object> root = stringMap(rawRoot);
        int schemaVersion = intValue(root.get("schemaVersion"), -1);
        if (!SCHEMA_VERSION_EQUALS(schemaVersion)) {
            throw new IOException("Metadata JSON schema version is not supported: " + schemaVersion);
        }
        String kind = stringValue(root.get("kind"));
        if (!KIND.equals(kind)) {
            throw new IOException("Metadata JSON kind is not supported: " + kind);
        }
        Instant updatedAt = instantValue(root.get("updatedAt"));
        String checksum = stringValue(root.get("checksum"));
        String dbChecksum = stringValue(root.get("dbChecksum"));
        String source = stringValue(root.get("source"));
        List<EmployeeFieldDefinition> definitions = definitionsValue(root.get("fields"));
        String computed = metadataChecksum(definitions);
        if (!safeEquals(checksum, computed)) {
            throw new IOException("Metadata JSON checksum failed: " + location);
        }
        return new Snapshot(
                schemaVersion,
                updatedAt,
                checksum,
                dbChecksum,
                source,
                definitions,
                path == null ? location : path.toString()
        );
    }

    private static List<EmployeeFieldDefinition> definitionsValue(Object value) throws IOException {
        if (!(value instanceof List<?> rawList)) {
            throw new IOException("Metadata JSON fields must be an array.");
        }
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw new IOException("Metadata JSON field item must be an object.");
            }
            Map<String, Object> field = stringMap(rawMap);
            definitions.add(new EmployeeFieldDefinition(
                    normalizeColumn(stringValue(field.get("columnName"))),
                    stringValue(field.get("label")),
                    stringValue(field.get("heading")),
                    booleanValue(field.get("documentField")),
                    booleanValue(field.get("customField")),
                    booleanValue(field.get("protectedField")),
                    booleanValue(field.get("detailField")),
                    booleanValue(field.get("dateField")),
                    intValue(field.get("sortOrder"), 0),
                    booleanValue(field.get("coreField")),
                    booleanValue(field.get("dropdownField")),
                    booleanValue(field.get("variableOptionField")),
                    booleanValue(field.get("textAreaField")),
                    stringValue(field.get("dropdownOptions")),
                    booleanValue(field.get("requiredField"))
            ));
        }
        return normalizeDefinitions(definitions, true);
    }

    private static void atomicWrite(
            Path target,
            Path backup,
            String json,
            boolean keepBackupCopy
    ) throws IOException {
        Path cleanTarget = target.toAbsolutePath().normalize();
        Path parent = cleanTarget.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temp = cleanTarget.resolveSibling(cleanTarget.getFileName() + ".tmp");
        Files.writeString(temp, json, StandardCharsets.UTF_8);
        readFile(temp, false);

        if (keepBackupCopy && backup != null && Files.isRegularFile(cleanTarget)) {
            try {
                readFile(cleanTarget, false);
                Files.copy(cleanTarget, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException invalidExisting) {
                Path corrupt = cleanTarget.resolveSibling(cleanTarget.getFileName()
                        + ".corrupt-" + System.currentTimeMillis());
                try {
                    Files.move(cleanTarget, corrupt, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                }
            }
        }

        try {
            Files.move(temp, cleanTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailure) {
            Files.move(temp, cleanTarget, StandardCopyOption.REPLACE_EXISTING);
        }
        readFile(cleanTarget, false);
    }

    private static void recoverExternalFromBackup(Snapshot backup) {
        if (backup == null) {
            return;
        }
        try {
            atomicWrite(externalPath(), externalBackupPath(), toJson(new SnapshotPayload(
                    backup.schemaVersion(),
                    backup.updatedAt(),
                    backup.checksum(),
                    backup.dbChecksum(),
                    "external-recovered-from-backup",
                    backup.definitions()
            )), false);
        } catch (IOException ignored) {
        }
    }

    private static String toJson(SnapshotPayload payload) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"kind\": ").append(jsonString(KIND)).append(",\n");
        json.append("  \"schemaVersion\": ").append(payload.schemaVersion()).append(",\n");
        json.append("  \"updatedAt\": ").append(jsonString(payload.updatedAt().toString())).append(",\n");
        json.append("  \"checksum\": ").append(jsonString(payload.checksum())).append(",\n");
        json.append("  \"dbChecksum\": ").append(jsonString(payload.dbChecksum())).append(",\n");
        json.append("  \"source\": ").append(jsonString(payload.source())).append(",\n");
        json.append("  \"count\": ").append(payload.definitions().size()).append(",\n");
        json.append("  \"fields\": [\n");
        for (int index = 0; index < payload.definitions().size(); index++) {
            EmployeeFieldDefinition definition = payload.definitions().get(index);
            json.append("    {\n");
            appendJsonField(json, "columnName", definition.columnName(), true);
            appendJsonField(json, "label", definition.label(), true);
            appendJsonField(json, "heading", definition.heading(), true);
            appendJsonField(json, "documentField", definition.documentField(), true);
            appendJsonField(json, "customField", definition.customField(), true);
            appendJsonField(json, "protectedField", definition.protectedField(), true);
            appendJsonField(json, "detailField", definition.detailField(), true);
            appendJsonField(json, "dateField", definition.dateField(), true);
            appendJsonField(json, "sortOrder", definition.sortOrder(), true);
            appendJsonField(json, "coreField", definition.coreField(), true);
            appendJsonField(json, "dropdownField", definition.dropdownField(), true);
            appendJsonField(json, "variableOptionField", definition.variableOptionField(), true);
            appendJsonField(json, "textAreaField", definition.textAreaField(), true);
            appendJsonField(json, "dropdownOptions", definition.dropdownOptions(), true);
            appendJsonField(json, "requiredField", definition.requiredField(), false);
            json.append("    }");
            if (index + 1 < payload.definitions().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static void appendJsonField(StringBuilder json, String name, String value, boolean comma) {
        json.append("      ").append(jsonString(name)).append(": ").append(jsonString(value));
        if (comma) {
            json.append(',');
        }
        json.append('\n');
    }

    private static void appendJsonField(StringBuilder json, String name, boolean value, boolean comma) {
        json.append("      ").append(jsonString(name)).append(": ").append(value);
        if (comma) {
            json.append(',');
        }
        json.append('\n');
    }

    private static void appendJsonField(StringBuilder json, String name, int value, boolean comma) {
        json.append("      ").append(jsonString(name)).append(": ").append(value);
        if (comma) {
            json.append(',');
        }
        json.append('\n');
    }

    private static List<EmployeeFieldDefinition> normalizeDefinitions(
            List<EmployeeFieldDefinition> definitions,
            boolean requireNonEmpty
    ) {
        List<EmployeeFieldDefinition> normalized = new ArrayList<>();
        Set<String> columns = new LinkedHashSet<>();
        for (EmployeeFieldDefinition definition : definitions == null ? List.<EmployeeFieldDefinition>of() : definitions) {
            if (definition == null) {
                continue;
            }
            EmployeeFieldDefinition clean = normalizeDefinition(definition);
            if (!validColumnName(clean.columnName())) {
                throw new IllegalArgumentException("Invalid metadata column name: " + definition.columnName());
            }
            if (!columns.add(clean.columnName().toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate metadata column name: " + clean.columnName());
            }
            normalized.add(clean);
        }
        if (requireNonEmpty && normalized.isEmpty()) {
            throw new IllegalArgumentException("Metadata JSON must contain at least one field.");
        }
        normalized.sort(fieldOrder());
        return List.copyOf(normalized);
    }

    private static EmployeeFieldDefinition normalizeDefinition(EmployeeFieldDefinition definition) {
        String column = normalizeColumn(definition.columnName());
        boolean documentField = definition.documentField();
        boolean dateField = !documentField && definition.dateField();
        boolean dropdownField = !documentField && !dateField && definition.dropdownField();
        boolean variableOptionField = dropdownField && definition.variableOptionField();
        boolean textAreaField = !documentField && !dateField && !dropdownField && definition.textAreaField();
        String heading = valueOrDefault(definition.heading(), documentField ? "Documents" : "Additional Details");

        return new EmployeeFieldDefinition(
                column,
                valueOrDefault(definition.label(), titleFromColumn(column)),
                heading,
                documentField,
                definition.customField(),
                definition.protectedField(),
                !documentField && definition.detailField(),
                dateField,
                definition.sortOrder(),
                !documentField && definition.coreField(),
                dropdownField,
                variableOptionField,
                textAreaField,
                dropdownField ? normalizeOptions(definition.dropdownOptions()) : "",
                definition.requiredField()
        );
    }

    private static Comparator<EmployeeFieldDefinition> fieldOrder() {
        return Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EmployeeFieldDefinition::columnName, String.CASE_INSENSITIVE_ORDER);
    }

    private static Path appDataDir() {
        String configured = System.getProperty("kgm.metadata.appdata.dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("KGM_METADATA_APPDATA_DIR");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, APP_DIR_NAME).toAbsolutePath().normalize();
        }

        String home = System.getProperty("user.home", ".");
        return Path.of(home, "." + APP_DIR_NAME.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"))
                .toAbsolutePath()
                .normalize();
    }

    private static Path cacheDir() {
        String configured = System.getProperty("kgm.metadata.cache.dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("KGM_METADATA_CACHE_DIR");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, APP_DIR_NAME, "cache").toAbsolutePath().normalize();
        }
        return appDataDir().resolve("cache");
    }

    private static Map<String, Object> stringMap(Map<?, ?> raw) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IOException("Metadata JSON object key must be a string.");
            }
            map.put(key, entry.getValue());
        }
        return map;
    }

    private static String stringValue(Object value) throws IOException {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        throw new IOException("Metadata JSON value must be a string.");
    }

    private static boolean booleanValue(Object value) throws IOException {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IOException("Metadata JSON value must be a boolean.");
    }

    private static int intValue(Object value, int fallback) throws IOException {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("Metadata JSON value must be a number.");
    }

    private static Instant instantValue(Object value) throws IOException {
        String text = stringValue(value);
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException exception) {
            throw new IOException("Metadata JSON updatedAt is invalid: " + text, exception);
        }
    }

    private static boolean SCHEMA_VERSION_EQUALS(int version) {
        return version == SCHEMA_VERSION;
    }

    private static boolean validColumnName(String columnName) {
        return columnName != null && columnName.matches("[A-Z][A-Z0-9_]{0,63}");
    }

    private static String normalizeColumn(String columnName) {
        return columnName == null ? "" : columnName.trim().toUpperCase(Locale.ROOT);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalizeOptions(String options) {
        if (options == null || options.isBlank()) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String raw : options.split("[\\r\\n,]+")) {
            String option = raw.trim();
            if (option.isEmpty() || containsIgnoreCase(normalized, option)) {
                continue;
            }
            normalized.add(option);
        }
        return String.join("\n", normalized);
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String titleFromColumn(String column) {
        String[] parts = column.toLowerCase(Locale.ROOT).split("_+");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return title.isEmpty() ? column : title.toString();
    }

    private static boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static void appendCanonical(StringBuilder builder, String value) {
        String text = value == null ? "" : value;
        builder.append(text.length()).append(':').append(text).append('|');
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static String jsonString(String value) {
        String text = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(text.length() + 2);
        escaped.append('"');
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parse() throws IOException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Unexpected trailing JSON content.");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON.");
            }
            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected JSON value.");
                }
            };
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch != '\\') {
                    builder.append(ch);
                    continue;
                }
                if (index >= text.length()) {
                    throw error("Unclosed JSON escape.");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicodeEscape());
                    default -> throw error("Unsupported JSON escape.");
                }
            }
            throw error("Unclosed JSON string.");
        }

        private char parseUnicodeEscape() throws IOException {
            if (index + 4 > text.length()) {
                throw error("Invalid JSON unicode escape.");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON unicode escape.");
            }
        }

        private Object parseLiteral(String literal, Object value) throws IOException {
            if (!text.startsWith(literal, index)) {
                throw error("Invalid JSON literal.");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() throws IOException {
            int start = index;
            if (peek('-')) {
                index++;
            }
            readDigits();
            if (peek('.')) {
                index++;
                readDigits();
                return Double.parseDouble(text.substring(start, index));
            }
            return Long.parseLong(text.substring(start, index));
        }

        private void readDigits() throws IOException {
            int start = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("JSON number requires digits.");
            }
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("Expected '" + expected + "'.");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private IOException error(String message) {
            return new IOException(message + " At character " + index + ".");
        }
    }
}
