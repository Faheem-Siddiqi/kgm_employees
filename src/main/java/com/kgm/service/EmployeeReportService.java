package com.kgm.service;

import com.kgm.dao.EmployeeRecordDao;
import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.util.EmployeeDocumentUtil;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EmployeeReportService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final double PAGE_WIDTH = 595;
    private static final double PAGE_HEIGHT = 842;
    private static final double MARGIN = 42;
    private static final double BODY_BOTTOM = 74;
    private static final double CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final double LABEL_WIDTH = 150;

    private static final String PRIMARY = "0070D2";
    private static final String TEXT_PRIMARY = "232B36";
    private static final String TEXT_SECONDARY = "637381";
    private static final String BORDER = "DCE2E8";
    private static final String HEADER_FILL = "EEF5FC";
    private static final String ROW_ALT = "F7F9FB";
    private static final String SUCCESS = "0F8B4C";
    private static final String WARNING = "B42318";

    private final EmployeeRecordDao employeeRecordDao = new EmployeeRecordDao();

    public PackageResult generateEmployeePackage(String employeeCode, File selectedDirectory) throws Exception {
        return generateEmployeePackage(employeeCode, selectedDirectory, PackageOptions.all());
    }

    public PackageResult generateEmployeePackage(
            String employeeCode,
            File selectedDirectory,
            PackageOptions options
    ) throws Exception {
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new IllegalArgumentException("Employee code is required.");
        }
        if (selectedDirectory == null) {
            throw new IllegalArgumentException("Please choose a folder to save the report package.");
        }
        PackageOptions effectiveOptions = options == null ? PackageOptions.all() : options;

        Employee employee = employeeRecordDao.getFullEmployeeByCode(employeeCode.trim());
        if (employee == null) {
            throw new IllegalArgumentException("Employee record was not found for code: " + employeeCode);
        }

        List<DocumentEntry> selectedDocuments = selectedDocumentEntries(employee, effectiveOptions);
        List<DocumentEntry> mergedPdfDocuments = effectiveOptions.includeMergedDocumentsPdf()
                ? mergeableDocumentEntries(employee)
                : List.of();
        if (!effectiveOptions.includePdfProfile()
                && !effectiveOptions.includeMergedDocumentsPdf()
                && selectedDocuments.isEmpty()) {
            throw new IllegalArgumentException("Select the PDF profile, all documents PDF, or at least one saved document.");
        }
        if (effectiveOptions.includeMergedDocumentsPdf() && mergedPdfDocuments.isEmpty()) {
            throw new IllegalArgumentException(" No saved document image files are available for the All Documents PDF.");
        }

        Path baseDirectory = selectedDirectory.toPath().toAbsolutePath().normalize();
        Files.createDirectories(baseDirectory);
        if (!Files.isDirectory(baseDirectory)) {
            throw new IOException("Selected location is not a folder: " + baseDirectory);
        }

        Path packageFolder = uniqueDirectory(baseDirectory.resolve(packageFolderName(employee)));
        Files.createDirectories(packageFolder);

        List<PackagedDocument> documents = selectedDocuments.isEmpty()
                ? new ArrayList<>()
                : copyEmployeeDocuments(selectedDocuments, packageFolder.resolve("documents"));

        File pdfFile = null;
        if (effectiveOptions.includePdfProfile()) {
            Path pdfPath = packageFolder.resolve("Employee_Report_" + sanitizeFileName(employee.getEMPLOYEE_CODE(), "Employee") + ".pdf");
            pdfFile = writePdfSafely(pdfPath, employee, documents);
        }

        File mergedDocumentsPdfFile = null;
        int mergedDocumentCount = 0;
        if (effectiveOptions.includeMergedDocumentsPdf()) {
            Path mergedPdfPath = packageFolder.resolve(
                    "All_Documents_" + sanitizeFileName(employee.getEMPLOYEE_CODE(), "Employee") + ".pdf"
            );
            MergedDocumentsPdfResult mergedResult =
                    writeMergedDocumentsPdfSafely(mergedPdfPath, mergedPdfDocuments);
            mergedDocumentsPdfFile = mergedResult.file();
            mergedDocumentCount = mergedResult.documentCount();
        }

        int copied = 0;
        for (PackagedDocument document : documents) {
            if ("Copied".equals(document.status())) {
                copied++;
            }
        }

        return new PackageResult(packageFolder.toFile(), pdfFile, mergedDocumentsPdfFile, copied,
                documents.size(), mergedDocumentCount);
    }

    private String packageFolderName(Employee employee) {
        String name = sanitizeFileName(employee.getEMP_NAME(), "Employee");
        String code = sanitizeFileName(employee.getEMPLOYEE_CODE(), "Record");
        return name + " - " + code;
    }

    public List<AvailableDocument> availableDocuments(Employee employee) {
        List<AvailableDocument> available = new ArrayList<>();
        for (DocumentEntry entry : savedDocumentEntries(employee)) {
            File sourceFile = resolveDocumentPath(entry.sourcePath()).toFile();
            available.add(new AvailableDocument(entry.label(), sourceFile, sourceFile.isFile(), !entry.employeePhoto()));
        }
        return available;
    }

    private List<DocumentEntry> selectedDocumentEntries(Employee employee, PackageOptions options) {
        List<DocumentEntry> available = savedDocumentEntries(employee);
        if (options.includeAllDocuments()) {
            return available;
        }

        Set<String> selectedLabels = new HashSet<>(options.selectedDocumentLabels());
        List<DocumentEntry> selected = new ArrayList<>();
        for (DocumentEntry entry : available) {
            if (selectedLabels.contains(entry.label())) {
                selected.add(entry);
            }
        }
        return selected;
    }

    private List<DocumentEntry> savedDocumentEntries(Employee employee) {
        List<DocumentEntry> available = new ArrayList<>();
        for (DocumentEntry entry : documentEntries(employee)) {
            if (!hasStoredPath(entry.sourcePath())) {
                continue;
            }
            available.add(entry);
        }
        return available;
    }

    private List<DocumentEntry> mergeableDocumentEntries(Employee employee) {
        List<DocumentEntry> available = new ArrayList<>();
        for (DocumentEntry entry : savedDocumentEntries(employee)) {
            if (entry.employeePhoto()) {
                continue;
            }
            Path source = resolveDocumentPath(entry.sourcePath());
            if (Files.isRegularFile(source)) {
                available.add(entry);
            }
        }
        return available;
    }

    private List<PackagedDocument> copyEmployeeDocuments(List<DocumentEntry> entries, Path documentsDirectory) throws IOException {
        Files.createDirectories(documentsDirectory);

        List<PackagedDocument> packaged = new ArrayList<>();
        for (DocumentEntry entry : entries) {
            if (!hasStoredPath(entry.sourcePath())) {
                packaged.add(new PackagedDocument(entry.label(), "-", "-", "Not provided"));
                continue;
            }

            Path source = resolveDocumentPath(entry.sourcePath());
            if (!Files.isRegularFile(source)) {
                packaged.add(new PackagedDocument(entry.label(), source.toString(), "-", "Missing file"));
                continue;
            }

            String extension = extension(source);
            String fileName = sanitizeFileName(entry.label(), "Document") + extension;
            Path target = nextAvailableFile(documentsDirectory.resolve(fileName));
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            packaged.add(new PackagedDocument(entry.label(), source.toString(), target.getFileName().toString(), "Copied"));
        }
        return packaged;
    }

    private List<DocumentEntry> documentEntries(Employee employee) {
        List<DocumentEntry> entries = new ArrayList<>();
        entries.add(new DocumentEntry("Employee Photo", employee.getEMP_IMG(), true));
        for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
            entries.add(new DocumentEntry(
                    EmployeeDocumentUtil.cleanDocumentLabel(index),
                    EmployeeDocumentUtil.documentPath(employee, index)
            ));
        }
        return entries;
    }

    private Path resolveDocumentPath(String rawPath) {
        Path path = Path.of(rawPath.trim());
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Path.of(System.getProperty("user.dir")).resolve(path).normalize();
    }

    private File writePdfSafely(Path target, Employee employee, List<PackagedDocument> documents) throws IOException {
        Path targetPath = target.toAbsolutePath().normalize();
        Path parent = targetPath.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
            targetPath = parent.resolve(targetPath.getFileName());
        }
        Files.createDirectories(parent);

        Path temp = Files.createTempFile(parent, targetPath.getFileName().toString(), ".tmp");
        boolean saved = false;
        try {
            try (OutputStream output = Files.newOutputStream(temp)) {
                PdfDocument document = buildPdf(employee, documents);
                document.write(output);
            }
            File savedFile = commitPdf(temp, targetPath);
            saved = true;
            return savedFile;
        } finally {
            if (!saved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    private MergedDocumentsPdfResult writeMergedDocumentsPdfSafely(
            Path target,
            List<DocumentEntry> entries
    ) throws IOException {
        List<DocumentImage> images = readDocumentImages(entries);
        if (images.isEmpty()) {
            throw new IllegalArgumentException(" No saved document image files are available for the All Documents PDF.");
        }

        Path targetPath = target.toAbsolutePath().normalize();
        Path parent = targetPath.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
            targetPath = parent.resolve(targetPath.getFileName());
        }
        Files.createDirectories(parent);

        Path temp = Files.createTempFile(parent, targetPath.getFileName().toString(), ".tmp");
        boolean saved = false;
        try {
            try (OutputStream output = Files.newOutputStream(temp)) {
                DocumentImagePdf document = new DocumentImagePdf(images);
                document.write(output);
            }
            File savedFile = commitPdf(temp, targetPath);
            saved = true;
            return new MergedDocumentsPdfResult(savedFile, images.size());
        } finally {
            if (!saved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    private List<DocumentImage> readDocumentImages(List<DocumentEntry> entries) throws IOException {
        List<DocumentImage> images = new ArrayList<>();
        for (DocumentEntry entry : entries) {
            Path source = resolveDocumentPath(entry.sourcePath());
            if (!Files.isRegularFile(source)) {
                continue;
            }

            DocumentImage image = readDocumentImage(entry.label(), source);
            if (image != null) {
                images.add(image);
            }
        }
        return images;
    }

    private DocumentImage readDocumentImage(String label, Path source) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            return null;
        }

        byte[] imageBytes;
        String extension = extension(source).toLowerCase(Locale.ROOT);
        if (".jpg".equals(extension) || ".jpeg".equals(extension)) {
            imageBytes = Files.readAllBytes(source);
        } else {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            try {
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(rgb, "jpg", buffer);
            imageBytes = buffer.toByteArray();
        }

        return new DocumentImage(label, source, image.getWidth(), image.getHeight(), imageBytes);
    }

    private File commitPdf(Path temp, Path target) throws IOException {
        try {
            moveReplacing(temp, target);
            return target.toFile();
        } catch (IOException firstFailure) {
            Path fallback = nextAvailableFile(target);
            try {
                moveNew(temp, fallback);
                return fallback.toFile();
            } catch (IOException secondFailure) {
                firstFailure.addSuppressed(secondFailure);
                throw reportSaveException(target, firstFailure);
            }
        }
    }

    private void moveReplacing(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void moveNew(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target);
        }
    }

    private IOException reportSaveException(Path target, IOException cause) {
        if (cause instanceof AccessDeniedException) {
            return new IOException("Report could not be saved. Close any open copy of the PDF and try again.\nPath: "
                    + target, cause);
        }
        return new IOException("Report could not be saved.\nPath: " + target, cause);
    }

    private PdfDocument buildPdf(Employee employee, List<PackagedDocument> documents) {
        PdfDocument document = new PdfDocument();
        ReportWriter writer = new ReportWriter(document, employee);
        writer.drawEmployeeSummary();
        writer.drawSection("Employee Identity");
        writer.drawRows(new String[][]{
                {"Employee Name", display(employee.getEMP_NAME())},
                {"Employee Code", display(employee.getEMPLOYEE_CODE())},
                {"CNIC / NID", display(employee.getNID())},
                {"Father Name", display(employee.getFATHER_NAME())},
                {"Mother Name", display(employee.getMOTHER_NAME())},
                {"Gender", display(employee.getGENDER())},
                {"Date of Birth", display(employee.getDOB())},
                {"Marital Status", display(employee.getM_STATUS())},
                {"Nationality", display(employee.getNATIONALITY())},
                {"Religion", display(employee.getRELIGION())},
                {"Blood Group", display(employee.getBLOOD_GROUP())}
        });

        writer.drawSection("Employment Details");
        writer.drawRows(new String[][]{
                {"Department", display(employee.getDEPARTMENT())},
                {"Designation", display(employee.getDESIGNATION())},
                {"Grade", display(employee.getGRADE())},
                {"Employee Status", display(employee.getEMP_STATUS())},
                {"Joining Date", display(employee.getJOINING_DATE())},
                {"Resign Date", display(employee.getRESIGN_DATE())},
                {"Resign Reason", display(employee.getRESIGN_REASON())},
                {"Shift", display(employee.getSHIFT())},
                {"Probation Period", display(employee.getPROB_PERIOD())},
                {"Experience in KTML", display(employee.getEXP_IN_KTML())}
        });

        writer.drawSection("Contact and Organization");
        writer.drawRows(new String[][]{
                {"Contact Number", display(employee.getEMP_CONTNO())},
                {"Emergency Number", display(employee.getEMERGENCY_NO())},
                {"Personal Email", display(employee.getPERSONAL_EMAIL())},
                {"Official Email", display(employee.getOFFICIAL_EMAIL())},
                {"Current Address", display(employee.getCURRENT_ADR())},
                {"Permanent Address", display(employee.getPERMANENT_ADR())},
                {"Unit Code", display(employee.getUNT_CODE())},
                {"Division", display(employee.getDIVISION())},
                {"Branch", display(employee.getBRANCH_NAME())},
                {"Reporting Employee", display(employee.getREP_EMP_ID())}
        });

        writer.drawSection("Reference and Compliance");
        writer.drawRows(new String[][]{
                {"Social Security No", display(employee.getSS_NO())},
                {"EOBI No", display(employee.getEOBI_NO())},
                {"Tax No", display(employee.getTAX_NO())},
                {"Bank Name", display(employee.getBANK_NAME())},
                {"Bank Account No", display(employee.getBANK_AC_NO())},
                {"Clearance Status", display(employee.getCLEARANCE_STATUS())},
                {"HOD Check", display(employee.getHOD_CHECK())},
                {"Security Head Check", display(employee.getSEC_HEAD_CHK())},
                {"NIC Verification", display(employee.getNIC_VERIFY())},
                {"Wellness Card No", display(employee.getWELLNESS_CARD_NO())}
        });

        for (Map.Entry<String, List<String[]>> customSection : customReportRows(employee).entrySet()) {
            writer.drawSection(customSection.getKey());
            writer.drawRows(customSection.getValue().toArray(new String[0][]));
        }

        writer.drawSection("Document Checklist");
        writer.drawDocumentTable(documents);
        document.addFooters();
        return document;
    }

    private Map<String, List<String[]>> customReportRows(Employee employee) {
        Map<String, List<String[]>> rows = new LinkedHashMap<>();
        try {
            for (EmployeeFieldDefinition definition : new EmployeeFieldDefinitionDao().listDetailFields()) {
                if (!definition.customField()) {
                    continue;
                }
                String value = employee.getDynamicField(definition.columnName());
                rows.computeIfAbsent(definition.heading(), ignored -> new ArrayList<>())
                        .add(new String[]{definition.label(), display(value)});
            }
        } catch (RuntimeException exception) {
            exception.printStackTrace();
        }
        return rows;
    }

    private Path uniqueDirectory(Path target) throws IOException {
        if (!Files.exists(target)) {
            return target;
        }

        Path parent = target.getParent();
        String name = target.getFileName().toString();
        for (int index = 1; index <= 99; index++) {
            Path candidate = parent.resolve(name + " (" + index + ")");
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new FileAlreadyExistsException(target.toString(), null, "No available employee package folder name.");
    }

    private Path nextAvailableFile(Path target) throws IOException {
        if (!Files.exists(target)) {
            return target;
        }

        Path parent = target.getParent();
        String fileName = target.getFileName().toString();
        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        for (int index = 1; index <= 99; index++) {
            Path candidate = parent.resolve(base + " (" + index + ")" + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new FileAlreadyExistsException(target.toString(), null, "No available numbered filename.");
    }

    private String extension(Path source) {
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot) : "";
    }

    private String sanitizeFileName(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        if (!hasStoredPath(cleaned)) {
            cleaned = fallback;
        }
        cleaned = cleaned
                .replaceAll("[\\\\/:*?\"<>|]+", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("[. ]+$", "")
                .trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private boolean hasStoredPath(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.equalsIgnoreCase("N/A")
                && !trimmed.equalsIgnoreCase("null")
                && !trimmed.equals("-");
    }

    private static String display(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("N/A") || trimmed.equalsIgnoreCase("null")) {
            return "-";
        }
        return trimmed;
    }

    private static List<String> wrap(String text, double maxWidth, double fontSize, int maxLines) {
        List<String> lines = new ArrayList<>();
        String normalized = display(text).replaceAll("\\s+", " ");
        StringBuilder current = new StringBuilder();
        for (String word : normalized.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (textWidth(candidate, fontSize) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                appendWord(lines, current, word, maxWidth, fontSize);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("-");
        }
        return trimLines(lines, maxLines, maxWidth, fontSize);
    }

    private static void appendWord(List<String> lines, StringBuilder current, String word, double maxWidth, double fontSize) {
        if (textWidth(word, fontSize) <= maxWidth) {
            current.append(word);
            return;
        }

        StringBuilder part = new StringBuilder();
        for (int index = 0; index < word.length(); index++) {
            String candidate = part + String.valueOf(word.charAt(index));
            if (textWidth(candidate, fontSize) > maxWidth && part.length() > 0) {
                lines.add(part.toString());
                part.setLength(0);
            }
            part.append(word.charAt(index));
        }
        current.append(part);
    }

    private static List<String> trimLines(List<String> lines, int maxLines, double maxWidth, double fontSize) {
        if (lines.size() <= maxLines) {
            return lines;
        }

        List<String> trimmed = new ArrayList<>(lines.subList(0, maxLines));
        String last = trimmed.get(maxLines - 1);
        while (!last.isEmpty() && textWidth(last + "...", fontSize) > maxWidth) {
            last = last.substring(0, last.length() - 1);
        }
        trimmed.set(maxLines - 1, last + "...");
        return trimmed;
    }

    private static double textWidth(String text, double fontSize) {
        double width = 0;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == ' ') {
                width += fontSize * 0.28;
            } else if ("ilI.,:;!'|".indexOf(c) >= 0) {
                width += fontSize * 0.25;
            } else if ("MW@#%&".indexOf(c) >= 0) {
                width += fontSize * 0.78;
            } else if (Character.isUpperCase(c)) {
                width += fontSize * 0.6;
            } else {
                width += fontSize * 0.52;
            }
        }
        return width;
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String escapePdf(String value) {
        StringBuilder escaped = new StringBuilder();
        String clean = value == null ? "" : value;
        for (int index = 0; index < clean.length(); index++) {
            char c = clean.charAt(index);
            if (c == '\\' || c == '(' || c == ')') {
                escaped.append('\\').append(c);
            } else if (c >= 32 && c <= 255) {
                escaped.append(c);
            } else if (Character.isWhitespace(c)) {
                escaped.append(' ');
            } else {
                escaped.append('?');
            }
        }
        return escaped.toString();
    }

    public static final class PackageOptions {
        private final boolean includePdfProfile;
        private final boolean includeAllDocuments;
        private final boolean includeMergedDocumentsPdf;
        private final List<String> selectedDocumentLabels;

        public PackageOptions(
                boolean includePdfProfile,
                boolean includeAllDocuments,
                List<String> selectedDocumentLabels
        ) {
            this(includePdfProfile, includeAllDocuments, false, selectedDocumentLabels);
        }

        public PackageOptions(
                boolean includePdfProfile,
                boolean includeAllDocuments,
                boolean includeMergedDocumentsPdf,
                List<String> selectedDocumentLabels
        ) {
            this.includePdfProfile = includePdfProfile;
            this.includeAllDocuments = includeAllDocuments;
            this.includeMergedDocumentsPdf = includeMergedDocumentsPdf;
            this.selectedDocumentLabels = selectedDocumentLabels == null
                    ? List.of()
                    : List.copyOf(selectedDocumentLabels);
        }

        private static PackageOptions all() {
            return new PackageOptions(true, true, false, List.of());
        }

        public boolean includePdfProfile() {
            return includePdfProfile;
        }

        public boolean includeAllDocuments() {
            return includeAllDocuments;
        }

        public boolean includeMergedDocumentsPdf() {
            return includeMergedDocumentsPdf;
        }

        public List<String> selectedDocumentLabels() {
            return selectedDocumentLabels;
        }
    }

    public static final class AvailableDocument {
        private final String label;
        private final File sourceFile;
        private final boolean fileReady;
        private final boolean mergeableForDocumentsPdf;

        private AvailableDocument(String label, File sourceFile, boolean fileReady, boolean mergeableForDocumentsPdf) {
            this.label = label;
            this.sourceFile = sourceFile;
            this.fileReady = fileReady;
            this.mergeableForDocumentsPdf = mergeableForDocumentsPdf;
        }

        public String label() {
            return label;
        }

        public File sourceFile() {
            return sourceFile;
        }

        public boolean fileReady() {
            return fileReady;
        }

        public boolean mergeableForDocumentsPdf() {
            return mergeableForDocumentsPdf;
        }
    }

    public static final class PackageResult {
        private final File folder;
        private final File pdfFile;
        private final File mergedDocumentsPdfFile;
        private final int copiedDocumentCount;
        private final int totalDocumentCount;
        private final int mergedDocumentCount;

        private PackageResult(
                File folder,
                File pdfFile,
                File mergedDocumentsPdfFile,
                int copiedDocumentCount,
                int totalDocumentCount,
                int mergedDocumentCount
        ) {
            this.folder = folder;
            this.pdfFile = pdfFile;
            this.mergedDocumentsPdfFile = mergedDocumentsPdfFile;
            this.copiedDocumentCount = copiedDocumentCount;
            this.totalDocumentCount = totalDocumentCount;
            this.mergedDocumentCount = mergedDocumentCount;
        }

        public File folder() {
            return folder;
        }

        public File pdfFile() {
            return pdfFile;
        }

        public File mergedDocumentsPdfFile() {
            return mergedDocumentsPdfFile;
        }

        public int copiedDocumentCount() {
            return copiedDocumentCount;
        }

        public int totalDocumentCount() {
            return totalDocumentCount;
        }

        public int mergedDocumentCount() {
            return mergedDocumentCount;
        }
    }

    private static final class DocumentEntry {
        private final String label;
        private final String sourcePath;
        private final boolean employeePhoto;

        private DocumentEntry(String label, String sourcePath) {
            this(label, sourcePath, false);
        }

        private DocumentEntry(String label, String sourcePath, boolean employeePhoto) {
            this.label = label;
            this.sourcePath = sourcePath;
            this.employeePhoto = employeePhoto;
        }

        private String label() {
            return label;
        }

        private String sourcePath() {
            return sourcePath;
        }

        private boolean employeePhoto() {
            return employeePhoto;
        }
    }

    private static final class PackagedDocument {
        private final String label;
        private final String sourcePath;
        private final String packagedFileName;
        private final String status;

        private PackagedDocument(String label, String sourcePath, String packagedFileName, String status) {
            this.label = label;
            this.sourcePath = sourcePath;
            this.packagedFileName = packagedFileName;
            this.status = status;
        }

        private String label() {
            return label;
        }

        private String sourcePath() {
            return sourcePath;
        }

        private String packagedFileName() {
            return packagedFileName;
        }

        private String status() {
            return status;
        }
    }

    private static final class MergedDocumentsPdfResult {
        private final File file;
        private final int documentCount;

        private MergedDocumentsPdfResult(File file, int documentCount) {
            this.file = file;
            this.documentCount = documentCount;
        }

        private File file() {
            return file;
        }

        private int documentCount() {
            return documentCount;
        }
    }

    private static final class DocumentImage {
        private final String label;
        private final Path source;
        private final int width;
        private final int height;
        private final byte[] bytes;

        private DocumentImage(String label, Path source, int width, int height, byte[] bytes) {
            this.label = label;
            this.source = source;
            this.width = width;
            this.height = height;
            this.bytes = bytes;
        }

        private String label() {
            return label;
        }

        private Path source() {
            return source;
        }

        private int width() {
            return width;
        }

        private int height() {
            return height;
        }

        private byte[] bytes() {
            return bytes;
        }
    }

    private static final class ImagePdfPage {
        private final int imageIndex;
        private final double imageX;
        private final double imageY;
        private final double imageWidth;
        private final double imageHeight;

        private ImagePdfPage(
                int imageIndex,
                double imageX,
                double imageY,
                double imageWidth,
                double imageHeight
        ) {
            this.imageIndex = imageIndex;
            this.imageX = imageX;
            this.imageY = imageY;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }

    private static final class DocumentImagePdf {
        private static final double SIDE_MARGIN = 36;
        private static final double TOP_MARGIN = 36;
        private static final double BOTTOM_MARGIN = 36;
        private static final double MAX_BODY_WIDTH = PAGE_WIDTH - (SIDE_MARGIN * 2);
        private static final double MAX_BODY_HEIGHT = PAGE_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN;

        private final List<DocumentImage> images;
        private final List<ImagePdfPage> pages = new ArrayList<>();

        private DocumentImagePdf(List<DocumentImage> images) {
            this.images = images;
            buildPages();
        }

        private void buildPages() {
            for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
                DocumentImage image = images.get(imageIndex);
                double scale = Math.min(1.0, Math.min(
                        MAX_BODY_WIDTH / image.width(),
                        MAX_BODY_HEIGHT / image.height()
                ));
                double imageWidth = image.width() * scale;
                double imageHeight = image.height() * scale;
                double imageX = (PAGE_WIDTH - imageWidth) / 2;
                double imageY = PAGE_HEIGHT - TOP_MARGIN - imageHeight;
                pages.add(new ImagePdfPage(imageIndex, imageX, imageY, imageWidth, imageHeight));
            }
        }

        private void write(OutputStream output) throws IOException {
            List<byte[]> objects = new ArrayList<>();
            int imageCount = images.size();
            int pageCount = pages.size();
            int firstImageObject = 3;
            int firstPageObject = firstImageObject + imageCount;
            int firstContentObject = firstPageObject + pageCount;

            objects.add(pdfObject("<< /Type /Catalog /Pages 2 0 R >>"));
            objects.add(pdfObject(pagesObject(pageCount, firstPageObject)));

            for (DocumentImage image : images) {
                objects.add(imageObject(image));
            }

            for (int index = 0; index < pageCount; index++) {
                ImagePdfPage page = pages.get(index);
                int contentObject = firstContentObject + index;
                int imageObject = firstImageObject + page.imageIndex;
                String imageName = "Im" + (page.imageIndex + 1);
                objects.add(pdfObject("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                        + fmt(PAGE_WIDTH) + " " + fmt(PAGE_HEIGHT)
                        + "] /Resources << /XObject << /" + imageName + " "
                        + imageObject + " 0 R >> >> /Contents " + contentObject + " 0 R >>"));
            }

            for (int index = 0; index < pageCount; index++) {
                byte[] stream = contentStream(pages.get(index))
                        .getBytes(StandardCharsets.ISO_8859_1);
                ByteArrayOutputStream object = new ByteArrayOutputStream();
                object.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
                object.write(stream);
                object.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
                objects.add(object.toByteArray());
            }

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write("%PDF-1.4\n%KGM\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < objects.size(); index++) {
                offsets.add(pdf.size());
                pdf.write((index + 1 + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                pdf.write(objects.get(index));
                pdf.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }

            int xref = pdf.size();
            pdf.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) {
                pdf.write(String.format(Locale.US, "%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }
            pdf.write(("trailer\n<< /Size " + (objects.size() + 1)
                    + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
            output.write(pdf.toByteArray());
        }

        private String contentStream(ImagePdfPage page) {
            String imageName = "Im" + (page.imageIndex + 1);

            StringBuilder content = new StringBuilder();
            content.append("q\n")
                    .append(fmt(page.imageWidth)).append(" 0 0 ").append(fmt(page.imageHeight)).append(' ')
                    .append(fmt(page.imageX)).append(' ').append(fmt(page.imageY)).append(" cm\n")
                    .append('/').append(imageName).append(" Do\n")
                    .append("Q\n");
            return content.toString();
        }

        private byte[] imageObject(DocumentImage image) throws IOException {
            ByteArrayOutputStream object = new ByteArrayOutputStream();
            object.write(("<< /Type /XObject /Subtype /Image /Width " + image.width()
                    + " /Height " + image.height()
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                    + image.bytes().length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            object.write(image.bytes());
            object.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            return object.toByteArray();
        }

        private String pagesObject(int pageCount, int firstPageObject) {
            StringBuilder kids = new StringBuilder();
            for (int index = 0; index < pageCount; index++) {
                if (index > 0) {
                    kids.append(' ');
                }
                kids.append(firstPageObject + index).append(" 0 R");
            }
            return "<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>";
        }

        private byte[] pdfObject(String value) {
            return value.getBytes(StandardCharsets.ISO_8859_1);
        }

    }

    private static class ReportWriter {
        private final PdfDocument document;
        private final Employee employee;
        private PdfCanvas page;
        private double y;

        private ReportWriter(PdfDocument document, Employee employee) {
            this.document = document;
            this.employee = employee;
            this.page = document.addPage();
            this.y = drawHeader(page);
        }

        private double drawHeader(PdfCanvas canvas) {
            double top = PAGE_HEIGHT - MARGIN;
            canvas.text("Kohinoor Textile Mills. Gujar Khan", MARGIN, top, 18, true, TEXT_PRIMARY);
            canvas.text("Employee Document Package", MARGIN, top - 20, 11, false, TEXT_SECONDARY);
            canvas.text("Generated: " + LocalDateTime.now().format(DATE_TIME), MARGIN, top - 36, 8.5, false, TEXT_SECONDARY);

            String employeeCode = "Employee Code: " + display(employee.getEMPLOYEE_CODE());
            canvas.textRight(employeeCode, PAGE_WIDTH - MARGIN, top - 2, 9, true, TEXT_PRIMARY);
            canvas.textRight("Internal HR record", PAGE_WIDTH - MARGIN, top - 18, 8.5, false, TEXT_SECONDARY);
            canvas.line(MARGIN, top - 52, PAGE_WIDTH - MARGIN, top - 52, BORDER);
            return top - 76;
        }

        private double drawContinuationHeader(PdfCanvas canvas) {
            double top = PAGE_HEIGHT - MARGIN;
            canvas.text("Employee Document Package", MARGIN, top, 13, true, TEXT_PRIMARY);
            canvas.text(display(employee.getEMP_NAME()) + " | Code: " + display(employee.getEMPLOYEE_CODE()),
                    MARGIN, top - 17, 8.5, false, TEXT_SECONDARY);
            canvas.line(MARGIN, top - 32, PAGE_WIDTH - MARGIN, top - 32, BORDER);
            return top - 54;
        }

        private void drawEmployeeSummary() {
            double height = 62;
            ensureSpace(height + 16);
            page.fillStrokeRect(MARGIN, y - height, CONTENT_WIDTH, height, HEADER_FILL, BORDER);
            page.text(display(employee.getEMP_NAME()), MARGIN + 14, y - 23, 16, true, TEXT_PRIMARY);
            page.text("Code: " + display(employee.getEMPLOYEE_CODE())
                            + " | Department: " + display(employee.getDEPARTMENT())
                            + " | Designation: " + display(employee.getDESIGNATION()),
                    MARGIN + 14, y - 42, 8.5, false, TEXT_SECONDARY);
            y -= height + 18;
        }

        private void drawSection(String title) {
            ensureSpace(36);
            page.fillStrokeRect(MARGIN, y - 24, CONTENT_WIDTH, 24, HEADER_FILL, BORDER);
            page.text(title, MARGIN + 10, y - 16, 9.5, true, TEXT_PRIMARY);
            y -= 32;
        }

        private void drawRows(String[][] rows) {
            for (String[] row : rows) {
                drawFieldRow(row[0], row[1]);
            }
            y -= 8;
        }

        private void drawFieldRow(String label, String value) {
            List<String> lines = wrap(value, CONTENT_WIDTH - LABEL_WIDTH - 22, 8.5, 4);
            double height = Math.max(24, 12 + (lines.size() * 10.5));
            ensureSpace(height);

            page.fillStrokeRect(MARGIN, y - height, LABEL_WIDTH, height, ROW_ALT, BORDER);
            page.fillStrokeRect(MARGIN + LABEL_WIDTH, y - height, CONTENT_WIDTH - LABEL_WIDTH, height, "FFFFFF", BORDER);
            page.text(label, MARGIN + 8, y - 15, 8.3, true, TEXT_SECONDARY);

            double baseline = y - 15;
            for (String line : lines) {
                page.text(line, MARGIN + LABEL_WIDTH + 10, baseline, 8.5, false, TEXT_PRIMARY);
                baseline -= 10.5;
            }
            y -= height;
        }

        private void drawDocumentTable(List<PackagedDocument> documents) {
            ensureSpace(48);
            drawDocumentHeader();

            if (documents.isEmpty()) {
                drawDocumentRow(new String[]{"No documents", "-", "-"}, 0);
                return;
            }

            for (int index = 0; index < documents.size(); index++) {
                PackagedDocument documentRow = documents.get(index);
                String status = documentRow.status();
                String detail = "Copied".equals(status)
                        ? documentRow.packagedFileName()
                        : "Missing file".equals(status) ? documentRow.sourcePath() : "-";
                drawDocumentRow(new String[]{documentRow.label(), status, detail}, index);
            }
            y -= 8;
        }

        private void drawDocumentHeader() {
            double height = 22;
            double x = MARGIN;
            double[] widths = {170, 90, CONTENT_WIDTH - 260};
            String[] columns = {"Document", "Status", "Package File / Source"};
            for (int index = 0; index < columns.length; index++) {
                page.fillStrokeRect(x, y - height, widths[index], height, PRIMARY, PRIMARY);
                page.text(columns[index], x + 6, y - 14, 8, true, "FFFFFF");
                x += widths[index];
            }
            y -= height;
        }

        private void drawDocumentRow(String[] values, int rowIndex) {
            double[] widths = {170, 90, CONTENT_WIDTH - 260};
            int maxLines = 1;
            for (int index = 0; index < values.length; index++) {
                maxLines = Math.max(maxLines, wrap(values[index], widths[index] - 12, 8, 3).size());
            }
            double height = Math.max(24, 12 + (maxLines * 10));
            if (!hasSpace(height)) {
                newPage();
                drawDocumentHeader();
            }

            double x = MARGIN;
            String fill = rowIndex % 2 == 0 ? "FFFFFF" : ROW_ALT;
            for (int column = 0; column < values.length; column++) {
                page.fillStrokeRect(x, y - height, widths[column], height, fill, BORDER);
                List<String> lines = wrap(values[column], widths[column] - 12, 8, 3);
                double baseline = y - 14;
                String color = column == 1 && "Copied".equals(values[column]) ? SUCCESS
                        : column == 1 && "Missing file".equals(values[column]) ? WARNING : TEXT_PRIMARY;
                for (String line : lines) {
                    page.text(line, x + 6, baseline, 8, column == 1, color);
                    baseline -= 10;
                }
                x += widths[column];
            }
            y -= height;
        }

        private void ensureSpace(double height) {
            if (!hasSpace(height)) {
                newPage();
            }
        }

        private boolean hasSpace(double height) {
            return y - height >= BODY_BOTTOM;
        }

        private void newPage() {
            page = document.addPage();
            y = drawContinuationHeader(page);
        }
    }

    private static class PdfDocument {
        private final List<PdfCanvas> pages = new ArrayList<>();

        private PdfCanvas addPage() {
            PdfCanvas canvas = new PdfCanvas();
            pages.add(canvas);
            return canvas;
        }

        private void addFooters() {
            int pageCount = pages.size();
            for (int index = 0; index < pageCount; index++) {
                PdfCanvas canvas = pages.get(index);
                String pageNumber = "Page " + (index + 1) + " of " + pageCount;
                canvas.line(MARGIN, 52, PAGE_WIDTH - MARGIN, 52, BORDER);
                canvas.text("KGM Ex-Employee Management System", MARGIN, 35, 8, false, TEXT_SECONDARY);
                canvas.textRight(pageNumber, PAGE_WIDTH - MARGIN, 35, 8, false, TEXT_SECONDARY);
            }
        }

        private void write(OutputStream output) throws IOException {
            List<byte[]> objects = new ArrayList<>();
            int pageCount = pages.size();
            int firstPageObject = 5;
            int firstContentObject = firstPageObject + pageCount;

            objects.add(pdfObject("<< /Type /Catalog /Pages 2 0 R >>"));
            objects.add(pdfObject(pagesObject(pageCount, firstPageObject)));
            objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
            objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));

            for (int index = 0; index < pageCount; index++) {
                int contentObject = firstContentObject + index;
                objects.add(pdfObject("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                        + fmt(PAGE_WIDTH) + " " + fmt(PAGE_HEIGHT)
                        + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                        + contentObject + " 0 R >>"));
            }

            for (PdfCanvas page : pages) {
                byte[] stream = page.content().getBytes(StandardCharsets.ISO_8859_1);
                String prefix = "<< /Length " + stream.length + " >>\nstream\n";
                String suffix = "\nendstream";
                ByteArrayOutputStream object = new ByteArrayOutputStream();
                object.write(prefix.getBytes(StandardCharsets.ISO_8859_1));
                object.write(stream);
                object.write(suffix.getBytes(StandardCharsets.ISO_8859_1));
                objects.add(object.toByteArray());
            }

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.write("%PDF-1.4\n%KGM\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            for (int index = 0; index < objects.size(); index++) {
                offsets.add(pdf.size());
                pdf.write((index + 1 + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                pdf.write(objects.get(index));
                pdf.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }

            int xref = pdf.size();
            pdf.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets) {
                pdf.write(String.format(Locale.US, "%010d 00000 n \n", offset).getBytes(StandardCharsets.ISO_8859_1));
            }
            pdf.write(("trailer\n<< /Size " + (objects.size() + 1)
                    + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
            output.write(pdf.toByteArray());
        }

        private String pagesObject(int pageCount, int firstPageObject) {
            StringBuilder kids = new StringBuilder();
            for (int index = 0; index < pageCount; index++) {
                if (index > 0) {
                    kids.append(' ');
                }
                kids.append(firstPageObject + index).append(" 0 R");
            }
            return "<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>";
        }

        private byte[] pdfObject(String value) {
            return value.getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    private static class PdfCanvas {
        private final StringBuilder content = new StringBuilder();

        private void fillStrokeRect(double x, double y, double width, double height, String fill, String stroke) {
            color(fill, "rg");
            color(stroke, "RG");
            content.append("0.6 w\n")
                    .append(fmt(x)).append(' ').append(fmt(y)).append(' ')
                    .append(fmt(width)).append(' ').append(fmt(height)).append(" re B\n");
        }

        private void line(double x1, double y1, double x2, double y2, String stroke) {
            color(stroke, "RG");
            content.append("0.8 w\n")
                    .append(fmt(x1)).append(' ').append(fmt(y1)).append(" m ")
                    .append(fmt(x2)).append(' ').append(fmt(y2)).append(" l S\n");
        }

        private void text(String text, double x, double y, double size, boolean bold, String fill) {
            color(fill, "rg");
            content.append("BT /").append(bold ? "F2" : "F1").append(' ')
                    .append(fmt(size)).append(" Tf 1 0 0 1 ")
                    .append(fmt(x)).append(' ').append(fmt(y)).append(" Tm (")
                    .append(escapePdf(text)).append(") Tj ET\n");
        }

        private void textRight(String text, double rightX, double y, double size, boolean bold, String fill) {
            text(text, rightX - textWidth(display(text), size), y, size, bold, fill);
        }

        private String content() {
            return content.toString();
        }

        private void color(String hex, String operator) {
            int red = Integer.parseInt(hex.substring(0, 2), 16);
            int green = Integer.parseInt(hex.substring(2, 4), 16);
            int blue = Integer.parseInt(hex.substring(4, 6), 16);
            content.append(fmt(red / 255.0)).append(' ')
                    .append(fmt(green / 255.0)).append(' ')
                    .append(fmt(blue / 255.0)).append(' ')
                    .append(operator).append('\n');
        }

        private String escapePdf(String value) {
            StringBuilder escaped = new StringBuilder();
            String clean = value == null ? "" : value;
            for (int index = 0; index < clean.length(); index++) {
                char c = clean.charAt(index);
                if (c == '\\' || c == '(' || c == ')') {
                    escaped.append('\\').append(c);
                } else if (c >= 32 && c <= 255) {
                    escaped.append(c);
                } else if (Character.isWhitespace(c)) {
                    escaped.append(' ');
                } else {
                    escaped.append('?');
                }
            }
            return escaped.toString();
        }
    }
}
