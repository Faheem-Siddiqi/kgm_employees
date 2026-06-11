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
    private static final String DOT_ENV_FILE_PROPERTY = "kgm.env.file";
    private static final String EMPLOYEE_STORAGE_PROPERTY = "kgm.employee.storage.dir";
    private static final String EMPLOYEE_STORAGE_ON_SERVER_PROPERTY = "kgm.employee.storage.on.server";
    private final String originalUserDir = System.getProperty("user.dir");
    private final String originalJavaHome = System.getProperty("java.home");

    @TempDir
    Path tempDir;

    @AfterEach
    void clearUploadLimitProperty() {
        System.clearProperty(DOCUMENT_UPLOAD_PROPERTY);
        System.clearProperty(DOT_ENV_FILE_PROPERTY);
        System.clearProperty(EMPLOYEE_STORAGE_PROPERTY);
        System.clearProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY);
        System.setProperty("user.dir", originalUserDir);
        System.setProperty("java.home", originalJavaHome);
    }

    @Test
    void documentTypesIncludeResignApplicationDefault() {
        EmployeeDocumentUtil.DocumentType type = EmployeeDocumentUtil.documentTypes().stream()
                .filter(documentType -> "RESIGN_APPLICATION".equals(documentType.employeeFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(type);
        assertEquals("Resign Application", type.label());
        assertEquals("RESIGN_APPLICATION.jpg", type.storageName());
        assertTrue(type.aliases().contains("Resign_Application"));
    }

    @Test
    void documentMatcherAcceptsKnownLabelsAndRejectsIncorrectLabels() throws IOException {
        File valid = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);
        File invalid = writeJpeg(tempDir.resolve("random_wrong_label.jpg"), 160, 100, 0.9f);

        EmployeeDocumentUtil.DocumentMatch validMatch = EmployeeDocumentUtil.matchDocumentForFile(valid);
        EmployeeDocumentUtil.DocumentMatch invalidMatch = EmployeeDocumentUtil.matchDocumentForFile(invalid);

        assertTrue(validMatch.matched());
        assertEquals("CNIC Front", EmployeeDocumentUtil.cleanDocumentLabel(validMatch.documentIndex()));
        assertFalse(invalidMatch.matched());
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
    void prepareImageForUploadRejectsOversizedCorruptJpegWithValidityMessage() throws IOException {
        File corruptJpeg = tempDir.resolve("CNIC_FRONT.jpg").toFile();
        byte[] bytes = new byte[4096];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        Files.write(corruptJpeg.toPath(), bytes);
        System.setProperty(DOCUMENT_UPLOAD_PROPERTY, "10");

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

        assertEquals("employees/EMP_001/CNIC_FRONT.jpg", dbPath);
        Path copied = storageRoot.resolve("EMP_001").resolve("CNIC_FRONT.jpg");
        assertTrue(Files.exists(copied));
        assertEquals(copied.toFile(), EmployeeDocumentUtil.resolveStoredFile(dbPath));
        assertStorageRootProtected(storageRoot);
    }

    @Test
    void numericEmployeeCodeUsesExactFolderNameWithoutExtraZeros() throws IOException {
        Path storageRoot = tempDir.resolve("employee-storage");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, storageRoot.toString());
        File source = writeJpeg(tempDir.resolve("CNIC_FRONT.jpg"), 160, 100, 0.9f);

        String dbPath = EmployeeDocumentUtil.copyDocumentToEmployeeStorage("1", 0, source);

        assertEquals("employees/1/CNIC_FRONT.jpg", dbPath);
        assertTrue(Files.exists(storageRoot.resolve("1").resolve("CNIC_FRONT.jpg")));
        assertFalse(Files.exists(storageRoot.resolve("001")));
        assertFalse(Files.exists(storageRoot.resolve("1").resolve("documents")));
    }

    @Test
    void serverStorageModeUsesConfiguredEmployeeStoragePath() throws IOException {
        Path sharedRoot = tempDir.resolve("shared-employees");
        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "true");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, sharedRoot.toString());
        File source = writeJpeg(tempDir.resolve("CNIC_BACK.jpg"), 160, 100, 0.9f);

        String dbPath = EmployeeDocumentUtil.copyDocumentToEmployeeStorage("EMP:002", 1, source);

        assertEquals("employees/EMP_002/CNIC_BACK.jpg", dbPath);
        Path copied = sharedRoot.resolve("EMP_002").resolve("CNIC_BACK.jpg");
        assertTrue(Files.exists(copied));
        assertEquals(copied.toFile(), EmployeeDocumentUtil.resolveStoredFile(dbPath));
        assertStorageRootProtected(sharedRoot);
    }

    @Test
    void resolveStoredFileRemapsMissingAbsolutePathFromAnotherPcToConfiguredStorage() throws IOException {
        Path storageRoot = tempDir.resolve("shared-employees");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, storageRoot.toString());
        Path copied = storageRoot.resolve("1").resolve("CNIC_FRONT.jpg");
        Files.createDirectories(copied.getParent());
        Files.writeString(copied, "image-placeholder");

        File resolved = EmployeeDocumentUtil.resolveStoredFile("C:\\OldPc\\KGM\\employees\\1\\CNIC_FRONT.jpg");

        assertEquals(copied.toFile(), resolved);
    }

    @Test
    void resolveStoredFileStillSupportsLegacyNestedDocumentsPath() throws IOException {
        Path storageRoot = tempDir.resolve("shared-employees");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, storageRoot.toString());
        Path copied = storageRoot.resolve("1").resolve("documents").resolve("CNIC_FRONT.jpg");
        Files.createDirectories(copied.getParent());
        Files.writeString(copied, "image-placeholder");

        File resolved = EmployeeDocumentUtil.resolveStoredFile("employees/1/documents/CNIC_FRONT.jpg");

        assertEquals(copied.toFile(), resolved);
    }

    @Test
    void relativeResourcesEmployeesPathIsCreatedAndProtected() throws IOException {
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        System.setProperty(EMPLOYEE_STORAGE_ON_SERVER_PROPERTY, "false");
        System.setProperty(EMPLOYEE_STORAGE_PROPERTY, "resources/employees");

        Path root = EmployeeStorageUtil.ensureStorageRoot();

        Path expected = projectDir.resolve("resources").resolve("employees").toAbsolutePath().normalize();
        assertEquals(expected, root);
        assertTrue(Files.isDirectory(expected));
        assertStorageRootProtected(expected);
    }

    @Test
    void packagedRelativeResourcesEmployeesPathIsCreatedUnderAppFolder() throws IOException {
        Path projectDir = tempDir.resolve("project-for-packaged-run");
        Path appDir = tempDir.resolve("packaged-app");
        Files.createDirectories(projectDir);
        Files.createDirectories(appDir.resolve("runtime"));
        Files.writeString(appDir.resolve(".env"),
                "KGM_EMPLOYEE_STORAGE_ON_SERVER=false\nKGM_EMPLOYEE_STORAGE_DIR=resources/employees\n");
        System.setProperty("user.dir", projectDir.toString());
        System.setProperty("java.home", appDir.resolve("runtime").toString());

        Path root = EmployeeStorageUtil.ensureStorageRoot();

        Path expected = appDir.resolve("resources").resolve("employees").toAbsolutePath().normalize();
        assertEquals(expected, root);
        assertTrue(Files.isDirectory(expected));
        assertStorageRootProtected(expected);
    }

    @Test
    void blankLocalStorageEnvIsRepairedToProjectResourcesEmployees() throws IOException {
        Path projectDir = tempDir.resolve("project-with-blank-env");
        Files.createDirectories(projectDir);
        Path dotEnv = projectDir.resolve(".env");
        Files.writeString(dotEnv, "KGM_EMPLOYEE_STORAGE_ON_SERVER=false\nKGM_EMPLOYEE_STORAGE_DIR=\n");
        System.setProperty("user.dir", projectDir.toString());

        Path root = EmployeeStorageUtil.ensureStorageRoot();

        Path expected = projectDir.resolve("resources").resolve("employees").toAbsolutePath().normalize();
        assertEquals(expected, root);
        assertTrue(Files.isDirectory(expected));
        assertStorageRootProtected(expected);
        String repairedEnv = Files.readString(dotEnv);
        assertTrue(repairedEnv.contains("KGM_EMPLOYEE_STORAGE_ON_SERVER=false"));
        assertTrue(repairedEnv.contains("KGM_EMPLOYEE_STORAGE_DIR=resources/employees"));
    }

    @Test
    void missingLocalStorageEnvIsAddedAsProjectResourcesEmployees() throws IOException {
        Path projectDir = tempDir.resolve("project-with-missing-env");
        Files.createDirectories(projectDir);
        Path dotEnv = projectDir.resolve(".env");
        Files.writeString(dotEnv, "KGM_EMPLOYEE_STORAGE_ON_SERVER=false\n");
        System.setProperty("user.dir", projectDir.toString());

        Path root = EmployeeStorageUtil.ensureStorageRoot();

        Path expected = projectDir.resolve("resources").resolve("employees").toAbsolutePath().normalize();
        assertEquals(expected, root);
        assertTrue(Files.isDirectory(expected));
        assertStorageRootProtected(expected);
        String repairedEnv = Files.readString(dotEnv);
        assertTrue(repairedEnv.contains("KGM_EMPLOYEE_STORAGE_ON_SERVER=false"));
        assertTrue(repairedEnv.contains("KGM_EMPLOYEE_STORAGE_DIR=resources/employees"));
    }

    @Test
    void missingDotEnvIsCreatedWithProjectResourcesEmployees() throws IOException {
        Path projectDir = tempDir.resolve("project-without-env");
        Files.createDirectories(projectDir);
        Path dotEnv = projectDir.resolve(".env");
        System.setProperty("user.dir", projectDir.toString());

        Path root = EmployeeStorageUtil.ensureStorageRoot();

        Path expected = projectDir.resolve("resources").resolve("employees").toAbsolutePath().normalize();
        assertEquals(expected, root);
        assertTrue(Files.isDirectory(expected));
        assertStorageRootProtected(expected);
        String repairedEnv = Files.readString(dotEnv);
        assertTrue(repairedEnv.contains("KGM_EMPLOYEE_STORAGE_ON_SERVER=false"));
        assertTrue(repairedEnv.contains("KGM_EMPLOYEE_STORAGE_DIR=resources/employees"));
    }

    @Test
    void customLocalStorageEnvPathIsCreatedWhenMissing() throws IOException {
        Path projectDir = tempDir.resolve("project-with-custom-env");
        Files.createDirectories(projectDir);
        Path dotEnv = projectDir.resolve(".env");
        Files.writeString(dotEnv, "KGM_EMPLOYEE_STORAGE_ON_SERVER=false\nKGM_EMPLOYEE_STORAGE_DIR=custom-employee-data\n");
        System.setProperty("user.dir", projectDir.toString());

        Path root = EmployeeStorageUtil.ensureStorageRoot();

        Path expected = projectDir.resolve("custom-employee-data").toAbsolutePath().normalize();
        assertEquals(expected, root);
        assertTrue(Files.isDirectory(expected));
        assertStorageRootProtected(expected);
        String unchangedEnv = Files.readString(dotEnv);
        assertTrue(unchangedEnv.contains("KGM_EMPLOYEE_STORAGE_DIR=custom-employee-data"));
        assertFalse(unchangedEnv.contains("KGM_EMPLOYEE_STORAGE_DIR=resources/employees"));
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

    private void assertStorageRootProtected(Path storageRoot) throws IOException {
        Path marker = storageRoot.resolve(".gitignore");
        assertTrue(Files.isRegularFile(marker));
        String content = Files.readString(marker);
        assertTrue(content.contains("*"));
        assertTrue(content.contains("!.gitignore"));
        assertTrue(content.contains("!.gitkeep"));
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
