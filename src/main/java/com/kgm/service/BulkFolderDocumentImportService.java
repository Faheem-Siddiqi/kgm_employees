package com.kgm.service;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.Employee;
import com.kgm.util.EmployeeDocumentUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BulkFolderDocumentImportService {
    public static final int MAX_FOLDERS = 50;
    private static final Pattern DIGIT_SEQUENCE = Pattern.compile("\\d{1,18}");
    private static final Pattern LABELED_DIGIT_SEQUENCE = Pattern.compile(
            "(?i)(?:employee|emp|id|code)\\D{0,12}(\\d{1,18})"
    );
    private static final Pattern LEADING_DIGIT_SEQUENCE = Pattern.compile("^\\s*(\\d{1,18})");

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(String message, int completedFolders, int totalFolders, int percent);
    }

    public ImportResult importFolders(File[] selectedFolders, ProgressListener progressListener) throws IOException {
        List<File> folders = validFolders(selectedFolders);
        if (folders.size() > MAX_FOLDERS) {
            throw new IllegalArgumentException("Maximum " + MAX_FOLDERS + " employee folders can be uploaded at once.");
        }

        ImportSummary summary = new ImportSummary();
        int total = folders.size();
        report(progressListener, "Preparing folder upload...", 0, total);

        try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
            Set<String> processedEmployeeFolders = new HashSet<>();
            for (int index = 0; index < folders.size(); index++) {
                File folder = folders.get(index);
                reportFolderStep(progressListener, "Scanning " + folder.getName() + "...", index, total, 0, 4);
                processFolder(folder, dao, summary, progressListener, index, total, processedEmployeeFolders);
                reportFolderStep(progressListener, "Processed " + folder.getName() + ".", index, total, 4, 4);
            }
        }

        report(progressListener, "Finalizing upload summary...", total, total);
        return summary.toResult();
    }

    private void processFolder(
            File folder,
            EmployeeRecordDao dao,
            ImportSummary summary,
            ProgressListener progressListener,
            int folderIndex,
            int totalFolders,
            Set<String> processedEmployeeFolders
    ) {
        String folderKey = folder.getName() == null ? "" : folder.getName().trim();
        if (folderKey.isEmpty()) {
            summary.error(folder, "No employee record matched this folder name.");
            return;
        }

        reportFolderStep(progressListener, "Finding employee for folder " + folderKey + "...", folderIndex, totalFolders, 1, 4);
        EmployeeFolder employeeFolder = findEmployeeFolder(folder, dao);
        if (employeeFolder == null) {
            summary.error(folder, "No employee record matched this folder name.");
            return;
        }
        folder = employeeFolder.folder();
        if (!processedEmployeeFolders.add(folder.getAbsolutePath())) {
            return;
        }
        folderKey = folder.getName() == null ? "" : folder.getName().trim();
        Employee employee = employeeFolder.employee();
        String employeeCode = employee.getEMPLOYEE_CODE();
        if (employeeCode == null || employeeCode.isBlank()) {
            summary.error(folder, "Employee record has no employee code.");
            return;
        }
        summary.employee(employeeCode, employee.getEMP_NAME(), folder);

        reportFolderStep(progressListener, "Reading files directly inside " + folderKey + "...", folderIndex, totalFolders, 2, 4);
        List<File> files = documentFiles(folder, summary, folderKey);
        if (files.isEmpty()) {
            summary.failed(employeeCode, "No document files found directly inside folder", folderKey);
            return;
        }

        Set<Integer> matchedThisFolder = new HashSet<>();
        Employee update = new Employee();
        update.setEMPLOYEE_CODE(employeeCode);
        int uploadedForEmployee = 0;

        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            File file = files.get(fileIndex);
            reportFileStep(
                    progressListener,
                    "Checking " + employeeCode + " / " + file.getName() + "...",
                    folderIndex,
                    totalFolders,
                    fileIndex,
                    files.size()
            );
            String typeValidation = EmployeeDocumentUtil.validateUploadImageType(file);
            if (typeValidation != null) {
                summary.failed(employeeCode, failureReason(typeValidation), file.getName());
                continue;
            }

            EmployeeDocumentUtil.DocumentMatch match = EmployeeDocumentUtil.matchDocumentForFile(file);
            if (!match.matched()) {
                summary.noMatch(employeeCode, file.getName());
                continue;
            }
            if (match.ambiguous()) {
                summary.failed(employeeCode, "Document label matches more than one field", file.getName());
                continue;
            }
            int documentIndex = match.documentIndex();
            String documentLabel = EmployeeDocumentUtil.cleanDocumentLabel(documentIndex);
            if (EmployeeDocumentUtil.hasStoredPath(EmployeeDocumentUtil.documentPath(employee, documentIndex))) {
                summary.alreadyExists(employeeCode, documentLabel);
                continue;
            }
            if (matchedThisFolder.contains(documentIndex)) {
                summary.failed(employeeCode, "Duplicate document label in folder", documentLabel);
                continue;
            }
            if (EmployeeDocumentUtil.shouldCompressBeforeUpload(file)) {
                reportFileStep(
                        progressListener,
                        "Compressing " + employeeCode + " / " + documentLabel + "...",
                        folderIndex,
                        totalFolders,
                        fileIndex,
                        files.size()
                );
            }
            EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(file);
            if (!prepared.ready()) {
                summary.failed(employeeCode, failureReason(prepared.message()), file.getName());
                continue;
            }

            try {
                reportFileStep(
                        progressListener,
                        "Uploading " + employeeCode + " / " + documentLabel + "...",
                        folderIndex,
                        totalFolders,
                        fileIndex,
                        files.size()
                );
                String dbPath = EmployeeDocumentUtil.copyDocumentToEmployeeStorage(employeeCode, documentIndex, prepared.file());
                EmployeeDocumentUtil.setDocumentPath(update, documentIndex, dbPath);
                matchedThisFolder.add(documentIndex);
                uploadedForEmployee++;
                summary.uploadedDocument(employeeCode, documentLabel);
            } catch (IOException exception) {
                summary.failed(employeeCode, failureReason("Upload failed: " + exception.getMessage()), file.getName());
            } finally {
                if (prepared.compressed()) {
                    EmployeeDocumentUtil.deleteTemporaryUpload(prepared.file());
                }
            }
        }

        if (uploadedForEmployee == 0) {
            return;
        }

        try {
            reportFolderStep(progressListener, "Saving database updates for " + employeeCode + "...", folderIndex, totalFolders, 3, 4);
            dao.updateEmployeeDynamic(update);
        } catch (Exception exception) {
            summary.rollbackUploadedForEmployee(
                    employeeCode,
                    failureReason("Database update failed after copying files: " + exception.getMessage())
            );
        }
    }

    private String failureReason(String message) {
        if (message == null || message.isBlank()) {
            return "Failed";
        }
        String clean = message.trim();
        String lower = clean.toLowerCase();
        if (lower.contains("compress") || lower.contains("size")) {
            return "Failed due to size/compression issue";
        }
        if (lower.contains("unsupported file type")) {
            return "Unsupported file type";
        }
        if (lower.contains("valid jpg") || lower.contains("valid jpeg") || lower.contains("valid file")) {
            return "Invalid file";
        }
        clean = clean.replaceAll("\\s+", " ");
        while (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean.isBlank() ? "Failed" : clean;
    }

    private EmployeeFolder findEmployeeFolder(File folder, EmployeeRecordDao dao) {
        File candidate = folder;
        Set<String> attemptedLookupNames = new HashSet<>();
        for (int depth = 0; candidate != null && depth < 5; depth++) {
            String name = candidate.getName() == null ? "" : candidate.getName().trim();
            for (String lookupName : employeeFolderLookupNames(name)) {
                if (!attemptedLookupNames.add(lookupName)) {
                    continue;
                }
                Employee employee = dao.getEmployeeDocumentsByCodeOrId(lookupName);
                if (employee != null) {
                    return new EmployeeFolder(candidate, employee);
                }
            }
            candidate = candidate.getParentFile();
        }
        return null;
    }

    static List<String> employeeFolderLookupNames(String folderName) {
        String cleanName = folderName == null ? "" : folderName.trim().replaceAll("\\s+", " ");
        if (cleanName.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> lookupNames = new LinkedHashSet<>();
        addLookupName(lookupNames, cleanName);
        addLabeledDigitLookupNames(lookupNames, cleanName);
        addPreferredDigitLookupNames(lookupNames, cleanName);
        addLeadingDigitLookupName(lookupNames, cleanName);
        addAllDigitLookupNames(lookupNames, cleanName);
        return List.copyOf(lookupNames);
    }

    private static void addLabeledDigitLookupNames(Set<String> lookupNames, String folderName) {
        Matcher matcher = LABELED_DIGIT_SEQUENCE.matcher(folderName);
        while (matcher.find()) {
            addLookupName(lookupNames, matcher.group(1));
        }
    }

    private static void addPreferredDigitLookupNames(Set<String> lookupNames, String folderName) {
        Matcher matcher = DIGIT_SEQUENCE.matcher(folderName);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.startsWith("0")) {
                addLookupName(lookupNames, token);
            }
        }

        matcher = DIGIT_SEQUENCE.matcher(folderName);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 4) {
                addLookupName(lookupNames, token);
            }
        }
    }

    private static void addLeadingDigitLookupName(Set<String> lookupNames, String folderName) {
        Matcher matcher = LEADING_DIGIT_SEQUENCE.matcher(folderName);
        if (matcher.find()) {
            addLookupName(lookupNames, matcher.group(1));
        }
    }

    private static void addAllDigitLookupNames(Set<String> lookupNames, String folderName) {
        Matcher matcher = DIGIT_SEQUENCE.matcher(folderName);
        while (matcher.find()) {
            addLookupName(lookupNames, matcher.group());
        }
    }

    private static void addLookupName(Set<String> lookupNames, String value) {
        if (value == null) {
            return;
        }
        String clean = value.trim();
        if (!clean.isEmpty()) {
            lookupNames.add(clean);
        }
    }

    private List<File> validFolders(File[] selectedFolders) {
        Map<String, File> folders = new LinkedHashMap<>();
        if (selectedFolders == null) {
            return new ArrayList<>();
        }
        for (File selected : selectedFolders) {
            File folder = selectedFolder(selected);
            if (folder != null && folder.isDirectory()) {
                folders.putIfAbsent(folder.getAbsolutePath(), folder);
            }
        }
        return new ArrayList<>(folders.values());
    }

    private File selectedFolder(File selected) {
        if (selected == null) {
            return null;
        }
        if (selected.isDirectory()) {
            return selected;
        }
        return selected.getParentFile();
    }

    private List<File> documentFiles(File folder, ImportSummary summary, String folderKey) {
        try (var stream = Files.list(folder.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(Path::toFile)
                    .toList();
        } catch (IOException exception) {
            summary.error(folder, "Folder could not be read: " + exception.getMessage());
            return List.of();
        }
    }

    private void report(ProgressListener progressListener, String message, int completed, int total) {
        if (progressListener == null) {
            return;
        }
        int percent = total <= 0 ? 100 : Math.min(100, Math.max(0, (int) Math.round(completed * 100.0 / total)));
        progressListener.onProgress(message, completed, total, percent);
    }

    private void reportFolderStep(
            ProgressListener progressListener,
            String message,
            int folderIndex,
            int totalFolders,
            int step,
            int totalSteps
    ) {
        if (progressListener == null) {
            return;
        }
        double folderProgress = totalFolders <= 0 ? 1.0 : folderIndex / (double) totalFolders;
        double stepProgress = totalSteps <= 0 || totalFolders <= 0 ? 0.0 : (step / (double) totalSteps) / totalFolders;
        int percent = Math.min(99, Math.max(0, (int) Math.round((folderProgress + stepProgress) * 100.0)));
        progressListener.onProgress(message, folderIndex, totalFolders, percent);
    }

    private void reportFileStep(
            ProgressListener progressListener,
            String message,
            int folderIndex,
            int totalFolders,
            int fileIndex,
            int totalFiles
    ) {
        if (progressListener == null) {
            return;
        }
        double fileProgress = totalFiles <= 0 ? 0.0 : fileIndex / (double) totalFiles;
        double folderStep = 2.0 + fileProgress;
        reportFolderStep(progressListener, message, folderIndex, totalFolders, (int) Math.round(folderStep * 100), 400);
    }

    public record ImportResult(
            List<UploadedEmployee> uploadedEmployees,
            List<FolderError> folderErrors,
            List<EmployeeUploadSummary> employeeSummaries
    ) {
        public int uploadedCount() {
            int count = 0;
            for (EmployeeUploadSummary employee : employeeSummaries) {
                count += employee.uploadedLabels().size();
            }
            return count;
        }

        public int skippedCount() {
            int count = 0;
            for (EmployeeUploadSummary employee : employeeSummaries) {
                count += employee.skippedCount();
            }
            for (FolderError folderError : folderErrors) {
                count += folderError.messages().size();
            }
            return count;
        }
    }

    public record UploadedEmployee(String employeeCode, List<String> labels) {
    }

    public record FolderError(String folderName, File folder, List<String> messages) {
    }

    public record EmployeeUploadSummary(
            String employeeCode,
            String employeeName,
            File folder,
            List<String> uploadedLabels,
            List<String> alreadyExistingLabels,
            List<String> noMatchFiles,
            Map<String, List<String>> failedByReason
    ) {
        public String displayName() {
            if (employeeName == null || employeeName.isBlank()) {
                return employeeCode;
            }
            return employeeCode + " - " + employeeName;
        }

        public int skippedCount() {
            int count = alreadyExistingLabels.size() + noMatchFiles.size();
            for (List<String> items : failedByReason.values()) {
                count += items.size();
            }
            return count;
        }

        public boolean hasDetails() {
            return !uploadedLabels.isEmpty()
                    || !alreadyExistingLabels.isEmpty()
                    || !noMatchFiles.isEmpty()
                    || !failedByReason.isEmpty();
        }
    }

    private record EmployeeFolder(File folder, Employee employee) {
    }

    private static final class ImportSummary {
        private final Map<String, EmployeeSummaryBuilder> employeesByCode = new LinkedHashMap<>();
        private final Map<String, FolderErrorBuilder> errorsByFolder = new LinkedHashMap<>();

        private void employee(String employeeCode, String employeeName, File folder) {
            employee(employeeCode).update(employeeName, folder);
        }

        private void uploadedDocument(String employeeCode, String label) {
            employee(employeeCode).uploaded(label);
        }

        private void alreadyExists(String employeeCode, String label) {
            employee(employeeCode).alreadyExists(label);
        }

        private void noMatch(String employeeCode, String fileName) {
            employee(employeeCode).noMatch(fileName);
        }

        private void failed(String employeeCode, String reason, String item) {
            employee(employeeCode).failed(reason, item);
        }

        private void error(File folder, String message) {
            String key = folder == null ? "" : folder.getAbsolutePath();
            String folderName = folder == null ? "Unknown folder" : folder.getName();
            errorsByFolder.computeIfAbsent(key, ignored -> new FolderErrorBuilder(folderName, folder)).messages.add(message);
        }

        private void rollbackUploadedForEmployee(String employeeCode, String reason) {
            employee(employeeCode).rollbackUploaded(reason);
        }

        private ImportResult toResult() {
            List<EmployeeUploadSummary> employeeSummaries = new ArrayList<>();
            List<UploadedEmployee> uploadedEmployees = new ArrayList<>();
            for (EmployeeSummaryBuilder builder : employeesByCode.values()) {
                EmployeeUploadSummary summary = builder.toSummary();
                if (!summary.hasDetails()) {
                    continue;
                }
                employeeSummaries.add(summary);
                if (!summary.uploadedLabels().isEmpty()) {
                    uploadedEmployees.add(new UploadedEmployee(summary.employeeCode(), summary.uploadedLabels()));
                }
            }

            List<FolderError> folderErrors = new ArrayList<>();
            for (FolderErrorBuilder builder : errorsByFolder.values()) {
                folderErrors.add(new FolderError(builder.folderName, builder.folder, List.copyOf(builder.messages)));
            }
            return new ImportResult(
                    List.copyOf(uploadedEmployees),
                    List.copyOf(folderErrors),
                    List.copyOf(employeeSummaries)
            );
        }

        private EmployeeSummaryBuilder employee(String employeeCode) {
            return employeesByCode.computeIfAbsent(employeeCode, EmployeeSummaryBuilder::new);
        }

        private static void addUnique(List<String> values, String value) {
            if (value == null || value.isBlank() || values.contains(value)) {
                return;
            }
            values.add(value);
        }

        private static final class EmployeeSummaryBuilder {
            private final String employeeCode;
            private String employeeName;
            private File folder;
            private final List<String> uploadedLabels = new ArrayList<>();
            private final List<String> alreadyExistingLabels = new ArrayList<>();
            private final List<String> noMatchFiles = new ArrayList<>();
            private final Map<String, List<String>> failedByReason = new LinkedHashMap<>();

            private EmployeeSummaryBuilder(String employeeCode) {
                this.employeeCode = employeeCode;
            }

            private void update(String employeeName, File folder) {
                if (employeeName != null && !employeeName.isBlank()) {
                    this.employeeName = employeeName.trim();
                }
                if (folder != null) {
                    this.folder = folder;
                }
            }

            private void uploaded(String label) {
                addUnique(uploadedLabels, label);
            }

            private void alreadyExists(String label) {
                addUnique(alreadyExistingLabels, label);
            }

            private void noMatch(String fileName) {
                addUnique(noMatchFiles, fileName);
            }

            private void failed(String reason, String item) {
                String cleanReason = reason == null || reason.isBlank() ? "Failed" : reason.trim();
                addUnique(failedByReason.computeIfAbsent(cleanReason, key -> new ArrayList<>()), item);
            }

            private void rollbackUploaded(String reason) {
                if (uploadedLabels.isEmpty()) {
                    return;
                }
                String cleanReason = reason == null || reason.isBlank() ? "Database update failed" : reason.trim();
                List<String> failedItems = failedByReason.computeIfAbsent(cleanReason, key -> new ArrayList<>());
                for (String label : uploadedLabels) {
                    addUnique(failedItems, label);
                }
                uploadedLabels.clear();
            }

            private EmployeeUploadSummary toSummary() {
                Map<String, List<String>> copiedFailures = new LinkedHashMap<>();
                for (Map.Entry<String, List<String>> entry : failedByReason.entrySet()) {
                    copiedFailures.put(entry.getKey(), List.copyOf(entry.getValue()));
                }
                return new EmployeeUploadSummary(
                        employeeCode,
                        employeeName,
                        folder,
                        List.copyOf(uploadedLabels),
                        List.copyOf(alreadyExistingLabels),
                        List.copyOf(noMatchFiles),
                        Collections.unmodifiableMap(copiedFailures)
                );
            }
        }

        private static final class FolderErrorBuilder {
            private final String folderName;
            private final File folder;
            private final List<String> messages = new ArrayList<>();

            private FolderErrorBuilder(String folderName, File folder) {
                this.folderName = folderName;
                this.folder = folder;
            }
        }
    }
}
