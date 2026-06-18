package com.kgm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelExportServiceTest {

    @Test
    void formatsInternalDatabaseDatesForExcelImportCompatibility() {
        assertEquals(
                "08/23/2010 00:00:00",
                ExcelExportService.formatExportDateValue("23/08/2010 00:00:00")
        );
    }

    @Test
    void keepsAlreadyCompatibleDatesStable() {
        assertEquals(
                "08/23/2010 00:00:00",
                ExcelExportService.formatExportDateValue("08/23/2010 00:00:00")
        );
    }

    @Test
    void formatsDateOnlyValuesWithMidnightTime() {
        assertEquals(
                "08/23/2010 00:00:00",
                ExcelExportService.formatExportDateValue("2010-08-23")
        );
    }

    @Test
    void leavesUnparseableLegacyValuesUnchanged() {
        assertEquals(
                "not a date",
                ExcelExportService.formatExportDateValue(" not a date ")
        );
    }

    @Test
    void treatsBlankDateValuesAsEmpty() {
        assertEquals("", ExcelExportService.formatExportDateValue(" N/A "));
        assertEquals("", ExcelExportService.formatExportDateValue(null));
    }
}
