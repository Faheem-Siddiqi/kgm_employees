package com.kgm.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeDocumentUtilTest {
    private static final String DOCUMENT_UPLOAD_PROPERTY = "kgm.document.upload.max.bytes";
    private static final String EMPLOYEE_STORAGE_PROPERTY = "kgm.employee.storage.dir";

    @TempDir
    Path tempDir;

    @AfterEach
    void clearUploadLimitProperty() {
        System.clearProperty(DOCUMENT_UPLOAD_PROPERTY);
        System.clearProperty(EMPLOYEE_STORAGE_PROPERTY);
    }

    @Test
    void prepareImageForUploadAcceptsReadableJpegUnderConfiguredLimit() throws IOException {
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(source.length() + 1024));

        EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(source);

        assertTrue(prepared.ready());
        assertFalse(prepared.compressed());
        assertEquals(source, prepared.file());
        assertEquals(source, prepared.originalFile());
        assertFalse(EmployeeDocumentUtil.isTemporaryUploadFile(prepared.file()));
    }

    @Test
    void prepareImageForUploadAcceptsJpegExtensionsIgnoringCase() throws IOException {
        File jpg = writeJpeg(tempDir.resolve("CNIC_FRONT.JPG"), 160, 100, 0.9f);
        File jpeg = writeJpeg(tempDir.resolve("CNIC_BACK.JPEG"), 160, 100, 0.9f);
        File jpe = writeJpeg(tempDir.resolve("EOBI.JPE"), 160, 100, 0.9f);
        File jfif = writeJpeg(tempDir.resolve("SS_CARD.JFIF"), 160, 100, 0.9f);
        long largest = Math.max(Math.max(jpg.length(), jpeg.length()), Math.max(jpe.length(), jfif.length()));
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(largest + 1024));

        assertTrue(EmployeeDocumentUtil.prepareImageForUpload(jpg).ready());
        assertTrue(EmployeeDocumentUtil.prepareImageForUpload(jpeg).ready());
        assertTrue(EmployeeDocumentUtil.prepareImageForUpload(jpe).ready());
        assertTrue(EmployeeDocumentUtil.prepareImageForUpload(jfif).ready());
    }

    @Test
    void prepareImageForUploadRejectsNonJpegContentEvenWithJpgExtension() throws IOException {
        File fakeJpeg = tempDir.resolve("CNIC_FRONT.jpg").toFile();
        Files.writeString(fakeJpeg.toPath(), "this is not image data");
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(fakeJpeg.length() + 1024));

        EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(fakeJpeg);

        assertFalse(prepared.ready());
        assertEquals("Please select a valid JPG or JPEG image.", prepared.message());
    }

    @Test
    void prepareImageForUploadRejectsCorruptJpegSignature() throws IOException {
        File corruptJpeg = tempDir.resolve("CNIC_FRONT.jpg").toFile();
        Files.write(corruptJpeg.toPath(), new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00, 0x00});
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(corruptJpeg.length() + 1024));

        EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(corruptJpeg);

        assertFalse(prepared.ready());
        assertEquals("Please select a valid JPG or JPEG image.", prepared.message());
    }

    @Test
    void prepareImageForUploadCompressesLargeJpegWithoutChangingDimensions() throws IOException {
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 900, 700, 1.0f);
        long limit = Math.max(1L, source.length() - 1L);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(limit));

        EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(source);

        assertTrue(prepared.ready(), prepared.message());
        assertTrue(prepared.compressed());
        assertTrue(prepared.file().length() <= limit);
        assertTrue(EmployeeDocumentUtil.isTemporaryUploadFile(prepared.file()));

        BufferedImage original = ImageIO.read(source);
        BufferedImage compressed = ImageIO.read(prepared.file());
        assertNotNull(compressed);
        assertEquals(original.getWidth(), compressed.getWidth());
        assertEquals(original.getHeight(), compressed.getHeight());

        EmployeeDocumentUtil.deleteTemporaryUpload(prepared.file());
        assertFalse(prepared.file().exists());
    }

    @Test
    void prepareImageForUploadFindsBestQualityBelowOldCompressionFloor() throws IOException {
        BufferedImage image = gradientImage(900, 700);
        File source = tempDir.resolve("CNIC_FRONT.jpg").toFile();
        File oldFloor = tempDir.resolve("old-floor.jpg").toFile();
        File lowFloor = tempDir.resolve("low-floor.jpg").toFile();
        writeJpeg(image, source, 1.0f);
        BufferedImage decodedSource = ImageIO.read(source);
        writeJpeg(decodedSource, oldFloor, 0.55f);
        writeJpeg(decodedSource, lowFloor, 0.01f);
        assertTrue(lowFloor.length() < oldFloor.length(), "test image should compress below the previous 55% floor");
        long limit = (oldFloor.length() + lowFloor.length()) / 2L;
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(limit));

        EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(source);

        assertTrue(prepared.ready(), prepared.message());
        assertTrue(prepared.compressed());
        assertTrue(prepared.file().length() <= limit);

        EmployeeDocumentUtil.deleteTemporaryUpload(prepared.file());
    }

    @Test
    void prepareImageForUploadRejectsWhenEvenMinimumQualityCannotFitLimit() throws IOException {
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 400, 300, 1.0f);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "10");

        EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(source);

        assertFalse(prepared.ready());
        assertTrue(prepared.message().contains("compressed under"));
        assertFalse(prepared.message().startsWith("File size is"));
    }

    @Test
    void shouldCompressBeforeUploadUsesConfiguredThresholdOnlyForJpegs() throws IOException {
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);
        File text = tempDir.resolve("CNIC_FRONT.txt").toFile();
        Files.writeString(text.toPath(), "not an image");

        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(source.length() - 1));
        assertTrue(EmployeeDocumentUtil.shouldCompressBeforeUpload(source));
        assertFalse(EmployeeDocumentUtil.shouldCompressBeforeUpload(text));

        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(source.length() + 1));
        assertFalse(EmployeeDocumentUtil.shouldCompressBeforeUpload(source));
    }

    @Test
    void validateImageFileDoesNotRejectReadableJpegOnlyBecauseItExceedsLimit() throws IOException {
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(source.length() - 1));

        assertNull(EmployeeDocumentUtil.validateImageFile(source));
    }

    @Test
    void copyDocumentToEmployeeStorageCopiesFileAndReturnsLogicalRelativePath() throws IOException {
        Path storageRoot = tempDir.resolve("employee-storage");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, storageRoot.toString());
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);

        String dbPath = EmployeeDocumentUtil.copyDocumentToEmployeeStorage("EMP/001", 0, source);

        assertEquals("employees/EMP_001/documents/CNIC_FRONT.jpg", dbPath);
        assertTrue(Files.exists(storageRoot.resolve("EMP_001").resolve("documents").resolve("CNIC_FRONT.jpg")));
    }

    @Test
    void matchBulkFilesPrevalidatesAndReportsUnsupportedUnmatchedDuplicateAndCompressedFiles() throws IOException {
        File first = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 900, 700, 1.0f);
        File duplicate = writeJpeg(tempDir.resolve("CNIC Front Duplicate.jpeg"), 900, 700, 1.0f);
        File unsupported = tempDir.resolve("CNIC_BACK.png").toFile();
        Files.writeString(unsupported.toPath(), "not a jpeg");
        File unmatched = writeJpeg(tempDir.resolve("unknown-document.jpg"), 120, 80, 0.8f);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(first.length() - 1));

        List<String> progressMessages = new ArrayList<>();
        EmployeeDocumentUtil.BulkUploadResult result = EmployeeDocumentUtil.matchBulkFiles(
                new File[]{first, duplicate, unsupported, unmatched},
                null,
                (message, completedFiles, totalFiles, percent) -> progressMessages.add(message)
        );

        assertEquals(1, result.uploadedCount());
        assertEquals(3, result.discardedCount());
        assertTrue(result.uploadedDocuments().get(0).compressed());
        assertTrue(result.discardedDetails().contains("Another selected file already matched CNIC Front."));
        assertTrue(result.discardedDetails().contains("Unsupported file type"));
        assertTrue(result.discardedDetails().contains("No document label matched this file name."));
        assertTrue(progressMessages.stream().anyMatch(message -> message.contains("Compressing CNIC Front")));

        EmployeeDocumentUtil.deleteTemporaryUpload(result.uploadedDocuments().get(0).file());
    }

    @Test
    void matchBulkFilesKeepsInLimitFilesOnFastPathAndCompressesOnlyOversizedMatches() throws IOException {
        File inLimit = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);
        File oversized = writeJpeg(tempDir.resolve("CNIC_BACK.jpeg"), 900, 700, 1.0f);
        long limit = Math.min(
                oversized.length() - 1,
                inLimit.length() + Math.max(1024L, (oversized.length() - inLimit.length()) / 3L)
        );
        assertTrue(inLimit.length() <= limit, "small test file should be under the configured limit");
        assertTrue(oversized.length() > limit, "large test file should exceed the configured limit");
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(limit));
        List<String> progressMessages = new ArrayList<>();

        EmployeeDocumentUtil.BulkUploadResult result = EmployeeDocumentUtil.matchBulkFiles(
                new File[]{inLimit, oversized},
                null,
                (message, completedFiles, totalFiles, percent) -> progressMessages.add(message)
        );

        assertEquals(2, result.uploadedCount());
        assertEquals(0, result.discardedCount());

        EmployeeDocumentUtil.BulkUploadItem fastPath = uploadedItemForOriginal(result, inLimit);
        assertFalse(fastPath.compressed());
        assertEquals(inLimit, fastPath.file());
        assertEquals(inLimit, fastPath.originalFile());
        assertFalse(EmployeeDocumentUtil.isTemporaryUploadFile(fastPath.file()));

        EmployeeDocumentUtil.BulkUploadItem compressedPath = uploadedItemForOriginal(result, oversized);
        assertTrue(compressedPath.compressed());
        assertEquals(oversized, compressedPath.originalFile());
        assertTrue(EmployeeDocumentUtil.isTemporaryUploadFile(compressedPath.file()));
        assertTrue(compressedPath.file().length() <= limit);
        assertTrue(progressMessages.stream().anyMatch(message -> message.contains("Compressing CNIC Back")));
        assertFalse(progressMessages.stream().anyMatch(message -> message.contains("Compressing CNIC Front")));

        EmployeeDocumentUtil.deleteTemporaryUpload(compressedPath.file());
    }

    @Test
    void matchBulkFilesSkipsLockedDocumentsBeforeCompressionWork() throws IOException {
        File lockedFile = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 900, 700, 1.0f);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(lockedFile.length() - 1));
        boolean[] lockedDocuments = new boolean[EmployeeDocumentUtil.documentCount()];
        lockedDocuments[0] = true;
        List<String> progressMessages = new ArrayList<>();

        EmployeeDocumentUtil.BulkUploadResult result = EmployeeDocumentUtil.matchBulkFiles(
                new File[]{lockedFile},
                lockedDocuments,
                (message, completedFiles, totalFiles, percent) -> progressMessages.add(message)
        );

        assertEquals(0, result.uploadedCount());
        assertEquals(1, result.discardedCount());
        assertTrue(result.discardedDetails().contains("already exists in DB"));
        assertFalse(progressMessages.stream().anyMatch(message -> message.contains("Compressing")));
    }

    @Test
    void matchBulkFilesSkipsUnmatchedOversizedFilesBeforeCompressionWork() throws IOException {
        File unmatchedFile = writeJpeg(tempDir.resolve("unknown-document.jpg"), 900, 700, 1.0f);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, Long.toString(unmatchedFile.length() - 1));
        List<String> progressMessages = new ArrayList<>();

        EmployeeDocumentUtil.BulkUploadResult result = EmployeeDocumentUtil.matchBulkFiles(
                new File[]{unmatchedFile},
                null,
                (message, completedFiles, totalFiles, percent) -> progressMessages.add(message)
        );

        assertEquals(0, result.uploadedCount());
        assertEquals(1, result.discardedCount());
        assertTrue(result.discardedDetails().contains("No document label matched"));
        assertFalse(progressMessages.stream().anyMatch(message -> message.contains("Compressing")));
    }

    @Test
    void deleteTemporaryUploadOnlyDeletesPreparedUploadTemps() throws IOException {
        File normalFile = writeJpeg(tempDir.resolve("kgm-upload-normal-name.jpg"), 120, 80, 0.8f);

        EmployeeDocumentUtil.deleteTemporaryUpload(normalFile);

        assertTrue(normalFile.exists());
    }

    private File writeJpeg(Path path, int width, int height, float quality) throws IOException {
        BufferedImage image = gradientImage(width, height);
        writeJpeg(image, path.toFile(), quality);
        return path.toFile();
    }

    private BufferedImage gradientImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int red = (x * 255) / Math.max(1, width - 1);
                    int green = (y * 255) / Math.max(1, height - 1);
                    int blue = ((x + y) * 127) / Math.max(1, width + height - 2);
                    image.setRGB(x, y, new Color(red, green, blue).getRGB());
                }
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private EmployeeDocumentUtil.BulkUploadItem uploadedItemForOriginal(
            EmployeeDocumentUtil.BulkUploadResult result,
            File original
    ) {
        for (EmployeeDocumentUtil.BulkUploadItem item : result.uploadedDocuments()) {
            if (original.equals(item.originalFile())) {
                return item;
            }
        }
        throw new AssertionError("No uploaded item found for " + original.getName());
    }

    private void writeJpeg(BufferedImage image, File target, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available for test.");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            try (ImageOutputStream output = ImageIO.createImageOutputStream(target)) {
                writer.setOutput(output);
                writer.write(null, new IIOImage(image, null, null), params);
            }
        } finally {
            writer.dispose();
        }
    }

}
