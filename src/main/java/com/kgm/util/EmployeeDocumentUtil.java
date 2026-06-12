package com.kgm.util;

import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;

import com.kgm.config.AppConfig;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EmployeeDocumentUtil {
    private static final String UPLOAD_TEMP_PREFIX = "kgm-upload-";
    private static final float MIN_JPEG_QUALITY = 0.01f;
    private static final float MAX_JPEG_QUALITY = 0.92f;
    private static final float QUALITY_PRECISION = 0.01f;
    private static volatile List<DocumentType> cachedDocumentTypes;
    private static volatile Set<String> cachedRequiredDocumentColumns;
    private static final Set<String> DEFAULT_REQUIRED_DOCUMENT_COLUMNS = Set.of(
            "CNIC_FRONT",
            "CNIC_BACK",
            "FINAL_SETTLEMENT",
            "APPOINTMENT_LETTER_FRONT",
            "APPOINTMENT_LETTER_BACK",
            "APPLICATION_FRONT",
            "APPLICATION_BACK",
            "CLEARANCE_CERTIFICATE",
            "EMP_IMG"
    );

    private static final List<DocumentType> DOCUMENT_TYPES = List.of(
            new DocumentType("CNIC Front", "CNIC_FRONT", "CNIC_FRONT.jpg", "CNIC_Front"),
            new DocumentType("CNIC Back", "CNIC_BACK", "CNIC_BACK.jpg", "CNIC_Back"),
            new DocumentType("EOBI", "EOBI", "EOBI.jpg", "EOBI Card", "EOBI Card Copy"),
            new DocumentType("Social Security Card", "SS_CARD", "SS_CARD.jpg", "SS_Card", "Social Security", "SS"),
            new DocumentType("Final Settlement", "FINAL_SETTLEMENT", "FINAL_SETTLEMENT.jpg"),
            new DocumentType("Appointment Letter Front", "APPOINTMENT_LETTER_FRONT", "APPOINTMENT_LETTER_FRONT.jpg", "Appointment_letter_Front"),
            new DocumentType("Appointment Letter Back", "APPOINTMENT_LETTER_BACK", "APPOINTMENT_LETTER_BACK.jpg", "Appointment_Letter_Back"),
            new DocumentType("Application Front", "APPLICATION_FRONT", "APPLICATION_FRONT.jpg", "Application_Front"),
            new DocumentType("Application Back", "APPLICATION_BACK", "APPLICATION_BACK.jpg", "Application_Back"),
            new DocumentType("Resign Application", "RESIGN_APPLICATION", "RESIGN_APPLICATION.jpg", "Resign_Application"),
            new DocumentType("Clearance Certificate", "CLEARANCE_CERTIFICATE", "CLEARANCE_CERTIFICATE.jpg"),
            new DocumentType("Service Certificate", "SERVICE_CERTIFICATE", "SERVICE_CERTIFICATE.jpg", "Service Certificate/"),
            new DocumentType("Payment Voucher", "PAYMENT_VOUCHER", "PAYMENT_VOUCHER.jpg"),
            new DocumentType("Trial Card", "TRIAL_CARD", "TRIAL_CARD.jpg"),
            new DocumentType("Medical", "MEDICAL_DOC", "MEDICAL_DOC.jpg", "Medical Document"),
            new DocumentType("Interview Forms", "INTERVIEW_FORMS", "INTERVIEW_FORMS.jpg", "Interview Form"),
            new DocumentType("Covid Certificate", "COVID_CERTIFICATE", "COVID_CERTIFICATE.jpg", "Cvoid Certificate", "Covid Certification"),
            new DocumentType("Disciplinary I", "DISCIPLINARY_I", "DISCIPLINARY_I.jpg"),
            new DocumentType("Disciplinary II", "DISCIPLINARY_II", "DISCIPLINARY_II.jpg"),
            new DocumentType("Disciplinary III", "DISCIPLINARY_III", "DISCIPLINARY_III.jpg"),
            new DocumentType("Miscellaneous I", "MISCELLANEOUS_I", "MISCELLANEOUS_I.jpg", "Miscellaneous-I"),
            new DocumentType("Miscellaneous II", "MISCELLANEOUS_II", "MISCELLANEOUS_II.jpg", "Miscellaneous-II"),
            new DocumentType("Miscellaneous III", "MISCELLANEOUS_III", "MISCELLANEOUS_III.jpg", "Miscellaneous-III")
    );

    private EmployeeDocumentUtil() {
    }

    public static List<DocumentType> documentTypes() {
        List<DocumentType> cached = cachedDocumentTypes;
        if (cached != null) {
            return cached;
        }
        synchronized (EmployeeDocumentUtil.class) {
            if (cachedDocumentTypes == null) {
                cachedDocumentTypes = loadDocumentTypes();
            }
            return cachedDocumentTypes;
        }
    }

    public static void refreshDocumentTypes() {
        synchronized (EmployeeDocumentUtil.class) {
            cachedDocumentTypes = null;
            cachedRequiredDocumentColumns = null;
        }
    }

    public static boolean[] requiredDocumentFlags() {
        Set<String> requiredColumns = requiredDocumentColumns();
        List<DocumentType> types = documentTypes();
        boolean[] flags = new boolean[types.size()];
        for (int index = 0; index < types.size(); index++) {
            flags[index] = requiredColumns.contains(types.get(index).employeeFieldName().toUpperCase(Locale.ROOT));
        }
        return flags;
    }

    public static boolean isProfileImageRequired() {
        return requiredDocumentColumns().contains("EMP_IMG");
    }

    public static List<String> missingRequiredDocumentLabels(String[] documentPaths) {
        boolean[] required = requiredDocumentFlags();
        List<String> missing = new ArrayList<>();
        for (int index = 0; index < required.length; index++) {
            String path = documentPaths != null && index < documentPaths.length ? documentPaths[index] : null;
            if (required[index] && !hasStoredPath(path)) {
                missing.add(cleanDocumentLabel(index));
            }
        }
        return missing;
    }

    private static Set<String> requiredDocumentColumns() {
        Set<String> cached = cachedRequiredDocumentColumns;
        if (cached != null) {
            return cached;
        }
        synchronized (EmployeeDocumentUtil.class) {
            if (cachedRequiredDocumentColumns == null) {
                cachedRequiredDocumentColumns = loadRequiredDocumentColumns();
            }
            return cachedRequiredDocumentColumns;
        }
    }

    private static Set<String> loadRequiredDocumentColumns() {
        Set<String> columns = new HashSet<>();
        try {
            for (EmployeeFieldDefinition definition : EmployeeFieldDefinitionCache.documentFields()) {
                if (definition.requiredField()) {
                    columns.add(definition.columnName().toUpperCase(Locale.ROOT));
                }
            }
        } catch (RuntimeException exception) {
            columns.addAll(DEFAULT_REQUIRED_DOCUMENT_COLUMNS);
        }
        return Set.copyOf(columns);
    }

    private static List<DocumentType> loadDocumentTypes() {
        List<DocumentType> types = new ArrayList<>();
        try {
            Map<String, EmployeeFieldDefinition> metadata = new LinkedHashMap<>();
            EmployeeFieldDefinition profileImageDefinition = null;
            for (EmployeeFieldDefinition definition : EmployeeFieldDefinitionCache.fields()) {
                if (isProfileImageColumn(definition.columnName())) {
                    profileImageDefinition = definition;
                    continue;
                }
                if (definition.documentField()) {
                    metadata.put(definition.columnName().toUpperCase(Locale.ROOT), definition);
                }
            }

            for (DocumentType type : DOCUMENT_TYPES) {
                EmployeeFieldDefinition definition = metadata.get(type.employeeFieldName().toUpperCase(Locale.ROOT));
                if (definition == null) {
                    types.add(type);
                } else {
                    types.add(new DocumentType(
                            definition.label(),
                            type.employeeFieldName(),
                            type.storageName(),
                            aliasesWithOriginalLabel(type)
                    ));
                }
            }

            for (EmployeeFieldDefinition definition : metadata.values()) {
                if (hasStaticDocumentColumn(definition.columnName())) {
                    continue;
                }
                types.add(new DocumentType(
                        definition.label(),
                        definition.columnName(),
                        definition.columnName() + ".jpg"
                ));
            }

            if (isEmployeeImageUploadTarget(profileImageDefinition)) {
                types.add(new DocumentType(
                        profileImageDefinition.label(),
                        "EMP_IMG",
                        "EMP_IMG.jpg",
                        "Employee Image",
                        "Employee Photo",
                        "Employee_Photo",
                        "Profile Image"
                ));
            }
        } catch (RuntimeException exception) {
            return DOCUMENT_TYPES;
        }
        return List.copyOf(types);
    }

    private static boolean isEmployeeImageUploadTarget(EmployeeFieldDefinition definition) {
        return definition != null
                && isProfileImageColumn(definition.columnName());
    }

    private static List<String> aliasesWithOriginalLabel(DocumentType type) {
        List<String> aliases = new ArrayList<>();
        aliases.add(type.label());
        aliases.addAll(type.aliases());
        return aliases;
    }

    public static int documentCount() {
        return documentTypes().size();
    }

    public static DocumentType documentType(int index) {
        return documentTypes().get(index);
    }

    public static String documentPath(Employee employee, int index) {
        if (employee == null) {
            return null;
        }
        try {
            Field field = Employee.class.getDeclaredField(documentType(index).employeeFieldName());
            field.setAccessible(true);
            Object value = field.get(employee);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException exception) {
            return employee.getDynamicField(documentType(index).employeeFieldName());
        }
    }

    public static void setDocumentPath(Employee employee, int index, String path) {
        if (employee == null) {
            return;
        }
        try {
            Field field = Employee.class.getDeclaredField(documentType(index).employeeFieldName());
            field.setAccessible(true);
            field.set(employee, path);
        } catch (ReflectiveOperationException exception) {
            employee.setDynamicField(documentType(index).employeeFieldName(), path);
        }
    }

    private static boolean hasStaticDocumentColumn(String columnName) {
        for (DocumentType documentType : DOCUMENT_TYPES) {
            if (documentType.employeeFieldName().equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProfileImageColumn(String columnName) {
        return "EMP_IMG".equalsIgnoreCase(columnName);
    }

    public static boolean isProfileImageDocument(int documentIndex) {
        return documentIndex >= 0
                && documentIndex < documentCount()
                && isProfileImageColumn(documentType(documentIndex).employeeFieldName());
    }

    public static boolean hasStoredPath(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String trimmed = value.trim();
        return !trimmed.equalsIgnoreCase("N/A")
                && !trimmed.equalsIgnoreCase("NA")
                && !trimmed.equalsIgnoreCase("NULL")
                && !trimmed.equalsIgnoreCase("EMPTY")
                && !trimmed.equals("-");
    }

    public static File resolveStoredFile(String path) {
        return EmployeeStorageUtil.resolveStoredFile(path);
    }

    public static String fileNameFromPath(String path) {
        if (!hasStoredPath(path)) {
            return "-";
        }
        return new File(path).getName();
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        return (bytes / (1024 * 1024)) + " MB";
    }

    @Deprecated
    public static String validateImageFile(File file) {
        String typeValidation = validateUploadImageType(file);
        if (typeValidation != null) {
            return typeValidation;
        }
        try {
            if (!isReadableJpegFile(file)) {
                return "Please select a valid JPG or JPEG image.";
            }
            return null;
        } catch (IOException exception) {
            return "Please select a valid JPG or JPEG image.";
        }
    }

    public static PreparedUploadFile prepareImageForUpload(File file) {
        return prepareImageForUpload(file, true);
    }

    public static PreparedUploadFile prepareImageForUpload(File file, boolean compressWhenNeeded) {
        String validationMessage = validateUploadImageType(file);
        if (validationMessage != null) {
            return PreparedUploadFile.rejected(file, validationMessage);
        }
        long maxBytes = maxUploadSizeBytes();
        if (file.length() <= maxBytes || !compressWhenNeeded) {
            try {
                if (!isReadableJpegFile(file)) {
                    return PreparedUploadFile.rejected(file, "Please select a valid JPG or JPEG image.");
                }
            } catch (IOException exception) {
                return PreparedUploadFile.rejected(file, "Please select a valid JPG or JPEG image.");
            }
            return PreparedUploadFile.ready(file, file, false, null);
        }

        try {
            File compressed = compressJpegWithinLimit(file, maxBytes);
            if (compressed == null) {
                return PreparedUploadFile.rejected(file, compressionLimitMessage());
            }
            return PreparedUploadFile.ready(
                    file,
                    compressed,
                    true,
                    "Compressed " + file.getName() + " from " + formatSize(file.length())
                            + " to " + formatSize(compressed.length()) + "."
            );
        } catch (IOException | RuntimeException exception) {
            return PreparedUploadFile.rejected(
                    file,
                    uploadPreparationFailureMessage(exception)
            );
        }
    }

    public static String validateUploadImageType(File file) {
        if (file == null || !file.isFile()) {
            return "This item is not a valid file.";
        }
        if (!isJpegFile(file)) {
            return "Unsupported file type. Please upload a JPG or JPEG image.";
        }
        return null;
    }

    private static String compressionLimitMessage() {
        return "This JPG/JPEG image could not be compressed under the configured upload limit.";
    }

    private static String uploadPreparationFailureMessage(Exception exception) {
        String message = exception == null || exception.getMessage() == null
                ? ""
                : exception.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("valid jpg") || message.contains("valid jpeg")) {
            return "Please select a valid JPG or JPEG image.";
        }
        if (message.contains("unsupported file type")) {
            return "Unsupported file type. Please upload a JPG or JPEG image.";
        }
        return compressionLimitMessage();
    }

    private static boolean isJpegFile(File file) {
        String name = file == null || file.getName() == null ? "" : file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".jpe")
                || name.endsWith(".jfif");
    }

    private static boolean isReadableJpegFile(File file) throws IOException {
        return readJpegImageInfo(file) != null;
    }

    private static boolean hasJpegSignature(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() < 2) {
            return false;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            int first = input.read();
            int second = input.read();
            return first == 0xFF && second == 0xD8;
        }
    }

    private static File compressJpegWithinLimit(File source, long maxBytes) throws IOException {
        BufferedImage image = readJpegImage(source);
        JpegImageInfo originalInfo = new JpegImageInfo(image.getWidth(), image.getHeight());

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer is available.");
        }

        String suffix = source.getName().toLowerCase(Locale.ROOT).endsWith(".jpeg") ? ".jpeg" : ".jpg";
        Path temp = Files.createTempFile(UPLOAD_TEMP_PREFIX, suffix);
        boolean keepTemp = false;

        ImageWriter writer = writers.next();
        try {
            writeJpeg(image, temp.toFile(), writer, MAX_JPEG_QUALITY);
            if (temp.toFile().length() <= maxBytes) {
                keepTemp = true;
            } else {
                writeJpeg(image, temp.toFile(), writer, MIN_JPEG_QUALITY);
                if (temp.toFile().length() > maxBytes) {
                    return null;
                }

                float low = MIN_JPEG_QUALITY;
                float high = MAX_JPEG_QUALITY;
                float bestQuality = MIN_JPEG_QUALITY;
                boolean tempContainsBest = true;
                while (high - low > QUALITY_PRECISION) {
                    float quality = (low + high) / 2.0f;
                    writeJpeg(image, temp.toFile(), writer, quality);
                    if (temp.toFile().length() <= maxBytes) {
                        bestQuality = quality;
                        tempContainsBest = true;
                        low = quality;
                    } else {
                        tempContainsBest = false;
                        high = quality;
                    }
                }
                if (!tempContainsBest) {
                    writeJpeg(image, temp.toFile(), writer, bestQuality);
                }
                if (temp.toFile().length() > maxBytes) {
                    return null;
                }
                keepTemp = true;
            }
        } finally {
            writer.dispose();
            if (!keepTemp) {
                Files.deleteIfExists(temp);
            }
        }

        File compressed = temp.toFile();
        JpegImageInfo compressedInfo = readJpegImageInfo(compressed);
        if (compressedInfo == null
                || compressedInfo.width() != originalInfo.width()
                || compressedInfo.height() != originalInfo.height()) {
            Files.deleteIfExists(temp);
            throw new IOException("Compressed image dimensions changed.");
        }
        compressed.deleteOnExit();
        return compressed;
    }

    private static void writeJpeg(BufferedImage image, File target, ImageWriter writer, float quality) throws IOException {
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(Math.max(MIN_JPEG_QUALITY, Math.min(MAX_JPEG_QUALITY, quality)));
        Files.deleteIfExists(target.toPath());
        try (ImageOutputStream output = ImageIO.createImageOutputStream(target)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        }
    }

    public static BufferedImage readJpegImage(File file) throws IOException {
        String typeValidation = validateUploadImageType(file);
        if (typeValidation != null) {
            throw new IOException(typeValidation);
        }

        BufferedImage image = readJpegImageLenient(file);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IOException("Please select a valid JPG or JPEG image.");
        }
        return rgbImage(image);
    }

    private static BufferedImage readJpegImageLenient(File file) throws IOException {
        if (file == null || !file.isFile()) {
            return null;
        }
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException | RuntimeException ignored) {
        }
        return image == null ? readJpegImageWithIconFallback(file) : image;
    }

    private static JpegImageInfo readJpegImageInfo(File file) throws IOException {
        if (!hasJpegSignature(file)) {
            return null;
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) {
                return readJpegImageInfoWithIconFallback(file);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width > 0 && height > 0) {
                        return new JpegImageInfo(width, height);
                    }
                } catch (IOException | RuntimeException ignored) {
                } finally {
                    reader.dispose();
                }
                input.seek(0L);
            }
        }
        return readJpegImageInfoWithIconFallback(file);
    }

    private static JpegImageInfo readJpegImageInfoWithIconFallback(File file) {
        ImageIcon icon;
        try {
            icon = new ImageIcon(Files.readAllBytes(file.toPath()));
        } catch (IOException exception) {
            return null;
        }
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        return new JpegImageInfo(icon.getIconWidth(), icon.getIconHeight());
    }

    private static BufferedImage readJpegImageWithIconFallback(File file) {
        ImageIcon icon;
        try {
            icon = new ImageIcon(Files.readAllBytes(file.toPath()));
        } catch (IOException exception) {
            return null;
        }
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.drawImage(icon.getImage(), 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static BufferedImage rgbImage(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    public static long maxUploadSizeBytes() {
        return AppConfig.documentUploadMaxBytes();
    }

    public static String maxUploadSizeLabel() {
        return formatSize(maxUploadSizeBytes());
    }

    public static boolean shouldCompressBeforeUpload(File file) {
        return file != null && file.isFile() && isJpegFile(file) && file.length() > maxUploadSizeBytes();
    }

    public static boolean shouldCompressBeforeUpload(File file, boolean compressionEnabled) {
        return compressionEnabled
                && file != null
                && file.isFile()
                && isJpegFile(file)
                && file.length() > maxUploadSizeBytes();
    }

    public static boolean isTemporaryUploadFile(File file) {
        if (file == null || file.getName() == null || !file.getName().startsWith(UPLOAD_TEMP_PREFIX)) {
            return false;
        }
        try {
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
            Path path = file.toPath().toAbsolutePath().normalize();
            return path.getParent() != null && path.getParent().equals(tempDir);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static void deleteTemporaryUpload(File file) {
        if (!isTemporaryUploadFile(file)) {
            return;
        }
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }

    public static String copyProfileImageToEmployeeStorage(String employeeCode, File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("Prepared upload file is missing.");
        }
        Path destination = EmployeeStorageUtil.ensureEmployeeDirectory(employeeCode).resolve("EMP_IMG.jpg");
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return EmployeeStorageUtil.profileImagePath(employeeCode);
    }

    public static String copyDocumentToEmployeeStorage(String employeeCode, int documentIndex, File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("Prepared upload file is missing.");
        }
        String storageName = documentType(documentIndex).storageName();
        if (isProfileImageDocument(documentIndex)) {
            Path destination = EmployeeStorageUtil.ensureEmployeeDirectory(employeeCode).resolve(storageName);
            Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return EmployeeStorageUtil.profileImagePath(employeeCode);
        }

        Path destination = EmployeeStorageUtil.ensureDocumentDirectory(employeeCode).resolve(storageName);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return EmployeeStorageUtil.documentPath(employeeCode, storageName);
    }

    @FunctionalInterface
    public interface UploadPreparationListener {
        void onProgress(String message, int completedFiles, int totalFiles, int percent);
    }

    public static BulkUploadResult matchBulkFiles(File[] selectedFiles, boolean[] lockedDocuments) {
        return matchBulkFiles(selectedFiles, lockedDocuments, null);
    }

    public static BulkUploadResult matchBulkFiles(
            File[] selectedFiles,
            boolean[] lockedDocuments,
            UploadPreparationListener listener
    ) {
        BulkUploadResult result = new BulkUploadResult();
        Set<Integer> matchedThisBatch = new HashSet<>();

        if (selectedFiles == null || selectedFiles.length == 0) {
            return result;
        }

        for (int index = 0; index < selectedFiles.length; index++) {
            File file = selectedFiles[index];
            notifyPreparation(listener, "Checking " + fileName(file) + "...", index, selectedFiles.length);
            String typeValidation = validateUploadImageType(file);
            if (typeValidation != null) {
                result.discard(fileName(file) + " - " + typeValidation);
                continue;
            }

            DocumentMatch match = matchDocumentForFile(file);
            if (!match.matched()) {
                result.discard(file.getName() + " - No document label matched this file name.");
                continue;
            }
            if (match.ambiguous()) {
                result.discard(file.getName() + " - The name matches more than one document. Rename it to the exact document label and try again.");
                continue;
            }
            if (isLocked(lockedDocuments, match.documentIndex())) {
                result.discard(file.getName() + " - Since " + cleanDocumentLabel(match.documentIndex()) + " already exists in DB, it cannot be replaced.");
                continue;
            }
            if (matchedThisBatch.contains(match.documentIndex())) {
                result.discard(file.getName() + " - Another selected file already matched " + cleanDocumentLabel(match.documentIndex()) + ".");
                continue;
            }

            int documentIndex = match.documentIndex();
            if (shouldCompressBeforeUpload(file)) {
                notifyPreparation(listener, "Compressing " + cleanDocumentLabel(documentIndex) + "...", index, selectedFiles.length);
            }
            PreparedUploadFile prepared = prepareImageForUpload(file);
            if (!prepared.ready()) {
                result.discard(fileName(file) + " - " + prepared.message());
                continue;
            }

            matchedThisBatch.add(documentIndex);
            result.uploaded(new BulkUploadItem(
                    documentIndex,
                    prepared.file(),
                    cleanDocumentLabel(documentIndex),
                    prepared.originalFile(),
                    prepared.compressed()
            ));
            notifyPreparation(listener, "Prepared " + cleanDocumentLabel(documentIndex) + ".", index + 1, selectedFiles.length);
        }

        notifyPreparation(listener, "Finished preparing selected files.", selectedFiles.length, selectedFiles.length);
        return result;
    }

    private static void notifyPreparation(
            UploadPreparationListener listener,
            String message,
            int completedFiles,
            int totalFiles
    ) {
        if (listener == null) {
            return;
        }
        int percent = totalFiles <= 0
                ? 100
                : Math.min(100, Math.max(0, (int) Math.round(completedFiles * 100.0 / totalFiles)));
        listener.onProgress(message, completedFiles, totalFiles, percent);
    }

    public static DocumentMatch matchDocumentForFile(File file) {
        String fileName = normalizeForMatch(stripExtension(file.getName()));
        int bestScore = Integer.MAX_VALUE;
        int bestIndex = -1;
        boolean ambiguous = false;

        List<DocumentType> types = documentTypes();
        for (int index = 0; index < types.size(); index++) {
            int score = documentFileMatchScore(fileName, index);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
                ambiguous = false;
            } else if (score == bestScore && score != Integer.MAX_VALUE) {
                ambiguous = true;
            }
        }

        if (bestIndex < 0 || bestScore == Integer.MAX_VALUE) {
            return DocumentMatch.none();
        }
        return new DocumentMatch(bestIndex, ambiguous);
    }

    public static String cleanDocumentLabel(int documentIndex) {
        return documentType(documentIndex).label().replace("*", "").trim();
    }

    public static String normalizedSearch(String value) {
        return value == null
                ? ""
                : value.replace("*", "").trim().toLowerCase();
    }

    private static int documentFileMatchScore(String fileName, int documentIndex) {
        int best = Integer.MAX_VALUE;
        for (String alias : documentAliases(documentIndex)) {
            if (alias.isEmpty()) {
                continue;
            }
            if (fileName.equals(alias)) {
                return 0;
            }
            if (fileName.startsWith(alias + " ") || fileName.endsWith(" " + alias)
                    || alias.startsWith(fileName + " ") || alias.endsWith(" " + fileName)) {
                best = Math.min(best, 1);
            }
            if (allTokensPresent(fileName, alias) || allTokensPresent(alias, fileName)) {
                best = Math.min(best, 2);
            }
        }
        return best;
    }

    private static List<String> documentAliases(int documentIndex) {
        DocumentType type = documentType(documentIndex);
        List<String> aliases = new ArrayList<>();
        aliases.add(normalizeForMatch(type.label()));
        aliases.add(normalizeForMatch(type.employeeFieldName()));
        aliases.add(normalizeForMatch(stripExtension(type.storageName())));
        for (String alias : type.aliases()) {
            aliases.add(normalizeForMatch(alias));
        }
        return aliases;
    }

    private static boolean allTokensPresent(String value, String requiredTokens) {
        String[] tokens = requiredTokens.split(" ");
        boolean foundAnyToken = false;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            foundAnyToken = true;
            if (!value.contains(token)) {
                return false;
            }
        }
        return foundAnyToken;
    }

    private static boolean isLocked(boolean[] lockedDocuments, int documentIndex) {
        return lockedDocuments != null
                && documentIndex >= 0
                && documentIndex < lockedDocuments.length
                && lockedDocuments[documentIndex];
    }

    private static String fileName(File file) {
        return file == null ? "Selected item" : file.getName();
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String normalizeForMatch(String value) {
        return value == null
                ? ""
                : value.replace("*", "")
                        .replaceAll("[^A-Za-z0-9]+", " ")
                        .trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase();
    }

    public record DocumentType(String label, String employeeFieldName, String storageName, List<String> aliases) {
        public DocumentType(String label, String employeeFieldName, String storageName, String... aliases) {
            this(label, employeeFieldName, storageName, aliases == null ? List.of() : List.of(aliases));
        }
    }

    public record PreparedUploadFile(File originalFile, File file, boolean ready, boolean compressed, String message) {
        private static PreparedUploadFile ready(File originalFile, File file, boolean compressed, String message) {
            return new PreparedUploadFile(originalFile, file, true, compressed, message);
        }

        private static PreparedUploadFile rejected(File originalFile, String message) {
            return new PreparedUploadFile(originalFile, null, false, false, message);
        }
    }

    private record JpegImageInfo(int width, int height) {
    }

    public record BulkUploadItem(
            int documentIndex,
            File file,
            String documentLabel,
            File originalFile,
            boolean compressed
    ) {
    }

    public static final class DocumentMatch {
        private final int documentIndex;
        private final boolean ambiguous;

        private DocumentMatch(int documentIndex, boolean ambiguous) {
            this.documentIndex = documentIndex;
            this.ambiguous = ambiguous;
        }

        private static DocumentMatch none() {
            return new DocumentMatch(-1, false);
        }

        public boolean matched() {
            return documentIndex >= 0;
        }

        public boolean ambiguous() {
            return ambiguous;
        }

        public int documentIndex() {
            return documentIndex;
        }
    }

    public static final class BulkUploadResult {
        private final List<BulkUploadItem> uploadedDocuments = new ArrayList<>();
        private final List<String> discardedFiles = new ArrayList<>();

        private void uploaded(BulkUploadItem item) {
            uploadedDocuments.add(item);
        }

        private void discard(String reason) {
            discardedFiles.add(reason);
        }

        public List<BulkUploadItem> uploadedDocuments() {
            return uploadedDocuments;
        }

        public int uploadedCount() {
            return uploadedDocuments.size();
        }

        public int discardedCount() {
            return discardedFiles.size();
        }

        public List<String> discardedFiles() {
            return discardedFiles;
        }

        public String discardedDetails() {
            if (discardedFiles.isEmpty()) {
                return "-";
            }

            StringBuilder details = new StringBuilder();
            for (String file : discardedFiles) {
                if (details.length() > 0) {
                    details.append('\n');
                }
                details.append("- ").append(file);
            }
            return details.toString();
        }
    }
}
