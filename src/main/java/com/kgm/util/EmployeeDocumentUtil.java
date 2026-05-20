package com.kgm.util;

import com.kgm.model.Employee;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EmployeeDocumentUtil {
    public static final long MAX_SIZE = 400 * 1024;

    private static final List<DocumentType> DOCUMENT_TYPES = List.of(
            new DocumentType("CNIC *", "CNIC_COPY", "CNIC_COPY.jpg"),
            new DocumentType("EOBI Card *", "EOBI_CARD_COPY", "EOBI_CARD_COPY.jpg"),
            new DocumentType("SS_CARD_COPY*", "SS_CARD_COPY", "SS_CARD_COPY.jpg"),
            new DocumentType("Final Settlement", "FINAL_SETTLEMENT", "FINAL_SETTLEMENT.jpg"),
            new DocumentType("Clearance Certificate", "CLEARANCE_CERT", "CLEARANCE_CERT.jpg"),
            new DocumentType("Job Appointment Letter", "JOB_APPOINTMENT", "JOB_APPOINTMENT.jpg"),
            new DocumentType("Application Letter", "APPLICATION_DOC", "APPLICATION_DOC.jpg"),
            new DocumentType("Issuance Form", "ISSUANCE_DOC", "ISSUANCE_DOC.jpg", "Insurance Form"),
            new DocumentType("Settlement Document", "SETTLEMENT_DOC", "SETTLEMENT_DOC.jpg"),
            new DocumentType("Trial Card", "TRIAL_CARD", "TRIAL_CARD.jpg"),
            new DocumentType("Interview Form", "INTERVIEW_DOC", "INTERVIEW_DOC.jpg"),
            new DocumentType("Service Letter", "SERVICE_LETTER", "SERVICE_LETTER.jpg"),
            new DocumentType("Extension Letter", "EXTENSION_LETTER", "EXTENSION_LETTER.jpg"),
            new DocumentType("Retirement Letter", "RETIREMENT_LETTER", "RETIREMENT_LETTER.jpg"),
            new DocumentType("Covid Certification", "COVID_CERT", "COVID_CERT.jpg", "Covid Certificate"),
            new DocumentType("DISCIPLINARY_I", "DISCIPLINARY_I", "DISCIPLINARY_I.jpg"),
            new DocumentType("DISCIPLINARY_II", "DISCIPLINARY_II", "DISCIPLINARY_II.jpg"),
            new DocumentType("DISCIPLINARY_III", "DISCIPLINARY_III", "DISCIPLINARY_III.jpg")
    );

    private EmployeeDocumentUtil() {
    }

    public static List<DocumentType> documentTypes() {
        return DOCUMENT_TYPES;
    }

    public static int documentCount() {
        return DOCUMENT_TYPES.size();
    }

    public static DocumentType documentType(int index) {
        return DOCUMENT_TYPES.get(index);
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
            return null;
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
            throw new IllegalArgumentException(
                    "Document field is not available: " + documentType(index).employeeFieldName(),
                    exception
            );
        }
    }

    public static boolean hasStoredPath(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String trimmed = value.trim();
        return !trimmed.equalsIgnoreCase("N/A")
                && !trimmed.equalsIgnoreCase("NA")
                && !trimmed.equalsIgnoreCase("NULL")
                && !trimmed.equals("-");
    }

    public static File resolveStoredFile(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(System.getProperty("user.dir"), path);
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

        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
            return "Only JPG or JPEG files can be uploaded.";
        }

        if (file.length() > MAX_SIZE) {
            return "File size is " + formatSize(file.length()) + "; the limit is 400 KB.";
        }

        return null;
    }

    public static BulkUploadResult matchBulkFiles(File[] selectedFiles, boolean[] lockedDocuments) {
        BulkUploadResult result = new BulkUploadResult();
        Set<Integer> matchedThisBatch = new HashSet<>();

        if (selectedFiles == null || selectedFiles.length == 0) {
            return result;
        }

        for (File file : selectedFiles) {
            String validationMessage = validateImageFile(file);
            if (validationMessage != null) {
                result.discard(fileName(file) + " - " + validationMessage);
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
            result.uploaded(new BulkUploadItem(documentIndex, file, cleanDocumentLabel(documentIndex)));
        }

        return result;
    }

    public static DocumentMatch matchDocumentForFile(File file) {
        String fileName = normalizeForMatch(stripExtension(file.getName()));
        int bestScore = Integer.MAX_VALUE;
        int bestIndex = -1;
        boolean ambiguous = false;

        for (int index = 0; index < DOCUMENT_TYPES.size(); index++) {
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

    public record BulkUploadItem(int documentIndex, File file, String documentLabel) {
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
