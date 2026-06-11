package com.kgm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {
    private static final String DOCUMENT_UPLOAD_PROPERTY = "kgm.document.upload.max.bytes";
    private static final String FIELD_SETTINGS_PROPERTY = "kgm.field.settings.password";
    private static final String DOT_ENV_FILE_PROPERTY = "kgm.env.file";
    private static final String EMPLOYEE_STORAGE_PROPERTY = "kgm.employee.storage.dir";
    private static final String EMPLOYEE_STORAGE_ON_SERVER_PROPERTY = "kgm.employee.storage.on.server";
    private static final String BULK_IMPORT_COMPRESSION_PROPERTY = "kgm.bulk.import.compression";
    private static final long DEFAULT_UPLOAD_LIMIT = 400L * 1024L;
    private final String originalUserDir = System.getProperty("user.dir");
    private final String originalJavaHome = System.getProperty("java.home");

    @TempDir
    Path tempDir;

    @AfterEach
    void clearProperties() {
        System.clearProperty(DOCUMENT_UPLOAD_PROPERTY);
        System.clearProperty(FIELD_SETTINGS_PROPERTY);
        System.clearProperty(DOT_ENV_FILE_PROPERTY);
        System.clearProperty(EMPLOYEE_STORAGE_PROPERTY);
        System.clearProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY);
        System.clearProperty(BULK_IMPORT_COMPRESSION_PROPERTY);
        System.setProperty("user.dir", originalUserDir);
        System.setProperty("java.home", originalJavaHome);
    }

    @Test
    void documentUploadMaxBytesUsesJvmPropertyAndNormalizesSeparators() {
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "614,400");

        assertEquals(614_400L, AppConfig.documentUploadMaxBytes());

        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "1_024_000");

        assertEquals(1_024_000L, AppConfig.documentUploadMaxBytes());
    }

    @Test
    void documentUploadMaxBytesFallsBackWhenConfiguredValueIsInvalid() {
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "0");
        assertEquals(DEFAULT_UPLOAD_LIMIT, AppConfig.documentUploadMaxBytes());

        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "-1");
        assertEquals(DEFAULT_UPLOAD_LIMIT, AppConfig.documentUploadMaxBytes());

        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "not-a-number");
        assertEquals(DEFAULT_UPLOAD_LIMIT, AppConfig.documentUploadMaxBytes());
    }

    @Test
    void documentUploadMaxBytesSupportsUnprefixedEnvAliasFromDotEnv() throws IOException {
        Path projectDir = tempDir.resolve("upload-alias-env");
        Path env = projectDir.resolve(".env");
        Files.createDirectories(projectDir);
        Files.writeString(env, "DOCUMENT_UPLOAD_MAX_BYTES=600000\n");
        System.setProperty(DOT_ENV_FILE_PROPERTY, env.toString());

        assertEquals(600_000L, AppConfig.documentUploadMaxBytes());
    }

    @Test
    void bulkImportFolderDirectoryUsesEmployeeStoragePath() {
        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "false");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, "custom-employee-data");

        Path expected = Path.of(System.getProperty("user.dir"), "custom-employee-data")
                .toAbsolutePath()
                .normalize();

        assertEquals(expected, AppConfig.bulkImportFolderDirectory());
    }

    @Test
    void bulkImportCompressionParsesBooleanSwitch() {
        System.setProperty(BULK_IMPORT_COMPRESSION_PROPERTY, "false");
        assertFalse(AppConfig.bulkImportCompressionEnabled());

        System.setProperty(BULK_IMPORT_COMPRESSION_PROPERTY, "true");
        assertTrue(AppConfig.bulkImportCompressionEnabled());
    }

    @Test
    void fieldSettingsPasswordUsesDedicatedJvmProperty() {
        System.setProperty(FIELD_SETTINGS_PROPERTY, "field-secret");

        assertEquals("field-secret", AppConfig.fieldSettingsPassword());
    }

    @Test
    void employeeStorageOnServerParsesBooleanSwitch() {
        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "true");
        assertTrue(AppConfig.employeeStorageOnServer());

        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "false");
        assertFalse(AppConfig.employeeStorageOnServer());

        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "not-a-boolean");
        assertFalse(AppConfig.employeeStorageOnServer());
    }

    @Test
    void employeeStorageDirectoryUsesConfiguredLocalPathWhenServerModeIsDisabled() {
        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "false");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, "custom-employee-data");

        Path expected = Path.of(System.getProperty("user.dir"), "custom-employee-data")
                .toAbsolutePath()
                .normalize();

        assertEquals(expected, AppConfig.employeeStorageDirectory());
    }

    @Test
    void employeeStorageDirectoryUsesConfiguredStoragePathWhenServerModeIsEnabled() {
        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "true");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, "custom-employee-data");

        Path expected = Path.of(System.getProperty("user.dir"), "custom-employee-data")
                .toAbsolutePath()
                .normalize();

        assertEquals(expected, AppConfig.employeeStorageDirectory());
    }

    @Test
    void employeeStorageDirectoryUsesProjectResourcesWhenLocalPathIsBlank() throws IOException {
        Path projectDir = tempDir.resolve("project-with-blank-env");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve(".env"), "KGM_EMPLOYEE_STORAGE_ON_SERVER=false\nKGM_EMPLOYEE_STORAGE_DIR=\n");
        System.setProperty("user.dir", projectDir.toString());

        Path expected = projectDir.resolve("resources").resolve("employees")
                .toAbsolutePath()
                .normalize();

        assertEquals(expected, AppConfig.employeeStorageDirectory());
    }

    @Test
    void employeeStorageDirectoryUsesPackagedAppFolderForRelativeLocalPath() throws IOException {
        Path projectDir = tempDir.resolve("project");
        Path appDir = tempDir.resolve("packaged-app");
        Files.createDirectories(projectDir);
        Files.createDirectories(appDir.resolve("runtime"));
        Files.writeString(appDir.resolve(".env"),
                "KGM_EMPLOYEE_STORAGE_ON_SERVER=false\nKGM_EMPLOYEE_STORAGE_DIR=resources/employees\n");
        System.setProperty("user.dir", projectDir.toString());
        System.setProperty("java.home", appDir.resolve("runtime").toString());

        Path expected = appDir.resolve("resources").resolve("employees")
                .toAbsolutePath()
                .normalize();

        assertEquals(expected, AppConfig.employeeStorageDirectory());
    }

    @Test
    void saveEmployeeServerStorageDirectoryUpdatesDotEnvValues() throws IOException {
        Path projectDir = tempDir.resolve("server-storage-config");
        Path env = projectDir.resolve(".env");
        Files.createDirectories(projectDir);
        Files.writeString(env, """
                KGM_EMPLOYEE_STORAGE_ON_SERVER=false
                KGM_EMPLOYEE_STORAGE_DIR=resources/employees
                """);
        System.setProperty(DOT_ENV_FILE_PROPERTY, env.toString());

        AppConfig.saveEmployeeServerStorageDirectory("\\\\192.168.2.93\\employees");

        String updated = Files.readString(env);
        assertTrue(updated.contains("KGM_EMPLOYEE_STORAGE_ON_SERVER=true"));
        assertTrue(updated.contains("KGM_EMPLOYEE_STORAGE_DIR=\\\\192.168.2.93\\employees"));
    }
}
