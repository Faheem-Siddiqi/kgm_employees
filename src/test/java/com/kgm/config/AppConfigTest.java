package com.kgm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {
    private static final String DOCUMENT_UPLOAD_PROPERTY = "kgm.document.upload.max.bytes";
    private static final String FIELD_SETTINGS_PROPERTY = "kgm.field.settings.password";
    private static final long DEFAULT_UPLOAD_LIMIT = 400L * 1024L;

    @AfterEach
    void clearProperties() {
        System.clearProperty(DOCUMENT_UPLOAD_PROPERTY);
        System.clearProperty(FIELD_SETTINGS_PROPERTY);
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
    void fieldSettingsPasswordUsesDedicatedJvmProperty() {
        System.setProperty(FIELD_SETTINGS_PROPERTY, "field-secret");

        assertEquals("field-secret", AppConfig.fieldSettingsPassword());
    }
}
