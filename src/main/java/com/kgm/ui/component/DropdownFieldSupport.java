package com.kgm.ui.component;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Locale;

public final class DropdownFieldSupport {
    private static final String AUTOCOMPLETE_INSTALLED = "kgm.dropdown.autocompleteInstalled";

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
            installAutocomplete(comboBox);
        }
    }

    public static String value(JComboBox<?> comboBox) {
        Object value = comboBox.isEditable()
                ? comboBox.getEditor().getItem()
                : comboBox.getSelectedItem();
        return value == null ? "" : value.toString().trim();
    }

    public static void setValue(JComboBox<String> comboBox, String value) {
        String cleanValue = value == null ? "" : value.trim();
        if (cleanValue.isEmpty()) {
            if (comboBox.getItemCount() > 0) {
                comboBox.setSelectedIndex(0);
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
        String query = typed.toLowerCase(Locale.ROOT);
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            String item = comboBox.getItemAt(index);
            if (item == null || item.isBlank()) {
                continue;
            }
            String candidate = item.toLowerCase(Locale.ROOT);
            if (candidate.startsWith(query)) {
                return index;
            }
            if (containsMatch < 0 && candidate.contains(query)) {
                containsMatch = index;
            }
        }
        return containsMatch;
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
}
