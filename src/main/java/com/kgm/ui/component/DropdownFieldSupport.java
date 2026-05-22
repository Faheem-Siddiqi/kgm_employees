package com.kgm.ui.component;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public final class DropdownFieldSupport {
    private static final String AUTOCOMPLETE_INSTALLED = "kgm.dropdown.autocompleteInstalled";

    private DropdownFieldSupport() {
    }

    public static void configure(JComboBox<String> comboBox, boolean allowCustomValue) {
        comboBox.setEditable(allowCustomValue);
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
        if (Boolean.TRUE.equals(comboBox.getClientProperty(AUTOCOMPLETE_INSTALLED))
                || !(comboBox.getEditor().getEditorComponent() instanceof JTextField editor)) {
            return;
        }
        comboBox.putClientProperty(AUTOCOMPLETE_INSTALLED, Boolean.TRUE);

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.isActionKey()
                        || event.getKeyCode() == KeyEvent.VK_BACK_SPACE
                        || event.getKeyCode() == KeyEvent.VK_DELETE
                        || event.getKeyCode() == KeyEvent.VK_ENTER
                        || event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }

                String typed = editor.getText();
                if (typed == null || typed.isBlank()) {
                    return;
                }

                String match = firstPrefixMatch(comboBox, typed);
                if (match == null || match.equalsIgnoreCase(typed)) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    editor.setText(match);
                    editor.setSelectionStart(Math.min(typed.length(), match.length()));
                    editor.setSelectionEnd(match.length());
                });
            }
        });
    }

    private static String firstPrefixMatch(JComboBox<String> comboBox, String typed) {
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            String item = comboBox.getItemAt(index);
            if (item != null
                    && !item.isBlank()
                    && item.toLowerCase().startsWith(typed.toLowerCase())) {
                return item;
            }
        }
        return null;
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
