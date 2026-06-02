package com.kgm.util;

import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;

import com.kgm.config.AppConfig;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EmployeeDocumentUtil {
    private static final float MIN_JPEG_QUALITY = 0.55f;
    private static final float MAX_JPEG_QUALITY = 0.92f;
    private static final float QUALITY_STEP = 0.05f;
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

    public static String validateImageFile(File file) {
        if (file == null || !file.isFile()) {
            return "This item is not a valid file.";
        }

        if (!isJpegFile(file)) {
            return "Unsupported file type. Please upload a JPG or JPEG image.";
        }

        if (file.length() > maxUploadSizeBytes()) {
            return "File size is " + formatSize(file.length()) + "; the limit is " + maxUploadSizeLabel() + ".";
        }

        return null;
    }

    public static PreparedUploadFile prepareImageForUpload(File file) {
        String validationMessage = validateImageType(file);
        if (validationMessage != null) {
            return PreparedUploadFile.rejected(file, validationMessage);
        }
        if (file.length() <= maxUploadSizeBytes()) {
            return PreparedUploadFile.ready(file, file, false, null);
        }

        try {
            File compressed = compressJpegWithinLimit(file, maxUploadSizeBytes());
            if (compressed == null) {
                return PreparedUploadFile.rejected(file, sizeLimitMessage(file.length()));
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
                    "This JPG/JPEG image is " + formatSize(file.length())
                            + ", above the " + maxUploadSizeLabel()
                            + " limit, and could not be compressed safely."
            );
        }
    }

    private static String validateImageType(File file) {
        if (file == null || !file.isFile()) {
            return "This item is not a valid file.";
        }
        if (!isJpegFile(file)) {
            return "Unsupported file type. Please upload a JPG or JPEG image.";
        }
        return null;
    }

    private static String sizeLimitMessage(long bytes) {
        return "File size is " + formatSize(bytes) + "; the limit is " + maxUploadSizeLabel() + ".";
    }

    private static boolean isJpegFile(File file) {
        String name = file == null || file.getName() == null ? "" : file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static File compressJpegWithinLimit(File source, long maxBytes) throws IOException {
        BufferedImage image = ImageIO.read(source);
        if (image == null) {
            throw new IOException("Selected file is not a readable JPG/JPEG image.");
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer is available.");
        }

        String suffix = source.getName().toLowerCase(Locale.ROOT).endsWith(".jpeg") ? ".jpeg" : ".jpg";
        Path temp = Files.createTempFile("kgm-upload-", suffix);
        File best = null;

        ImageWriter writer = writers.next();
        try {
            for (float quality = MAX_JPEG_QUALITY; quality >= MIN_JPEG_QUALITY; quality -= QUALITY_STEP) {
                writeJpeg(image, temp.toFile(), writer, quality);
                File candidate = temp.toFile();
                if (candidate.length() <= maxBytes) {
                    best = candidate;
                    break;
                }
            }
        } finally {
            writer.dispose();
        }

        if (best == null) {
            Files.deleteIfExists(temp);
            return null;
        }
        best.deleteOnExit();
        return best;
    }

    private static void writeJpeg(BufferedImage image, File target, ImageWriter writer, float quality) throws IOException {
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(Math.max(MIN_JPEG_QUALITY, Math.min(MAX_JPEG_QUALITY, quality)));
        try (ImageOutputStream output = ImageIO.createImageOutputStream(target)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        }
    }

    public static long maxUploadSizeBytes() {
        return AppConfig.documentUploadMaxBytes();
    }

    public static String maxUploadSizeLabel() {
        return formatSize(maxUploadSizeBytes());
    }

    public static BulkUploadResult matchBulkFiles(File[] selectedFiles, boolean[] lockedDocuments) {
        BulkUploadResult result = new BulkUploadResult();
        Set<Integer> matchedThisBatch = new HashSet<>();

        if (selectedFiles == null || selectedFiles.length == 0) {
            return result;
        }

        for (File file : selectedFiles) {
            PreparedUploadFile prepared = prepareImageForUpload(file);
            if (!prepared.ready()) {
                result.discard(fileName(file) + " - " + prepared.message());
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
            matchedThisBatch.add(documentIndex);
            result.uploaded(new BulkUploadItem(
                    documentIndex,
                    prepared.file(),
                    cleanDocumentLabel(documentIndex),
                    prepared.originalFile(),
                    prepared.compressed()
            ));
        }

        return result;
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
