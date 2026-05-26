package com.kgm.ui.component;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public final class DropdownFieldSupport {
    private static final int VARIABLE_OPTION_VISIBLE_ROWS = 8;
    private static final String AUTOCOMPLETE_INSTALLED = "kgm.dropdown.autocompleteInstalled";
    private static final String PLACEHOLDER_KEY = "kgm.dropdown.placeholder";
    private static final String PLACEHOLDER_CLEAR_INSTALLED = "kgm.dropdown.placeholderClearInstalled";
    private static final String ASYNC_SEARCH_INSTALLED = "kgm.dropdown.asyncSearchInstalled";

    private DropdownFieldSupport() {
    }

    public static void configure(JComboBox<String> comboBox, boolean allowCustomValue) {
        comboBox.setEditable(allowCustomValue);
        comboBox.setFocusable(allowCustomValue);
        comboBox.setRequestFocusEnabled(allowCustomValue);
        if (allowCustomValue
                && comboBox.getEditor().getEditorComponent() instanceof JComponent editor) {
            editor.setFocusable(true);
            editor.setRequestFocusEnabled(true);
        }
        if (allowCustomValue) {
            comboBox.setMaximumRowCount(VARIABLE_OPTION_VISIBLE_ROWS);
            installAutocomplete(comboBox);
            if (comboBox.getEditor().getEditorComponent() instanceof JTextField editor) {
                installPlaceholderClear(comboBox, editor);
            }
        }
    }

    public static String value(JComboBox<?> comboBox) {
        Object value = comboBox.isEditable()
                ? comboBox.getEditor().getItem()
                : comboBox.getSelectedItem();
        String text = value == null ? "" : value.toString().trim();
        if (isPlaceholderValue(comboBox, text)) {
            return "";
        }
        return text;
    }

    public static void setValue(JComboBox<String> comboBox, String value) {
        String cleanValue = value == null ? "" : value.trim();
        String placeholder = getPlaceholder(comboBox);
        if (cleanValue.isEmpty() || (placeholder != null && placeholder.equalsIgnoreCase(cleanValue))) {
            if (comboBox.getItemCount() > 0) {
                comboBox.setSelectedIndex(0);
            }
            if (comboBox.isEditable() && comboBox.getEditor().getEditorComponent() instanceof JTextField editor) {
                editor.setText("");
            }
            return;
        }

        if (!comboBox.isEditable() && !containsItem(comboBox, cleanValue)) {
            comboBox.addItem(cleanValue);
        }
        comboBox.setSelectedItem(cleanValue);
        if (comboBox.isEditable()) {
            comboBox.getEditor().setItem(cleanValue);
        }
    }

    public static void setPlaceholder(JComboBox<String> comboBox, String placeholder) {
        comboBox.putClientProperty(PLACEHOLDER_KEY, placeholder);
        if (comboBox.getEditor().getEditorComponent() instanceof JTextField editor) {
            editor.setToolTipText(placeholder);
            installPlaceholderClear(comboBox, editor);
        }
    }

    public static void installAsyncSearch(
            JComboBox<String> comboBox,
            Function<String, List<String>> searcher,
            int debounceMs
    ) {
        if (comboBox == null
                || searcher == null
                || !comboBox.isEditable()
                || !(comboBox.getEditor().getEditorComponent() instanceof JTextField editor)) {
            return;
        }
        if (comboBox.getClientProperty(ASYNC_SEARCH_INSTALLED) != null) {
            return;
        }

        SearchState state = new SearchState(comboBox, editor, searcher, Math.max(100, debounceMs));
        comboBox.putClientProperty(ASYNC_SEARCH_INSTALLED, state);
        state.install();
    }

    private static String getPlaceholder(JComboBox<?> comboBox) {
        Object prop = comboBox.getClientProperty(PLACEHOLDER_KEY);
        return prop instanceof String text ? text : null;
    }

    private static boolean isPlaceholderValue(JComboBox<?> comboBox, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String placeholder = getPlaceholder(comboBox);
        return (placeholder != null && placeholder.equalsIgnoreCase(text))
                || "Choose".equalsIgnoreCase(text)
                || "Type to select".equalsIgnoreCase(text);
    }

    private static void installPlaceholderClear(JComboBox<String> comboBox, JTextField editor) {
        if (comboBox.getClientProperty(PLACEHOLDER_CLEAR_INSTALLED) == editor) {
            return;
        }
        comboBox.putClientProperty(PLACEHOLDER_CLEAR_INSTALLED, editor);

        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                clearPlaceholder(comboBox, editor);
            }

            @Override
            public void focusLost(FocusEvent event) {
                restorePlaceholder(comboBox, editor);
            }
        });
        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                clearPlaceholder(comboBox, editor);
            }
        });
    }

    private static void clearPlaceholder(JComboBox<?> comboBox, JTextField editor) {
        if (isPlaceholderValue(comboBox, editor.getText())) {
            editor.setText("");
        }
    }

    private static void restorePlaceholder(JComboBox<String> comboBox, JTextField editor) {
        String placeholder = getPlaceholder(comboBox);
        if (placeholder == null || !editor.getText().trim().isEmpty() || comboBox.getItemCount() == 0) {
            return;
        }

        String firstItem = comboBox.getItemAt(0);
        if (firstItem != null && firstItem.equalsIgnoreCase(placeholder)) {
            comboBox.setSelectedIndex(0);
            editor.setText(firstItem);
            editor.setCaretPosition(0);
        }
    }

    private static void installAutocomplete(JComboBox<String> comboBox) {
        if (!(comboBox.getEditor().getEditorComponent() instanceof JTextField editor)
                || comboBox.getClientProperty(AUTOCOMPLETE_INSTALLED) == editor) {
            return;
        }
        comboBox.putClientProperty(AUTOCOMPLETE_INSTALLED, editor);

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    acceptSuggestion(comboBox, editor, event);
                } else if (event.getKeyCode() == KeyEvent.VK_ESCAPE && comboBox.isPopupVisible()) {
                    comboBox.hidePopup();
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.isActionKey()
                        || event.getKeyCode() == KeyEvent.VK_ENTER
                        || event.getKeyCode() == KeyEvent.VK_ESCAPE
                        || event.getKeyCode() == KeyEvent.VK_UP
                        || event.getKeyCode() == KeyEvent.VK_DOWN) {
                    return;
                }

                showSuggestion(comboBox, editor);
            }
        });
    }

    private static void showSuggestion(JComboBox<String> comboBox, JTextField editor) {
        String typed = editor.getText();
        if (typed == null || typed.isBlank()) {
            if (comboBox.isPopupVisible()) {
                comboBox.hidePopup();
            }
            return;
        }

        int matchIndex = firstMatchIndex(comboBox, typed);
        if (matchIndex < 0) {
            if (comboBox.isPopupVisible()) {
                comboBox.hidePopup();
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (!editor.isFocusOwner()) {
                return;
            }

            String currentText = editor.getText();
            comboBox.setSelectedIndex(matchIndex);
            editor.setText(currentText);
            editor.setCaretPosition(currentText.length());
            if (comboBox.isShowing()) {
                comboBox.showPopup();
            }
        });
    }

    private static void acceptSuggestion(JComboBox<String> comboBox, JTextField editor, KeyEvent event) {
        String typed = editor.getText();
        if (typed == null || typed.isBlank() || !comboBox.isPopupVisible()) {
            return;
        }

        int matchIndex = firstMatchIndex(comboBox, typed);
        if (matchIndex < 0) {
            comboBox.hidePopup();
            return;
        }

        String match = comboBox.getItemAt(matchIndex);
        comboBox.setSelectedItem(match);
        editor.setText(match);
        editor.setCaretPosition(match.length());
        comboBox.hidePopup();
        event.consume();
    }

    private static int firstMatchIndex(JComboBox<String> comboBox, String typed) {
        int containsMatch = -1;
        String query = searchKey(typed);
        if (query.isBlank()) {
            return -1;
        }
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            String item = comboBox.getItemAt(index);
            if (item == null || item.isBlank()) {
                continue;
            }
            String candidate = searchKey(item);
            if (candidate.startsWith(query)) {
                return index;
            }
            if (containsMatch < 0 && candidate.contains(query)) {
                containsMatch = index;
            }
        }
        return containsMatch;
    }

    private static String searchKey(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "").trim();
    }

    private static List<String> comboItems(JComboBox<String> comboBox) {
        List<String> items = new ArrayList<>();
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            String item = comboBox.getItemAt(index);
            if (item != null && !containsIgnoreCase(items, item)) {
                items.add(item);
            }
        }
        return items;
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsItem(JComboBox<String> comboBox, String value) {
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            String item = comboBox.getItemAt(index);
            if (item != null && item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static final class SearchState {
        private final JComboBox<String> comboBox;
        private final JTextField editor;
        private final Function<String, List<String>> searcher;
        private final int debounceMs;
        private final List<String> baseItems;
        private Timer timer;
        private SwingWorker<List<String>, Void> worker;
        private int revision;
        private boolean updatingModel;

        private SearchState(
                JComboBox<String> comboBox,
                JTextField editor,
                Function<String, List<String>> searcher,
                int debounceMs
        ) {
            this.comboBox = comboBox;
            this.editor = editor;
            this.searcher = searcher;
            this.debounceMs = debounceMs;
            this.baseItems = comboItems(comboBox);
        }

        private void install() {
            timer = new Timer(debounceMs, event -> runSearch(editor.getText()));
            timer.setRepeats(false);

            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    schedule();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    schedule();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    schedule();
                }
            });
        }

        private void schedule() {
            if (updatingModel) {
                return;
            }
            revision++;
            if (worker != null && !worker.isDone()) {
                worker.cancel(true);
            }

            String query = cleanEditorText();
            if (query.isBlank() || isPlaceholderValue(comboBox, query)) {
                timer.stop();
                int resetRevision = revision;
                SwingUtilities.invokeLater(() -> {
                    if (resetRevision == revision) {
                        resetToBaseItems("");
                    }
                });
                if (comboBox.isPopupVisible()) {
                    comboBox.hidePopup();
                }
                return;
            }
            timer.restart();
        }

        private void runSearch(String rawQuery) {
            String query = cleanQuery(rawQuery);
            if (query.isBlank() || isPlaceholderValue(comboBox, query)) {
                return;
            }

            int searchRevision = revision;
            worker = new SwingWorker<>() {
                @Override
                protected List<String> doInBackground() {
                    List<String> results = searcher.apply(query);
                    return results == null ? List.of() : results;
                }

                @Override
                protected void done() {
                    if (isCancelled() || searchRevision != revision) {
                        return;
                    }
                    try {
                        updateResults(query, get());
                    } catch (Exception ignored) {
                    }
                }
            };
            worker.execute();
        }

        private void updateResults(String query, List<String> results) {
            List<String> values = new ArrayList<>();
            for (String item : baseItems) {
                if (item != null && !item.isBlank() && !containsIgnoreCase(values, item)) {
                    values.add(item);
                }
            }
            for (String result : results) {
                if (result != null && !result.isBlank() && !containsIgnoreCase(values, result.trim())) {
                    values.add(result.trim());
                }
            }
            rebuildModel(new LinkedHashSet<>(values), query);
            if (editor.isFocusOwner() && comboBox.isShowing() && comboBox.getItemCount() > 0) {
                comboBox.showPopup();
            }
        }

        private void resetToBaseItems(String editorText) {
            rebuildModel(new LinkedHashSet<>(baseItems), editorText);
        }

        private void rebuildModel(Set<String> values, String editorText) {
            updatingModel = true;
            try {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                for (String value : values) {
                    model.addElement(value);
                }
                comboBox.setModel(model);
                comboBox.setSelectedItem(editorText);
                editor.setText(editorText);
                editor.setCaretPosition(editor.getText().length());
            } finally {
                updatingModel = false;
            }
        }

        private String cleanEditorText() {
            return cleanQuery(editor.getText());
        }

        private String cleanQuery(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
