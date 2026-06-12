package com.kgm.service;

import com.kgm.config.AppConfig;
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
    private static final Pattern DIGIT_SEQUENCE = Pattern.compile("\\d{1,18}");
    private static final Pattern LABELED_DIGIT_SEQUENCE = Pattern.compile(
            "(?i)(?:employee|emp|id|code)\\D{0,12}(\\d{1,18})"
    );
    private static final Pattern LEADING_DIGIT_SEQUENCE = Pattern.compile("^\\s*(\\d{1,18})");

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(String message, int completedFolders, int totalFolders, int percent);

        default void onProgressDetail(ProgressDetail detail) {
            onProgress(detail.status(), detail.completedEmployees(), detail.totalEmployees(), detail.percent());
        }
    }

    public interface ImportControl {
        default boolean stopRequested() {
            return false;
        }

        default void waitIfPaused() throws InterruptedException {
        }
    }

    public record ProgressDetail(
            String employeeCode,
            String documentName,
            String status,
            int uploadedCount,
            int skippedCount,
            int failedCount,
            int duplicateCount,
            int discardedCount,
            int completedEmployees,
            int totalEmployees,
            int percent
    ) {
    }

    public ImportResult importFolders(File[] selectedFolders, ProgressListener progressListener) throws IOException {
        EmployeeDocumentUtil.refreshDocumentTypes();
        List<File> folders = validFolders(selectedFolders);
        boolean compressionEnabled = AppConfig.bulkImportCompressionEnabled();
        ImportSummary summary = new ImportSummary(null, compressionEnabled);
        return importFolders(folders, summary, compressionEnabled, progressListener);
    }

    public ImportResult importConfiguredFolder(ProgressListener progressListener) throws IOException {
        EmployeeDocumentUtil.refreshDocumentTypes();
        Path sourceRoot = AppConfig.bulkImportFolderDirectory();
        boolean compressionEnabled = AppConfig.bulkImportCompressionEnabled();
        ImportSummary summary = new ImportSummary(sourceRoot == null ? null : sourceRoot.toFile(), compressionEnabled);
        List<File> folders = configuredEmployeeFolders(sourceRoot, summary);
        return importFolders(folders, summary, compressionEnabled, progressListener);
    }

    public ImportResult importConfiguredFolderRange(
            long startCode,
            long endCode,
            ProgressListener progressListener,
            ImportControl control
    ) throws IOException, InterruptedException {
        EmployeeDocumentUtil.refreshDocumentTypes();
        Path sourceRoot = AppConfig.bulkImportFolderDirectory();
        boolean compressionEnabled = AppConfig.bulkImportCompressionEnabled();
        ImportSummary summary = new ImportSummary(sourceRoot == null ? null : sourceRoot.toFile(), compressionEnabled);
        List<File> folders = configuredEmployeeFoldersInRange(sourceRoot, startCode, endCode, summary);
        return importExactFolders(folders, summary, compressionEnabled, progressListener, control);
    }

    private ImportResult importFolders(
            List<File> folders,
            ImportSummary summary,
            boolean compressionEnabled,
            ProgressListener progressListener
    ) throws IOException {
        int total = folders.size();
        report(progressListener, "Preparing bulk import from configured employee folders...", 0, total);

        try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
            Set<String> processedEmployeeFolders = new HashSet<>();
            for (int index = 0; index < folders.size(); index++) {
                File folder = folders.get(index);
                reportFolderStep(progressListener, "Scanning Employee-Code " + folder.getName() + "...", index, total, 0, 4);
                processFolder(folder, dao, summary, progressListener, index, total, processedEmployeeFolders, compressionEnabled);
                reportFolderStep(progressListener, "Processed Employee-Code " + folder.getName() + ".", index, total, 4, 4);
            }
        }

        report(progressListener, "Finalizing upload summary...", total, total);
        return summary.toResult();
    }

    private ImportResult importExactFolders(
            List<File> folders,
            ImportSummary summary,
            boolean compressionEnabled,
            ProgressListener progressListener,
            ImportControl control
    ) throws IOException, InterruptedException {
        int total = folders.size();
        ProgressCounters counters = new ProgressCounters();
        reportDetail(progressListener, counters, "", "", "Preparing bulk upload from selected employee-code range...", 0, total, 0);

        try (EmployeeRecordDao dao = new EmployeeRecordDao()) {
            Set<String> processedEmployeeFolders = new HashSet<>();
            for (int index = 0; index < folders.size(); index++) {
                if (control != null && control.stopRequested()) {
                    summary.error(summary.sourceDirectory, "Bulk upload stopped before Employee Code " + folders.get(index).getName() + ".");
                    break;
                }
                if (control != null) {
                    control.waitIfPaused();
                }

                File folder = folders.get(index);
                String code = folder.getName() == null ? "" : folder.getName().trim();
                reportDetail(progressListener, counters, code, "", "Scanning Employee Code " + code + "...", index, total, percentFor(index, total));
                processExactFolder(folder, dao, summary, progressListener, counters, index, total, processedEmployeeFolders, compressionEnabled);
                reportDetail(progressListener, counters, code, "", "Finished Employee Code " + code + ".", index + 1, total, percentFor(index + 1, total));

                if (control != null && control.stopRequested()) {
                    summary.error(folder, "Bulk upload stopped after this employee completed.");
                    break;
                }
            }
        }

        reportDetail(progressListener, counters, "", "", "Finalizing upload summary...", total, total, 100);
        return summary.toResult();
    }

    private void processFolder(
            File folder,
            EmployeeRecordDao dao,
            ImportSummary summary,
            ProgressListener progressListener,
            int folderIndex,
            int totalFolders,
            Set<String> processedEmployeeFolders,
            boolean compressionEnabled
    ) {
        String folderKey = folder.getName() == null ? "" : folder.getName().trim();
        if (folderKey.isEmpty()) {
            summary.error(folder, "Folder name is blank. Rename it to an Employee-Code.");
            return;
        }

        reportFolderStep(progressListener, "Finding Employee-Code " + folderKey + " in database...", folderIndex, totalFolders, 1, 4);
        EmployeeFolder employeeFolder = findEmployeeFolder(folder, dao);
        if (employeeFolder == null) {
            summary.error(folder, "Missing Employee-Code in database: " + folderKey);
            return;
        }
        folder = employeeFolder.folder();
        folderKey = folder.getName() == null ? "" : folder.getName().trim();
        Employee employee = employeeFolder.employee();
        String employeeCode = employee.getEMPLOYEE_CODE();
        if (employeeCode == null || employeeCode.isBlank()) {
            summary.error(folder, "Employee record has no employee code.");
            return;
        }
        if (!processedEmployeeFolders.add(employeeCode)) {
            summary.error(folder, "Employee-Code was already processed in this import: " + employeeCode);
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
                    "Checking Employee-Code " + employeeCode + " / " + file.getName() + "...",
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
            if (matchedThisFolder.contains(documentIndex)) {
                summary.failed(employeeCode, "Duplicate document label in folder", file.getName());
                continue;
            }
            if (EmployeeDocumentUtil.hasStoredPath(EmployeeDocumentUtil.documentPath(employee, documentIndex))) {
                summary.skippedDocument(employeeCode, documentLabel, file.getName(), "Already saved in database; left unchanged");
                continue;
            }
            Path storageTarget = storageTargetPath(employeeCode, documentIndex);
            if (Files.isRegularFile(storageTarget)) {
                if (isSameFile(file.toPath(), storageTarget)) {
                    EmployeeDocumentUtil.setDocumentPath(update, documentIndex, storageLogicalPath(employeeCode, documentIndex));
                    matchedThisFolder.add(documentIndex);
                    uploadedForEmployee++;
                    summary.uploadedDocument(
                            employeeCode,
                            documentLabel,
                            file.getName(),
                            "Already in employee storage; database path saved without copying"
                    );
                    continue;
                }
                summary.skippedDocument(
                        employeeCode,
                        documentLabel,
                        file.getName(),
                        "File already exists in employee storage; left unchanged"
                );
                continue;
            }
            if (EmployeeDocumentUtil.shouldCompressBeforeUpload(file, compressionEnabled)) {
                reportFileStep(
                        progressListener,
                        "Compressing Employee-Code " + employeeCode + " / " + documentLabel + "...",
                        folderIndex,
                        totalFolders,
                        fileIndex,
                        files.size()
                );
            }
            EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(file, compressionEnabled);
            if (!prepared.ready()) {
                summary.failed(employeeCode, failureReason(prepared.message()), file.getName());
                continue;
            }

            try {
                reportFileStep(
                        progressListener,
                        "Uploading Employee-Code " + employeeCode + " / " + documentLabel + "...",
                        folderIndex,
                        totalFolders,
                        fileIndex,
                        files.size()
                );
                String dbPath = storePreparedDocument(employeeCode, documentIndex, file, prepared);
                EmployeeDocumentUtil.setDocumentPath(update, documentIndex, dbPath);
                matchedThisFolder.add(documentIndex);
                uploadedForEmployee++;
                summary.uploadedDocument(employeeCode, documentLabel, file.getName(), uploadStatus(file, prepared, compressionEnabled));
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

    private void processExactFolder(
            File folder,
            EmployeeRecordDao dao,
            ImportSummary summary,
            ProgressListener progressListener,
            ProgressCounters counters,
            int folderIndex,
            int totalFolders,
            Set<String> processedEmployeeFolders,
            boolean compressionEnabled
    ) {
        String folderKey = folder.getName() == null ? "" : folder.getName().trim();
        if (folderKey.isEmpty()) {
            summary.error(folder, "Folder name is blank. Rename it to an Employee Code.");
            counters.failed++;
            return;
        }

        Employee employee = dao.getEmployeeDocumentsByCode(folderKey);
        if (employee == null) {
            summary.error(folder, "Employee not found in database for exact Employee Code: " + folderKey);
            counters.failed++;
            reportDetail(progressListener, counters, folderKey, "", "Employee Code " + folderKey + " was not found in DB.", folderIndex, totalFolders, percentFor(folderIndex, totalFolders));
            return;
        }

        String employeeCode = employee.getEMPLOYEE_CODE() == null ? "" : employee.getEMPLOYEE_CODE().trim();
        if (employeeCode.isEmpty()) {
            summary.error(folder, "Employee record has no employee code.");
            counters.failed++;
            return;
        }
        if (!employeeCode.equals(folderKey)) {
            summary.error(folder, "Folder name must match Employee Code exactly. Found employee code: " + employeeCode);
            counters.failed++;
            return;
        }
        if (!processedEmployeeFolders.add(employeeCode)) {
            summary.error(folder, "Employee Code was already processed in this upload: " + employeeCode);
            counters.duplicates++;
            return;
        }

        summary.employee(employeeCode, employee.getEMP_NAME(), folder);
        reportDetail(progressListener, counters, employeeCode, "", "Reading files for Employee Code " + employeeCode + "...", folderIndex, totalFolders, percentFor(folderIndex, totalFolders));
        List<File> files = documentFiles(folder, summary, folderKey);
        if (files.isEmpty()) {
            summary.failed(employeeCode, "Empty folder", "No supported document files found directly inside folder");
            counters.failed++;
            return;
        }

        Set<Integer> matchedThisFolder = new HashSet<>();
        Employee update = new Employee();
        update.setEMPLOYEE_CODE(employeeCode);
        int uploadedForEmployee = 0;

        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            File file = files.get(fileIndex);
            String fileName = file.getName();
            reportDetail(
                    progressListener,
                    counters,
                    employeeCode,
                    fileName,
                    "Checking " + fileName + "...",
                    folderIndex,
                    totalFolders,
                    filePercent(folderIndex, totalFolders, fileIndex, files.size())
            );

            String typeValidation = EmployeeDocumentUtil.validateUploadImageType(file);
            if (typeValidation != null) {
                summary.failed(employeeCode, failureReason(typeValidation), fileName);
                counters.failed++;
                continue;
            }

            EmployeeDocumentUtil.DocumentMatch match = EmployeeDocumentUtil.matchDocumentForFile(file);
            if (!match.matched()) {
                summary.noMatch(employeeCode, fileName);
                counters.discarded++;
                continue;
            }
            if (match.ambiguous()) {
                summary.failed(employeeCode, "Ambiguous document label - not uploaded", fileName);
                counters.discarded++;
                continue;
            }

            int documentIndex = match.documentIndex();
            String documentLabel = EmployeeDocumentUtil.cleanDocumentLabel(documentIndex);
            if (matchedThisFolder.contains(documentIndex)) {
                summary.failed(employeeCode, "Duplicate document label in folder", fileName);
                counters.duplicates++;
                continue;
            }
            if (EmployeeDocumentUtil.hasStoredPath(EmployeeDocumentUtil.documentPath(employee, documentIndex))) {
                summary.skippedDocument(employeeCode, documentLabel, fileName, "already exists in DB - not uploaded");
                counters.skipped++;
                counters.duplicates++;
                continue;
            }
            Path storageTarget = storageTargetPath(employeeCode, documentIndex);
            if (Files.isRegularFile(storageTarget)) {
                if (isSameFile(file.toPath(), storageTarget)) {
                    EmployeeDocumentUtil.setDocumentPath(update, documentIndex, storageLogicalPath(employeeCode, documentIndex));
                    matchedThisFolder.add(documentIndex);
                    uploadedForEmployee++;
                    counters.uploaded++;
                    summary.uploadedDocument(
                            employeeCode,
                            documentLabel,
                            fileName,
                            "Already in employee storage; database path saved without copying"
                    );
                    continue;
                }
                summary.skippedDocument(employeeCode, documentLabel, fileName, "Different file already exists in employee storage - not uploaded");
                counters.skipped++;
                counters.duplicates++;
                continue;
            }

            if (EmployeeDocumentUtil.shouldCompressBeforeUpload(file, compressionEnabled)) {
                reportDetail(
                        progressListener,
                        counters,
                        employeeCode,
                        fileName,
                        "Compressing " + fileName + "...",
                        folderIndex,
                        totalFolders,
                        filePercent(folderIndex, totalFolders, fileIndex, files.size())
                );
            }
            EmployeeDocumentUtil.PreparedUploadFile prepared = EmployeeDocumentUtil.prepareImageForUpload(file, compressionEnabled);
            if (!prepared.ready()) {
                summary.failed(employeeCode, failureReason(prepared.message()), fileName);
                counters.failed++;
                continue;
            }

            try {
                reportDetail(
                        progressListener,
                        counters,
                        employeeCode,
                        fileName,
                        "Uploading " + fileName + "...",
                        folderIndex,
                        totalFolders,
                        filePercent(folderIndex, totalFolders, fileIndex, files.size())
                );
                String dbPath = storePreparedDocument(employeeCode, documentIndex, file, prepared);
                EmployeeDocumentUtil.setDocumentPath(update, documentIndex, dbPath);
                matchedThisFolder.add(documentIndex);
                uploadedForEmployee++;
                counters.uploaded++;
                summary.uploadedDocument(employeeCode, documentLabel, fileName, uploadStatus(file, prepared, compressionEnabled));
            } catch (IOException exception) {
                summary.failed(employeeCode, failureReason("Upload failed: " + exception.getMessage()), fileName);
                counters.failed++;
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
            reportDetail(progressListener, counters, employeeCode, "", "Saving database updates for " + employeeCode + "...", folderIndex, totalFolders, percentFor(folderIndex, totalFolders));
            dao.updateEmployeeDynamic(update);
        } catch (Exception exception) {
            summary.rollbackUploadedForEmployee(
                    employeeCode,
                    failureReason("Database update failed after copying files: " + exception.getMessage())
            );
            counters.failed += uploadedForEmployee;
            counters.uploaded = Math.max(0, counters.uploaded - uploadedForEmployee);
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
        String name = folder == null || folder.getName() == null ? "" : folder.getName().trim();
        if (name.isBlank()) {
            return null;
        }
        Employee employee = dao.getEmployeeDocumentsByCode(name);
        if (employee != null) {
            return new EmployeeFolder(folder, employee);
        }
        return null;
    }

    private Path storageTargetPath(String employeeCode, int documentIndex) {
        String storageName = EmployeeDocumentUtil.documentType(documentIndex).storageName();
        return EmployeeStorageUtil.employeeDirectory(employeeCode).resolve(storageName).normalize();
    }

    private String storageLogicalPath(String employeeCode, int documentIndex) {
        String storageName = EmployeeDocumentUtil.documentType(documentIndex).storageName();
        if (EmployeeDocumentUtil.isProfileImageDocument(documentIndex)) {
            return EmployeeStorageUtil.profileImagePath(employeeCode);
        }
        return EmployeeStorageUtil.documentPath(employeeCode, storageName);
    }

    private String storePreparedDocument(
            String employeeCode,
            int documentIndex,
            File originalSource,
            EmployeeDocumentUtil.PreparedUploadFile prepared
    ) throws IOException {
        Path storageTarget = storageTargetPath(employeeCode, documentIndex);
        if (!sourceIsInEmployeeStorageDirectory(employeeCode, originalSource)) {
            return EmployeeDocumentUtil.copyDocumentToEmployeeStorage(employeeCode, documentIndex, prepared.file());
        }

        Files.createDirectories(storageTarget.getParent());
        if (prepared.compressed()) {
            Files.copy(prepared.file().toPath(), storageTarget, StandardCopyOption.REPLACE_EXISTING);
            deleteOriginalIfDifferent(originalSource, storageTarget);
        } else {
            Files.move(originalSource.toPath(), storageTarget, StandardCopyOption.REPLACE_EXISTING);
        }
        return storageLogicalPath(employeeCode, documentIndex);
    }

    private boolean sourceIsInEmployeeStorageDirectory(String employeeCode, File source) {
        if (source == null || source.getParentFile() == null) {
            return false;
        }
        Path employeeDirectory = EmployeeStorageUtil.employeeDirectory(employeeCode).toAbsolutePath().normalize();
        Path parent = source.getParentFile().toPath().toAbsolutePath().normalize();
        return parent.equals(employeeDirectory);
    }

    private void deleteOriginalIfDifferent(File originalSource, Path storageTarget) throws IOException {
        if (originalSource == null) {
            return;
        }
        Path originalPath = originalSource.toPath().toAbsolutePath().normalize();
        Path targetPath = storageTarget.toAbsolutePath().normalize();
        if (!originalPath.equals(targetPath)) {
            Files.deleteIfExists(originalPath);
        }
    }

    private boolean isSameFile(Path first, Path second) {
        try {
            return Files.isSameFile(first, second);
        } catch (IOException exception) {
            return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
        }
    }

    private String uploadStatus(
            File source,
            EmployeeDocumentUtil.PreparedUploadFile prepared,
            boolean compressionEnabled
    ) {
        String originalSize = EmployeeDocumentUtil.formatSize(source.length());
        if (!compressionEnabled) {
            return "Compression off; uploaded original file (" + originalSize + ")";
        }
        if (prepared.compressed()) {
            return prepared.message() == null || prepared.message().isBlank()
                    ? "Compressed before upload"
                    : prepared.message();
        }
        return "No compression needed (" + originalSize + ")";
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

    private List<File> configuredEmployeeFolders(Path sourceRoot, ImportSummary summary) {
        if (sourceRoot == null) {
            summary.error(null, "KGM_EMPLOYEE_STORAGE_DIR is not configured.");
            return List.of();
        }
        if (!Files.isDirectory(sourceRoot)) {
            summary.error(sourceRoot.toFile(), "Bulk import source folder was not found: " + sourceRoot);
            return List.of();
        }
        try (var stream = Files.list(sourceRoot)) {
            List<Path> entries = stream
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            List<File> folders = new ArrayList<>();
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    folders.add(entry.toFile());
                } else {
                    summary.error(entry.toFile(),
                            "Invalid item in bulk import folder. Only direct Employee-Code folders are processed.");
                }
            }
            if (folders.isEmpty()) {
                summary.error(sourceRoot.toFile(), "No employee folders found directly inside configured bulk import folder.");
            }
            return folders;
        } catch (IOException exception) {
            summary.error(sourceRoot.toFile(), "Bulk import source folder could not be read: " + exception.getMessage());
            return List.of();
        }
    }

    private List<File> configuredEmployeeFoldersInRange(
            Path sourceRoot,
            long startCode,
            long endCode,
            ImportSummary summary
    ) {
        if (sourceRoot == null) {
            summary.error(null, "Bulk upload path is not configured.");
            return List.of();
        }
        if (!Files.exists(sourceRoot)) {
            summary.error(sourceRoot.toFile(), "Bulk upload path was not found or the LAN folder is unavailable: " + sourceRoot);
            return List.of();
        }
        if (!Files.isDirectory(sourceRoot)) {
            summary.error(sourceRoot.toFile(), "Bulk upload path is not a folder: " + sourceRoot);
            return List.of();
        }
        if (!Files.isReadable(sourceRoot)) {
            summary.error(sourceRoot.toFile(), "Bulk upload path is not accessible. Check LAN permissions: " + sourceRoot);
            return List.of();
        }

        try (var stream = Files.list(sourceRoot)) {
            List<File> allFolders = new ArrayList<>();
            List<File> matchingFolders = new ArrayList<>();
            stream
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(path -> {
                        if (!Files.isDirectory(path)) {
                            summary.error(path.toFile(), "Invalid item in bulk upload folder. Only direct Employee Code folders are processed.");
                            return;
                        }
                        File folder = path.toFile();
                        allFolders.add(folder);
                        Long code = exactNumericCode(folder.getName());
                        if (code == null) {
                            summary.error(folder, "Folder name must be an exact numeric Employee Code.");
                            return;
                        }
                        if (code >= startCode && code <= endCode) {
                            matchingFolders.add(folder);
                        }
                    });
            if (allFolders.isEmpty()) {
                summary.error(sourceRoot.toFile(), "No employee folders found directly inside the configured bulk upload path.");
            } else if (matchingFolders.isEmpty()) {
                summary.error(sourceRoot.toFile(), "No folder names matched Employee Codes in range " + startCode + " to " + endCode + ".");
            }
            return matchingFolders;
        } catch (IOException exception) {
            summary.error(sourceRoot.toFile(), "Bulk upload path could not be read. Check LAN connection and permissions: " + exception.getMessage());
            return List.of();
        }
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

    private void reportDetail(
            ProgressListener progressListener,
            ProgressCounters counters,
            String employeeCode,
            String documentName,
            String status,
            int completedEmployees,
            int totalEmployees,
            int percent
    ) {
        if (progressListener == null) {
            return;
        }
        progressListener.onProgressDetail(new ProgressDetail(
                employeeCode == null ? "" : employeeCode,
                documentName == null ? "" : documentName,
                status == null ? "" : status,
                counters.uploaded,
                counters.skipped,
                counters.failed,
                counters.duplicates,
                counters.discarded,
                completedEmployees,
                totalEmployees,
                percent
        ));
    }

    private int percentFor(int completed, int total) {
        return total <= 0 ? 100 : Math.min(100, Math.max(0, (int) Math.round(completed * 100.0 / total)));
    }

    private int filePercent(int folderIndex, int totalFolders, int fileIndex, int fileCount) {
        if (totalFolders <= 0) {
            return 100;
        }
        double folderProgress = folderIndex / (double) totalFolders;
        double fileProgress = fileCount <= 0 ? 0 : fileIndex / (double) fileCount / totalFolders;
        return Math.min(99, Math.max(0, (int) Math.round((folderProgress + fileProgress) * 100.0)));
    }

    private Long exactNumericCode(String folderName) {
        String clean = folderName == null ? "" : folderName.trim();
        if (!clean.matches("\\d{1,18}")) {
            return null;
        }
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record ImportResult(
            File sourceDirectory,
            boolean compressionEnabled,
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

    public record DocumentUploadDetail(String label, String fileName, String status) {
        public String displayText() {
            StringBuilder text = new StringBuilder(label == null || label.isBlank() ? fileName : label);
            if (fileName != null && !fileName.isBlank()) {
                text.append(" (").append(fileName).append(")");
            }
            if (status != null && !status.isBlank()) {
                text.append(" - ").append(status);
            }
            return text.toString();
        }
    }

    public record FolderError(String folderName, File folder, List<String> messages) {
    }

    public record EmployeeUploadSummary(
            String employeeCode,
            String employeeName,
            File folder,
            List<String> uploadedLabels,
            List<DocumentUploadDetail> uploadedDetails,
            List<String> alreadyExistingLabels,
            List<DocumentUploadDetail> skippedDetails,
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
            int count = skippedDetails.size() + noMatchFiles.size();
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

    private static final class ProgressCounters {
        private int uploaded;
        private int skipped;
        private int failed;
        private int duplicates;
        private int discarded;
    }

    private static final class ImportSummary {
        private final File sourceDirectory;
        private final boolean compressionEnabled;
        private final Map<String, EmployeeSummaryBuilder> employeesByCode = new LinkedHashMap<>();
        private final Map<String, FolderErrorBuilder> errorsByFolder = new LinkedHashMap<>();

        private ImportSummary(File sourceDirectory, boolean compressionEnabled) {
            this.sourceDirectory = sourceDirectory;
            this.compressionEnabled = compressionEnabled;
        }

        private void employee(String employeeCode, String employeeName, File folder) {
            employee(employeeCode).update(employeeName, folder);
        }

        private void uploadedDocument(String employeeCode, String label, String fileName, String status) {
            employee(employeeCode).uploaded(label, fileName, status);
        }

        private void alreadyExists(String employeeCode, String label) {
            employee(employeeCode).alreadyExists(label, "", "Already saved; left unchanged");
        }

        private void skippedDocument(String employeeCode, String label, String fileName, String status) {
            employee(employeeCode).alreadyExists(label, fileName, status);
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
                    sourceDirectory,
                    compressionEnabled,
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
            private final List<DocumentUploadDetail> uploadedDetails = new ArrayList<>();
            private final List<String> alreadyExistingLabels = new ArrayList<>();
            private final List<DocumentUploadDetail> skippedDetails = new ArrayList<>();
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

            private void uploaded(String label, String fileName, String status) {
                addUnique(uploadedLabels, label);
                uploadedDetails.add(new DocumentUploadDetail(label, fileName, status));
            }

            private void alreadyExists(String label, String fileName, String status) {
                addUnique(alreadyExistingLabels, label);
                skippedDetails.add(new DocumentUploadDetail(label, fileName, status));
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
                        List.copyOf(uploadedDetails),
                        List.copyOf(alreadyExistingLabels),
                        List.copyOf(skippedDetails),
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
