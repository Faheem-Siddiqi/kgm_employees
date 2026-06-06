package com.kgm.util;

import com.kgm.model.EmployeeFieldDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeFieldMetadataStoreTest {
    private static final String APPDATA_PROPERTY = "kgm.metadata.appdata.dir";
    private static final String CACHE_PROPERTY = "kgm.metadata.cache.dir";

    @AfterEach
    void clearProperties() {
        System.clearProperty(APPDATA_PROPERTY);
        System.clearProperty(CACHE_PROPERTY);
    }

    @Test
    void restoreSnapshotUsesValidCacheWhenExternalBackupIsMissing(@TempDir Path tempDir) throws Exception {
        System.setProperty(APPDATA_PROPERTY, tempDir.resolve("appdata").toString());
        System.setProperty(CACHE_PROPERTY, tempDir.resolve("cache").toString());
        List<EmployeeFieldDefinition> definitions = List.of(customDefinition());
        String checksum = EmployeeFieldMetadataStore.metadataChecksum(definitions);

        EmployeeFieldMetadataStore.saveCache(definitions, checksum);

        EmployeeFieldMetadataStore.Snapshot snapshot = EmployeeFieldMetadataStore.loadRestoreSnapshot();

        assertEquals("cache", snapshot.source());
        assertEquals("CUSTOM_TEST_FIELD", snapshot.definitions().get(0).columnName());
        assertTrue(Files.isRegularFile(EmployeeFieldMetadataStore.externalPath()));
    }

    private static EmployeeFieldDefinition customDefinition() {
        return new EmployeeFieldDefinition(
                "CUSTOM_TEST_FIELD",
                "Custom Test Field",
                "Additional Details",
                false,
                true,
                false,
                true,
                false,
                3010,
                false,
                false,
                false,
                false,
                "",
                true
        );
    }
}
