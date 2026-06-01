package com.kgm.service;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.model.Employee;
import com.kgm.util.EmployeeDocumentUtil;
import com.kgm.util.EmployeeStorageUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BulkFolderDocumentImportService {
    public static final int MAX_FOLDERS = 50;

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
            summary.error(folder, "No employee found in DB for this folder name.");
            return;
        }

        reportFolderStep(progressListener, "Finding employee for folder " + folderKey + "...", folderIndex, totalFolders, 1, 4);
        EmployeeFolder employeeFolder = findEmployeeFolder(folder, dao);
        if (employeeFolder == null) {
            summary.error(folder, "No employee found in DB for this folder name.");
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

        reportFolderStep(progressListener, "Reading files inside " + folderKey + "...", folderIndex, totalFolders, 2, 4);
        List<File> files = documentFiles(folder, summary, folderKey);
        if (files.isEmpty()) {
            summary.error(folder, "No document files found.");
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
            String prefix = file.getName() + " - ";
            String validationMessage = EmployeeDocumentUtil.validateImageFile(file);
            if (validationMessage != null) {
                summary.error(folder, prefix + validationMessage);
                continue;
            }

            EmployeeDocumentUtil.DocumentMatch match = EmployeeDocumentUtil.matchDocumentForFile(file);
            if (!match.matched()) {
                summary.error(folder, prefix + "No document label matched this file name.");
                continue;
            }
            if (match.ambiguous()) {
                summary.error(folder, prefix + "The name matches more than one document. Rename it to the exact document label and try again.");
                continue;
            }
            int documentIndex = match.documentIndex();
            String documentLabel = EmployeeDocumentUtil.cleanDocumentLabel(documentIndex);
            if (EmployeeDocumentUtil.hasStoredPath(EmployeeDocumentUtil.documentPath(employee, documentIndex))) {
                summary.error(folder, prefix + documentLabel + " already exists in DB, so it was not uploaded.");
                continue;
            }
            if (matchedThisFolder.contains(documentIndex)) {
                summary.error(folder, prefix + "Another file in this folder already matched " + documentLabel + ".");
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
                String dbPath = copyDocument(employeeCode, documentIndex, file);
                EmployeeDocumentUtil.setDocumentPath(update, documentIndex, dbPath);
                matchedThisFolder.add(documentIndex);
                uploadedForEmployee++;
                summary.uploadedDocument(employeeCode, documentLabel);
            } catch (IOException exception) {
                summary.error(folder, prefix + "Upload failed: " + exception.getMessage());
            }
        }

        if (uploadedForEmployee == 0) {
            return;
        }

        try {
            reportFolderStep(progressListener, "Saving database updates for " + employeeCode + "...", folderIndex, totalFolders, 3, 4);
            dao.updateEmployeeDynamic(update);
        } catch (Exception exception) {
            summary.rollbackUploadedForEmployee(employeeCode);
            summary.error(folder, "Database update failed after copying files: " + exception.getMessage());
        }
    }

    private EmployeeFolder findEmployeeFolder(File folder, EmployeeRecordDao dao) {
        File candidate = folder;
        for (int depth = 0; candidate != null && depth < 5; depth++) {
            String name = candidate.getName() == null ? "" : candidate.getName().trim();
            if (!name.isEmpty()) {
                Employee employee = dao.getEmployeeDocumentsByCodeOrId(name);
                if (employee != null) {
                    return new EmployeeFolder(candidate, employee);
                }
            }
            candidate = candidate.getParentFile();
        }
        return null;
    }

    private String copyDocument(String employeeCode, int documentIndex, File file) throws IOException {
        String storageName = EmployeeDocumentUtil.documentType(documentIndex).storageName();
        Path employeeDir = EmployeeStorageUtil.ensureEmployeeDirectory(employeeCode);
        Path destination;
        String dbPath;
        if (EmployeeDocumentUtil.isProfileImageDocument(documentIndex)) {
            destination = employeeDir.resolve(storageName);
            dbPath = EmployeeStorageUtil.profileImagePath(employeeCode);
        } else {
            destination = EmployeeStorageUtil.ensureDocumentDirectory(employeeCode).resolve(storageName);
            dbPath = EmployeeStorageUtil.documentPath(employeeCode, storageName);
        }
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return dbPath;
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
        try (var stream = Files.walk(folder.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .toList();
        } catch (IOException exception) {
            summary.error(folder, "Folder could not be scanned: " + exception.getMessage());
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
            List<FolderError> folderErrors
    ) {
        public int uploadedCount() {
            int count = 0;
            for (UploadedEmployee employee : uploadedEmployees) {
                count += employee.labels().size();
            }
            return count;
        }

        public int skippedCount() {
            int count = 0;
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

    private record EmployeeFolder(File folder, Employee employee) {
    }

    private static final class ImportSummary {
        private final Map<String, List<String>> uploadedByEmployee = new LinkedHashMap<>();
        private final Map<String, FolderErrorBuilder> errorsByFolder = new LinkedHashMap<>();

        private void uploadedDocument(String employeeCode, String label) {
            uploadedByEmployee.computeIfAbsent(employeeCode, key -> new ArrayList<>()).add(label);
        }

        private void error(File folder, String message) {
            String key = folder == null ? "" : folder.getAbsolutePath();
            String folderName = folder == null ? "Unknown folder" : folder.getName();
            errorsByFolder.computeIfAbsent(key, ignored -> new FolderErrorBuilder(folderName, folder)).messages.add(message);
        }

        private void rollbackUploadedForEmployee(String employeeCode) {
            uploadedByEmployee.remove(employeeCode);
        }

        private ImportResult toResult() {
            List<UploadedEmployee> uploadedEmployees = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : uploadedByEmployee.entrySet()) {
                uploadedEmployees.add(new UploadedEmployee(entry.getKey(), List.copyOf(entry.getValue())));
            }

            List<FolderError> folderErrors = new ArrayList<>();
            for (FolderErrorBuilder builder : errorsByFolder.values()) {
                folderErrors.add(new FolderError(builder.folderName, builder.folder, List.copyOf(builder.messages)));
            }
            return new ImportResult(List.copyOf(uploadedEmployees), List.copyOf(folderErrors));
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
