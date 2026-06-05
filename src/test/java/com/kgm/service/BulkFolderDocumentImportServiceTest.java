package com.kgm.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkFolderDocumentImportServiceTest {
    @Test
    void employeeFolderLookupNamesUsesExactFolderNameFirst() {
        List<String> lookupNames = BulkFolderDocumentImportService.employeeFolderLookupNames("00050");

        assertEquals(List.of("00050"), lookupNames);
    }

    @Test
    void employeeFolderLookupNamesExtractsEmployeeIdFromFriendlyFolderName() {
        List<String> lookupNames = BulkFolderDocumentImportService.employeeFolderLookupNames("00050 - Ali Khan");

        assertEquals(List.of("00050 - Ali Khan", "00050"), lookupNames);
    }

    @Test
    void employeeFolderLookupNamesPrefersLabeledAndZeroPaddedIdsBeforeOtherNumbers() {
        List<String> lookupNames = BulkFolderDocumentImportService.employeeFolderLookupNames(
                "Batch 2024 Employee ID 00123 Documents"
        );

        assertEquals(
                List.of("Batch 2024 Employee ID 00123 Documents", "00123", "2024"),
                lookupNames
        );
    }

    @Test
    void employeeFolderLookupNamesStillSupportsShortNumericIdsInsideNames() {
        List<String> lookupNames = BulkFolderDocumentImportService.employeeFolderLookupNames("Ali Khan 50");

        assertEquals(List.of("Ali Khan 50", "50"), lookupNames);
    }
}
