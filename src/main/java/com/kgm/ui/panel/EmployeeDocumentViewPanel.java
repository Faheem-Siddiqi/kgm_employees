package com.kgm.ui.panel;

import com.kgm.model.Employee;
import com.kgm.ui.component.FileUploadCard;
import com.kgm.ui.component.LoadingOverlay;
import com.kgm.ui.styling.DialogHelper;
import com.kgm.ui.styling.EmployeeDocumentViewPanelHelper;
import com.kgm.ui.styling.TablePaginationHelper;
import com.kgm.ui.styling.UniversalDialogHelper;
import com.kgm.util.EmployeeDocumentUtil;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class EmployeeDocumentViewPanel extends JPanel {
    private static final int ACTION_COLUMN = 3;
    private static final int DOCUMENT_INDEX_COLUMN = 4;

    private JTable table;
    private DefaultTableModel model;
    private File[] files;
    private String[] filePaths;
    private String[] displayFileNames;
    private boolean[] temporaryUploadFiles;
    private boolean[] lockedDocuments;
    private boolean[] activeDocumentViews;
    private JLabel uploadedCountLabel;
    private JTextField searchField;
    private JButton clearSearchButton;
    private JScrollPane documentScrollPane;
    private Runnable pendingChangesListener;
    private Consumer<File> profileImageUploadListener;

    public EmployeeDocumentViewPanel() {
        this(null);
    }

    public EmployeeDocumentViewPanel(Employee employee) {
        EmployeeDocumentViewPanelHelper.stylePanel(this);

        files = new File[EmployeeDocumentUtil.documentCount()];
        filePaths = new String[EmployeeDocumentUtil.documentCount()];
        displayFileNames = new String[EmployeeDocumentUtil.documentCount()];
        temporaryUploadFiles = new boolean[EmployeeDocumentUtil.documentCount()];
        lockedDocuments = new boolean[EmployeeDocumentUtil.documentCount()];
        activeDocumentViews = new boolean[EmployeeDocumentUtil.documentCount()];

        JPanel topPanel = EmployeeDocumentViewPanelHelper.createTopPanel();
        uploadedCountLabel = EmployeeDocumentViewPanelHelper.createUploadedCountLabel(
                "Total uploads: 0 / " + EmployeeDocumentUtil.documentCount()
        );
        JLabel sizeLabel = EmployeeDocumentViewPanelHelper.createSizeLabel();
        searchField = new PlaceholderTextField("Search Document Name");
        clearSearchButton = new JButton("Clear");
        JButton uploadAllButton = new JButton("Upload All");
        uploadAllButton.setToolTipText("Select multiple JPG or JPEG documents");
        searchField.setToolTipText("Search document name or saved file");

        EmployeeDocumentViewPanelHelper.styleSearchField(searchField);
        EmployeeDocumentViewPanelHelper.styleClearButton(clearSearchButton);
        EmployeeDocumentViewPanelHelper.styleTextCtaButton(uploadAllButton);

        uploadAllButton.addActionListener(e -> chooseMultipleFiles());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            searchField.requestFocusInWindow();
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                updateSearch();
            }

            public void removeUpdate(DocumentEvent event) {
                updateSearch();
            }

            public void changedUpdate(DocumentEvent event) {
                updateSearch();
            }

            private void updateSearch() {
                EmployeeDocumentViewPanelHelper.updateClearButtonState(
                        clearSearchButton,
                        !searchField.getText().trim().isEmpty()
                );
                refreshDocumentRows();
            }
        });

        topPanel.add(EmployeeDocumentViewPanelHelper.createSummaryPanel(
                uploadedCountLabel,
                sizeLabel,
                null
        ));
        topPanel.add(Box.createVerticalStrut(12));
        topPanel.add(EmployeeDocumentViewPanelHelper.createSearchPanel(searchField, clearSearchButton, uploadAllButton));
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Document", "File", "Status", "Actions", "DocumentIndex"};
        model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == ACTION_COLUMN;
            }
        };

        loadDocumentState(employee);
        refreshDocumentRows();

        table = TablePaginationHelper.createDocumentTable(model);
        hideDocumentIndexColumn();
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(ACTION_COLUMN).setCellEditor(new ActionEditor());

        documentScrollPane = TablePaginationHelper.createScrollPane(table, false);
        showFullTableWithoutScroll(documentScrollPane);
        installDocumentTableWheelForwarding();
        add(documentScrollPane, BorderLayout.CENTER);
    }

    private void loadDocumentState(Employee employee) {
        for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
            String path = EmployeeDocumentUtil.documentPath(employee, index);
            if (EmployeeDocumentUtil.hasStoredPath(path)) {
                filePaths[index] = path;
                lockedDocuments[index] = true;
                File resolved = EmployeeDocumentUtil.resolveStoredFile(path);
                if (resolved.exists()) {
                    files[index] = resolved;
                }
            }
        }
        updateCount();
    }

    private void refreshDocumentRows() {
        if (model == null) {
            return;
        }
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        String query = EmployeeDocumentUtil.normalizedSearch(searchField == null ? "" : searchField.getText());
        List<Integer> orderedIndexes = new ArrayList<>();
        for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
            orderedIndexes.add(index);
        }

        if (!query.isEmpty()) {
            orderedIndexes.removeIf(index -> matchScore(index, query) == 2);
            orderedIndexes.sort((left, right) -> {
                int score = Integer.compare(matchScore(left, query), matchScore(right, query));
                return score != 0 ? score : Integer.compare(left, right);
            });
        }

        model.setRowCount(0);
        for (Integer documentIndex : orderedIndexes) {
            addDocumentRow(documentIndex);
        }

        if (documentScrollPane != null) {
            showFullTableWithoutScroll(documentScrollPane);
            revalidate();
            repaint();
        }
    }

    private void addDocumentRow(int documentIndex) {
        String documentLabel = EmployeeDocumentUtil.documentType(documentIndex).label();
        if (lockedDocuments[documentIndex]) {
            File resolved = files[documentIndex] != null
                    ? files[documentIndex]
                    : EmployeeDocumentUtil.resolveStoredFile(filePaths[documentIndex]);
            if (resolved.exists()) {
                model.addRow(new Object[]{documentLabel, resolved.getName(), "Uploaded", "View", documentIndex});
            } else {
                model.addRow(new Object[]{documentLabel, EmployeeDocumentUtil.fileNameFromPath(filePaths[documentIndex]), "Saved Path Missing", "Locked", documentIndex});
            }
            return;
        }

        File file = files[documentIndex];
        if (file == null) {
            model.addRow(new Object[]{documentLabel, "-", "Not Uploaded", "Upload", documentIndex});
        } else {
            model.addRow(new Object[]{documentLabel, selectedFileName(documentIndex), "Ready to Save (" + EmployeeDocumentUtil.formatSize(file.length()) + ")", "View", documentIndex});
        }
    }

    private void hideDocumentIndexColumn() {
        TableColumn hiddenColumn = table.getColumnModel().getColumn(DOCUMENT_INDEX_COLUMN);
        table.getColumnModel().removeColumn(hiddenColumn);
    }

    private int documentIndexForModelRow(int modelRow) {
        Object value = model.getValueAt(modelRow, DOCUMENT_INDEX_COLUMN);
        return value instanceof Integer ? (Integer) value : Integer.parseInt(String.valueOf(value));
    }

    private int findModelRowByDocumentIndex(int documentIndex) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (documentIndexForModelRow(row) == documentIndex) {
                return row;
            }
        }
        return -1;
    }

    private int matchScore(int documentIndex, String query) {
        List<String> searchableValues = new ArrayList<>();
        searchableValues.add(EmployeeDocumentUtil.documentType(documentIndex).label());
        searchableValues.add(EmployeeDocumentUtil.documentType(documentIndex).employeeFieldName());
        searchableValues.add(EmployeeDocumentUtil.documentType(documentIndex).storageName());
        String path = filePaths[documentIndex];
        if (EmployeeDocumentUtil.hasStoredPath(path)) {
            searchableValues.add(EmployeeDocumentUtil.fileNameFromPath(path));
        }
        if (displayFileNames[documentIndex] != null) {
            searchableValues.add(displayFileNames[documentIndex]);
        }

        int bestScore = 2;
        for (String value : searchableValues) {
            String documentName = EmployeeDocumentUtil.normalizedSearch(value);
            if (documentName.startsWith(query)) {
                return 0;
            }
            if (documentName.contains(query)) {
                bestScore = 1;
            }
        }
        return bestScore;
    }

    private void showFullTableWithoutScroll(JScrollPane scrollPane) {
        if (table == null || scrollPane == null) {
            return;
        }
        TablePaginationHelper.autoResizeColumns(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        table.setFillsViewportHeight(false);

        int headerHeight = table.getTableHeader() == null ? 0 : table.getTableHeader().getPreferredSize().height;
        int tableHeight = headerHeight + (table.getRowHeight() * table.getRowCount()) + 2;
        int tableWidth = Math.max(320, table.getPreferredSize().width + 2);
        scrollPane.setPreferredSize(new Dimension(tableWidth, tableHeight));
        scrollPane.setMinimumSize(new Dimension(320, tableHeight));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, tableHeight));
    }

    private void installDocumentTableWheelForwarding() {
        MouseWheelListener listener = this::forwardMouseWheelToPageScroll;
        table.addMouseWheelListener(listener);
        if (table.getTableHeader() != null) {
            table.getTableHeader().addMouseWheelListener(listener);
        }
        documentScrollPane.addMouseWheelListener(listener);
        documentScrollPane.getViewport().addMouseWheelListener(listener);
    }

    private void forwardMouseWheelToPageScroll(MouseWheelEvent event) {
        if (event.isConsumed()) {
            return;
        }
        JScrollPane pageScroll = findOuterScrollPane();
        if (pageScroll == null) {
            return;
        }

        JScrollBar vertical = pageScroll.getVerticalScrollBar();
        if (vertical == null || !vertical.isVisible()) {
            return;
        }

        int direction = event.getWheelRotation() < 0 ? -1 : 1;
        int amount = event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                ? event.getWheelRotation() * vertical.getBlockIncrement(direction)
                : event.getUnitsToScroll() * vertical.getUnitIncrement(direction);

        int maxValue = vertical.getMaximum() - vertical.getVisibleAmount();
        int nextValue = Math.max(vertical.getMinimum(), Math.min(maxValue, vertical.getValue() + amount));
        if (nextValue != vertical.getValue()) {
            vertical.setValue(nextValue);
            event.consume();
        }
    }

    private JScrollPane findOuterScrollPane() {
        Component current = documentScrollPane == null ? null : documentScrollPane.getParent();
        while (current != null) {
            if (current instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
            current = current.getParent();
        }
        return null;
    }

    private void updateCount() {
        int count = 0;
        for (String path : filePaths) {
            if (EmployeeDocumentUtil.hasStoredPath(path)) {
                count++;
            }
        }
        if (uploadedCountLabel != null) {
            uploadedCountLabel.setText("Total uploads: " + count + " / " + EmployeeDocumentUtil.documentCount());
        }
    }

    private void chooseFile(int documentIndex) {
        if (lockedDocuments[documentIndex]) {
            DialogHelper.warning(
                    this,
                    "Document Locked",
                    "Since " + EmployeeDocumentUtil.cleanDocumentLabel(documentIndex) + " already exists in DB, it cannot be replaced."
            );
            return;
        }

        File file = FileUploadCard.chooseFile(
                this,
                "Upload " + EmployeeDocumentUtil.cleanDocumentLabel(documentIndex),
                FileUploadCard.jpegImages()
        );
        if (file == null) {
            return;
        }

        prepareSingleFile(documentIndex, file);
    }

    private void prepareSingleFile(int documentIndex, File selectedFile) {
        if (!EmployeeDocumentUtil.shouldCompressBeforeUpload(selectedFile)) {
            applySingleFile(documentIndex, EmployeeDocumentUtil.prepareImageForUpload(selectedFile));
            return;
        }
        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Preparing Upload",
                "Compressing JPG/JPEG image to fit the upload limit..."
        );
        SwingWorker<EmployeeDocumentUtil.PreparedUploadFile, Void> worker = new SwingWorker<>() {
            @Override
            protected EmployeeDocumentUtil.PreparedUploadFile doInBackground() {
                return EmployeeDocumentUtil.prepareImageForUpload(selectedFile);
            }

            @Override
            protected void done() {
                loader.close();
                try {
                    applySingleFile(documentIndex, get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.warning(EmployeeDocumentViewPanel.this, "Upload Stopped", "Image preparation was interrupted.");
                } catch (ExecutionException exception) {
                    DialogHelper.warning(EmployeeDocumentViewPanel.this, "Cannot Upload File", "The selected image could not be prepared.");
                }
            }
        };
        worker.execute();
    }

    private void applySingleFile(int documentIndex, EmployeeDocumentUtil.PreparedUploadFile prepared) {
        if (!prepared.ready()) {
            DialogHelper.warning(this, "Cannot Upload File", prepared.message());
            return;
        }

        File file = prepared.file();
        discardTemporaryUpload(documentIndex, file);
        files[documentIndex] = file;
        filePaths[documentIndex] = file.getAbsolutePath();
        displayFileNames[documentIndex] = prepared.originalFile().getName();
        temporaryUploadFiles[documentIndex] = prepared.compressed();
        notifyProfileImageUpload(documentIndex, file);
        int modelRow = findModelRowByDocumentIndex(documentIndex);
        if (modelRow >= 0) {
            model.setValueAt(prepared.originalFile().getName(), modelRow, 1);
            model.setValueAt("Ready to Save (" + EmployeeDocumentUtil.formatSize(file.length()) + ")", modelRow, 2);
        }
        updateCount();
        model.fireTableDataChanged();
        notifyPendingChanges();
    }

    private void chooseMultipleFiles() {
        File[] selectedFiles = FileUploadCard.chooseFiles(
                this,
                "Upload Employee Documents",
                FileUploadCard.jpegImages()
        );
        if (selectedFiles.length == 0) {
            return;
        }

        LoadingOverlay.Handle loader = LoadingOverlay.show(
                this,
                "Preparing Upload All",
                "Checking selected JPG/JPEG documents..."
        );
        SwingWorker<EmployeeDocumentUtil.BulkUploadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected EmployeeDocumentUtil.BulkUploadResult doInBackground() {
                return EmployeeDocumentUtil.matchBulkFiles(
                        selectedFiles,
                        lockedDocuments,
                        (message, completedFiles, totalFiles, percent) -> SwingUtilities.invokeLater(() -> {
                            loader.setMessage(message);
                            loader.setProgress(percent);
                        })
                );
            }

            @Override
            protected void done() {
                loader.close();
                try {
                    applyBulkUploadSummary(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    DialogHelper.warning(EmployeeDocumentViewPanel.this, "Upload Stopped", "Upload preparation was interrupted.");
                } catch (ExecutionException exception) {
                    DialogHelper.warning(EmployeeDocumentViewPanel.this, "Cannot Prepare Uploads", "Selected files could not be prepared.");
                }
            }
        };
        worker.execute();
    }

    private void applyBulkUploadSummary(EmployeeDocumentUtil.BulkUploadResult summary) {
        for (EmployeeDocumentUtil.BulkUploadItem item : summary.uploadedDocuments()) {
            discardTemporaryUpload(item.documentIndex(), item.file());
            files[item.documentIndex()] = item.file();
            filePaths[item.documentIndex()] = item.file().getAbsolutePath();
            displayFileNames[item.documentIndex()] = item.originalFile().getName();
            temporaryUploadFiles[item.documentIndex()] = item.compressed();
            notifyProfileImageUpload(item.documentIndex(), item.file());
        }

        refreshDocumentRows();
        updateCount();
        model.fireTableDataChanged();
        notifyPendingChanges();
        showBulkUploadSummary(summary);
    }

    private void showBulkUploadSummary(EmployeeDocumentUtil.BulkUploadResult summary) {
        String uploadedText = summary.uploadedCount() == 1
                ? "1 document is ready to save with this employee."
                : summary.uploadedCount() + " documents are ready to save with this employee.";
        String discardedText = summary.discardedCount() == 1
                ? "1 selected file could not be matched to a document slot."
                : summary.discardedCount() + " selected files could not be matched to document slots.";

        if (summary.discardedCount() == 0) {
            DialogHelper.success(this, "Documents ready\n" + uploadedText + "\nSave the employee record to keep these uploads.");
            return;
        }

        DialogHelper.warningSections(
                this,
                "Review unmatched files",
                "Ready to save\n" + uploadedText + "\nSave the employee record to keep these uploads.",
                "Files that need attention\n" + discardedText + "\n" + summary.discardedDetails()
        );
    }

    private void viewFile(int documentIndex) {
        if (activeDocumentViews[documentIndex]) {
            return;
        }

        File file = files[documentIndex];
        if (file == null && EmployeeDocumentUtil.hasStoredPath(filePaths[documentIndex])) {
            file = EmployeeDocumentUtil.resolveStoredFile(filePaths[documentIndex]);
        }
        if (file == null || !file.exists()) {
            DialogHelper.warning(this, "File Not Found", "The saved document file could not be found.");
            return;
        }

        openPreviewAsync(documentIndex, file);
    }

    private void openPreviewAsync(int documentIndex, File file) {
        activeDocumentViews[documentIndex] = true;
        refreshDocumentActionCell(documentIndex);

        final DocumentOpenProgressDialog[] progressDialog = new DocumentOpenProgressDialog[1];
        final Timer[] delayTimer = new Timer[1];
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws IOException {
                return EmployeeDocumentUtil.readJpegImage(file);
            }

            @Override
            protected void done() {
                if (delayTimer[0] != null) {
                    delayTimer[0].stop();
                }
                if (progressDialog[0] != null) {
                    progressDialog[0].dispose();
                }

                if (isCancelled()) {
                    releaseActiveDocumentView(documentIndex);
                    return;
                }

                try {
                    BufferedImage img = get();
                    if (img == null) {
                        releaseActiveDocumentView(documentIndex);
                        DialogHelper.error(EmployeeDocumentViewPanel.this, "Cannot Open File", "Cannot open file.");
                        return;
                    }
                    showPreviewFrame(documentIndex, file, img);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    releaseActiveDocumentView(documentIndex);
                    DialogHelper.warning(EmployeeDocumentViewPanel.this, "Open Stopped", "Document loading was stopped.");
                } catch (ExecutionException exception) {
                    releaseActiveDocumentView(documentIndex);
                    DialogHelper.error(EmployeeDocumentViewPanel.this, "Cannot Open File", "Cannot open file.");
                }
            }
        };

        progressDialog[0] = new DocumentOpenProgressDialog(
                this,
                file.getName(),
                () -> {
                    worker.cancel(true);
                    releaseActiveDocumentView(documentIndex);
                }
        );
        progressDialog[0].setVisible(true);

        delayTimer[0] = new Timer(2200, event -> {
            if (worker.isDone()) {
                return;
            }
            if (progressDialog[0] != null) {
                progressDialog[0].showSlowNetworkMessage();
            }
        });
        delayTimer[0].setRepeats(false);
        delayTimer[0].start();
        worker.execute();
    }

    private void showPreviewFrame(int documentIndex, File file, BufferedImage img) {
        JFrame frame = new JFrame("Document Preview - " + file.getName());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JScrollPane previewScroll = new JScrollPane(new DocumentImagePreviewPanel(img));
        previewScroll.setBorder(null);
        previewScroll.getViewport().setBackground(Color.WHITE);
        frame.getContentPane().add(previewScroll);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                releaseActiveDocumentView(documentIndex);
            }

            @Override
            public void windowClosing(WindowEvent event) {
                releaseActiveDocumentView(documentIndex);
            }
        });
        EmployeeDocumentViewPanelHelper.stylePreviewFrame(frame, this);
        frame.setVisible(true);
    }

    private void releaseActiveDocumentView(int documentIndex) {
        if (documentIndex < 0 || documentIndex >= activeDocumentViews.length || !activeDocumentViews[documentIndex]) {
            return;
        }
        activeDocumentViews[documentIndex] = false;
        refreshDocumentActionCell(documentIndex);
    }

    private void refreshDocumentActionCell(int documentIndex) {
        int modelRow = findModelRowByDocumentIndex(documentIndex);
        if (modelRow < 0 || table == null) {
            return;
        }
        int viewRow = table.convertRowIndexToView(modelRow);
        int viewColumn = table.convertColumnIndexToView(ACTION_COLUMN);
        if (viewRow >= 0 && viewColumn >= 0) {
            table.repaint(table.getCellRect(viewRow, viewColumn, false));
        }
        model.fireTableRowsUpdated(modelRow, modelRow);
    }

    public boolean hasPendingDocumentUpdates() {
        for (int index = 0; index < files.length; index++) {
            if (!lockedDocuments[index] && files[index] != null) {
                return true;
            }
        }
        return false;
    }

    public void setPendingChangesListener(Runnable pendingChangesListener) {
        this.pendingChangesListener = pendingChangesListener;
    }

    public void setProfileImageUploadListener(Consumer<File> profileImageUploadListener) {
        this.profileImageUploadListener = profileImageUploadListener;
    }

    public void setProfileImageFromMainTab(File file) {
        int documentIndex = profileImageDocumentIndex();
        if (documentIndex < 0 || file == null || lockedDocuments[documentIndex]) {
            return;
        }
        discardTemporaryUpload(documentIndex, file);
        files[documentIndex] = file;
        filePaths[documentIndex] = file.getAbsolutePath();
        displayFileNames[documentIndex] = file.getName();
        temporaryUploadFiles[documentIndex] = EmployeeDocumentUtil.isTemporaryUploadFile(file);
        refreshDocumentRows();
        updateCount();
        model.fireTableDataChanged();
        notifyPendingChanges();
    }

    public File pendingProfileImageFile() {
        for (int index = 0; index < files.length; index++) {
            if (EmployeeDocumentUtil.isProfileImageDocument(index) && files[index] != null && !lockedDocuments[index]) {
                return files[index];
            }
        }
        return null;
    }

    private void notifyPendingChanges() {
        if (pendingChangesListener != null) {
            pendingChangesListener.run();
        }
    }

    private void notifyProfileImageUpload(int documentIndex, File file) {
        if (profileImageUploadListener != null
                && EmployeeDocumentUtil.isProfileImageDocument(documentIndex)
                && file != null) {
            profileImageUploadListener.accept(file);
        }
    }

    private int profileImageDocumentIndex() {
        for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
            if (EmployeeDocumentUtil.isProfileImageDocument(index)) {
                return index;
            }
        }
        return -1;
    }

    public Employee getDocumentUpdates(String empCode) throws IOException {
        Employee update = new Employee();
        if (!hasPendingDocumentUpdates()) {
            return update;
        }

        for (int index = 0; index < files.length; index++) {
            if (lockedDocuments[index] || files[index] == null) {
                continue;
            }

            String dbPath = EmployeeDocumentUtil.copyDocumentToEmployeeStorage(empCode, index, files[index]);
            EmployeeDocumentUtil.setDocumentPath(update, index, dbPath);
        }
        return update;
    }

    public String getDocumentPath(int index) {
        return filePaths != null && index < filePaths.length ? filePaths[index] : null;
    }

    public String[] getAllDocumentPaths() {
        return filePaths;
    }

    private boolean hasDocumentReference(int documentIndex) {
        return files[documentIndex] != null
                || EmployeeDocumentUtil.hasStoredPath(filePaths[documentIndex]);
    }

    private boolean rowHasPendingFile(int row) {
        return !lockedDocuments[row] && files[row] != null;
    }

    private String selectedFileName(int documentIndex) {
        String displayName = displayFileNames[documentIndex];
        return displayName == null || displayName.isBlank()
                ? files[documentIndex].getName()
                : displayName;
    }

    private void discardTemporaryUpload(int documentIndex, File replacement) {
        if (documentIndex < 0
                || documentIndex >= temporaryUploadFiles.length
                || !temporaryUploadFiles[documentIndex]) {
            return;
        }
        File current = files[documentIndex];
        temporaryUploadFiles[documentIndex] = false;
        if (current != null && !sameFile(current, replacement)) {
            EmployeeDocumentUtil.deleteTemporaryUpload(current);
        }
    }

    private boolean sameFile(File first, File second) {
        if (first == null || second == null) {
            return false;
        }
        return first.toPath().toAbsolutePath().normalize()
                .equals(second.toPath().toAbsolutePath().normalize());
    }

    class ActionRenderer extends JPanel implements TableCellRenderer {
        public ActionRenderer() {
            EmployeeDocumentViewPanelHelper.styleRendererPanel(this);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            removeAll();
            EmployeeDocumentViewPanelHelper.styleActionCell(this, isSelected);
            int modelRow = table.convertRowIndexToModel(row);
            int documentIndex = documentIndexForModelRow(modelRow);
            JPanel buttons = EmployeeDocumentViewPanelHelper.createActionButtonsPanel();

            if (!lockedDocuments[documentIndex]) {
                JButton uploadButton = createLink(rowHasPendingFile(documentIndex) ? "Replace" : "Upload");
                EmployeeDocumentViewPanelHelper.styleActionLink(uploadButton, true);
                buttons.add(uploadButton);
            } else {
                JButton locked = createLink("Locked");
                locked.setEnabled(false);
                EmployeeDocumentViewPanelHelper.styleActionLink(locked, false);
                buttons.add(locked);
            }

            JButton viewBtn = createLink("View");
            boolean canView = canViewDocument(documentIndex);
            viewBtn.setEnabled(canView);
            EmployeeDocumentViewPanelHelper.styleViewLink(viewBtn, canView);
            buttons.add(viewBtn);
            add(buttons, EmployeeDocumentViewPanelHelper.actionCellConstraints());

            return this;
        }

        private JButton createLink(String text) {
            return EmployeeDocumentViewPanelHelper.createActionLink(text);
        }
    }

    class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int documentIndex;

        public ActionEditor() {
            panel = EmployeeDocumentViewPanelHelper.createEditorPanel();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {

            int modelRow = table.convertRowIndexToModel(row);
            this.documentIndex = documentIndexForModelRow(modelRow);
            panel.removeAll();
            EmployeeDocumentViewPanelHelper.styleActionCell(panel, isSelected);
            JPanel buttons = EmployeeDocumentViewPanelHelper.createActionButtonsPanel();

            if (!lockedDocuments[documentIndex]) {
                JButton uploadButton = createButton(rowHasPendingFile(documentIndex) ? "Replace" : "Upload");
                EmployeeDocumentViewPanelHelper.styleActionLink(uploadButton, true);
                buttons.add(uploadButton);
            } else {
                JButton locked = createButton("Locked");
                locked.setEnabled(false);
                EmployeeDocumentViewPanelHelper.styleActionLink(locked, false);
                buttons.add(locked);
            }

            JButton viewBtn = createButton("View");
            boolean canView = canViewDocument(documentIndex);
            viewBtn.setEnabled(canView);
            EmployeeDocumentViewPanelHelper.styleViewLink(viewBtn, canView);
            buttons.add(viewBtn);
            panel.add(buttons, EmployeeDocumentViewPanelHelper.actionCellConstraints());

            return panel;
        }

        private JButton createButton(String text) {
            JButton btn = EmployeeDocumentViewPanelHelper.createActionLink(text);
            int actionDocumentIndex = documentIndex;
            btn.addActionListener(e -> {
                if ("Upload".equals(text) || "Replace".equals(text)) {
                    stopCellEditing();
                    chooseFile(actionDocumentIndex);
                } else if ("View".equals(text)) {
                    if (!canViewDocument(actionDocumentIndex)) {
                        stopCellEditing();
                        return;
                    }
                    btn.setEnabled(false);
                    EmployeeDocumentViewPanelHelper.styleViewLink(btn, false);
                    stopCellEditing();
                    viewFile(actionDocumentIndex);
                    return;
                }
            });
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private boolean canViewDocument(int documentIndex) {
        return hasDocumentReference(documentIndex) && !activeDocumentViews[documentIndex];
    }

    private static class DocumentOpenProgressDialog extends JDialog {
        private static final int DIALOG_WIDTH = 480;
        private JLabel titleLabel;
        private JTextArea messageArea;

        DocumentOpenProgressDialog(Component parent, String fileName, Runnable onStop) {
            super(owner(parent), "Opening document", Dialog.ModalityType.MODELESS);
            UniversalDialogHelper.styleDialogWindow(this);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            JPanel root = new JPanel(new BorderLayout());
            UniversalDialogHelper.styleRoot(root);
            root.add(createHeader(), BorderLayout.NORTH);
            root.add(createBody(fileName), BorderLayout.CENTER);
            root.add(createFooter(onStop), BorderLayout.SOUTH);
            setContentPane(root);
            pack();
            setMinimumSize(new Dimension(360, 230));
            setResizable(false);
            setLocationRelativeTo(owner(parent));
        }

        private JComponent createHeader() {
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(true);
            header.setBackground(UniversalDialogHelper.BACKGROUND);
            header.setBorder(BorderFactory.createEmptyBorder(22, 24, 4, 24));

            JPanel copy = new JPanel();
            copy.setOpaque(false);
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

            titleLabel = new JLabel("Opening document");
            titleLabel.setFont(UniversalDialogHelper.mediumFont(18));
            titleLabel.setForeground(UniversalDialogHelper.TEXT_PRIMARY);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel helper = new JLabel("Please keep this window open while the preview loads.");
            helper.setFont(UniversalDialogHelper.regularFont(13));
            helper.setForeground(UniversalDialogHelper.MUTED_TEXT);
            helper.setAlignmentX(Component.LEFT_ALIGNMENT);

            copy.add(titleLabel);
            copy.add(Box.createVerticalStrut(4));
            copy.add(helper);

            header.add(copy, BorderLayout.CENTER);
            return header;
        }

        private JComponent createBody(String fileName) {
            JPanel panel = new JPanel();
            panel.setOpaque(true);
            panel.setBackground(UniversalDialogHelper.BACKGROUND);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(14, 24, 18, 24));

            messageArea = new JTextArea(openingMessage(fileName));
            messageArea.setEditable(false);
            messageArea.setFocusable(false);
            messageArea.setOpaque(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setRows(4);
            messageArea.setFont(UniversalDialogHelper.regularFont(14));
            messageArea.setForeground(UniversalDialogHelper.TEXT_SECONDARY);
            messageArea.setBorder(BorderFactory.createEmptyBorder());
            Dimension textSize = new Dimension(DIALOG_WIDTH - 48, 78);
            messageArea.setPreferredSize(textSize);
            messageArea.setMaximumSize(textSize);
            messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            progress.setBorderPainted(false);
            progress.setPreferredSize(new Dimension(DIALOG_WIDTH - 48, 14));
            progress.setMaximumSize(new Dimension(DIALOG_WIDTH - 48, 14));
            progress.setAlignmentX(Component.LEFT_ALIGNMENT);

            panel.add(messageArea);
            panel.add(Box.createVerticalStrut(14));
            panel.add(progress);
            return panel;
        }

        private JComponent createFooter(Runnable onStop) {
            JPanel footer = UniversalDialogHelper.createFooter();
            JButton stop = destructiveButton("Stop");
            stop.addActionListener(event -> {
                stop.setText("Stopping...");
                stop.setEnabled(false);
                stop.setBackground(new Color(153, 27, 27));
                stop.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                onStop.run();
                dispose();
            });
            footer.add(stop);
            getRootPane().setDefaultButton(stop);
            return footer;
        }

        private JButton destructiveButton(String text) {
            JButton button = new JButton(text);
            Color normal = new Color(220, 38, 38);
            Color hover = new Color(185, 28, 28);
            Color pressed = new Color(153, 27, 27);
            button.setPreferredSize(new Dimension(112, 36));
            button.setBackground(normal);
            button.setForeground(Color.WHITE);
            button.setFont(UniversalDialogHelper.mediumFont(13));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setOpaque(true);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.getModel().addChangeListener(event -> {
                ButtonModel model = button.getModel();
                if (!button.isEnabled()) {
                    return;
                }
                if (model.isPressed()) {
                    button.setBackground(pressed);
                } else if (model.isRollover()) {
                    button.setBackground(hover);
                } else {
                    button.setBackground(normal);
                }
            });
            return button;
        }

        private void showSlowNetworkMessage() {
            titleLabel.setText("Still opening document");
            messageArea.setText(
                    "This is taking longer than usual, usually because the shared folder or network is slow. "
                            + "The preview will open automatically when the file is ready. Press Stop to cancel this attempt."
            );
            revalidate();
            repaint();
        }

        private static String openingMessage(String fileName) {
            String cleanName = fileName == null || fileName.isBlank() ? "the selected document" : fileName;
            return "Loading " + cleanName
                    + ". The preview will open automatically when the file is ready. Press Stop to cancel this attempt.";
        }

        private static Window owner(Component parent) {
            if (parent instanceof Window window) {
                return window;
            }
            return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        }
    }

    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(130, 140, 150));
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, 0, y);
            g2.dispose();
        }
    }
}
