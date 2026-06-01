package com.kgm.ui.component;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

public final class NativeFileDialog {
    private NativeFileDialog() {
    }

    public static File chooseFile(Component parent, String title, FileUploadCard.FileFilterSpec filter) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.LOAD);
        applyFilter(dialog, filter);
        dialog.setVisible(true);
        return selectedFile(dialog);
    }

    public static File[] chooseFiles(Component parent, String title, FileUploadCard.FileFilterSpec filter) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.LOAD);
        dialog.setMultipleMode(true);
        applyFilter(dialog, filter);
        dialog.setVisible(true);
        File[] files = dialog.getFiles();
        return files == null ? new File[0] : files;
    }

    public static File[] chooseDirectories(Component parent, String title) {
        return WindowsNativeFolderDialog.chooseFolders(parent, title);
    }

    public static File chooseSaveFile(
            Component parent,
            String title,
            String suggestedFileName,
            FileUploadCard.FileFilterSpec filter
    ) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.SAVE);
        if (suggestedFileName != null && !suggestedFileName.isBlank()) {
            dialog.setFile(suggestedFileName);
        }
        applyFilter(dialog, filter);
        dialog.setVisible(true);
        return selectedFile(dialog);
    }

    public static File chooseDirectory(Component parent, String title, String suggestedDirectoryName) {
        FileDialog dialog = nativeFileDialog(parent, title, FileDialog.SAVE);
        dialog.setFile(cleanSuggestedName(suggestedDirectoryName));
        dialog.setVisible(true);
        return selectedFile(dialog);
    }

    private static FileDialog nativeFileDialog(Component parent, String title, int mode) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner instanceof Frame frame) {
            return new FileDialog(frame, title, mode);
        }
        if (owner instanceof Dialog dialog) {
            return new FileDialog(dialog, title, mode);
        }
        return new FileDialog((Frame) null, title, mode);
    }

    private static void applyFilter(FileDialog dialog, FileUploadCard.FileFilterSpec filter) {
        if (filter == null || filter.extensions().length == 0) {
            return;
        }
        FilenameFilter filenameFilter = (directory, name) -> hasAllowedExtension(name, filter.extensions());
        dialog.setFilenameFilter(filenameFilter);
        if (dialog.getMode() == FileDialog.LOAD) {
            dialog.setFile(glob(filter.extensions()));
        }
    }

    private static File selectedFile(FileDialog dialog) {
        String fileName = dialog.getFile();
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String directory = dialog.getDirectory();
        return directory == null ? new File(fileName) : new File(directory, fileName);
    }

    private static boolean hasAllowedExtension(String fileName, String[] extensions) {
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (normalized.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    private static String glob(String[] extensions) {
        StringBuilder pattern = new StringBuilder();
        for (String extension : extensions) {
            if (pattern.length() > 0) {
                pattern.append(';');
            }
            pattern.append("*.").append(extension);
        }
        return pattern.toString();
    }

    private static String cleanSuggestedName(String suggestedName) {
        String clean = suggestedName == null ? "" : suggestedName.trim();
        if (clean.isEmpty()) {
            return "Employee Report Package";
        }
        return clean.replaceAll("[\\\\/:*?\"<>|]+", " ").replaceAll("\\s+", " ").trim();
    }
}
