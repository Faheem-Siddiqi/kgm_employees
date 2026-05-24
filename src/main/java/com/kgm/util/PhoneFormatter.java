package com.kgm.util;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Phone number formatter that auto-formats as nnnn-nnnnnn (4 digits, dash, 6 digits).
 */
public final class PhoneFormatter {
    public static final String FORMAT_EXAMPLE = "1234-567890";
    private static final int MAX_DIGITS = 10;
    private static final int FIRST_PART_LENGTH = 4;

    private PhoneFormatter() {
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static String format(String value) {
        String digits = digitsOnly(value);
        if (digits.length() > MAX_DIGITS) {
            digits = digits.substring(0, MAX_DIGITS);
        }
        if (digits.length() <= FIRST_PART_LENGTH) {
            return digits;
        }
        return digits.substring(0, FIRST_PART_LENGTH) + "-" + digits.substring(FIRST_PART_LENGTH);
    }

    public static boolean isValid(String value) {
        return value != null && value.trim().matches("\\d{4}-\\d{6}");
    }

    public static void installFormatter(JTextField field) {
        if (field == null || !(field.getDocument() instanceof AbstractDocument document)) {
            return;
        }
        document.setDocumentFilter(new PhoneDocumentFilter());
        field.setToolTipText("Phone format: " + FORMAT_EXAMPLE);
    }

    private static class PhoneDocumentFilter extends DocumentFilter {
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset)
                    + (text == null ? "" : text)
                    + current.substring(offset + length);
            String formatted = format(next);
            fb.replace(0, fb.getDocument().getLength(), formatted, attrs);
        }

        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            replace(fb, offset, length, "", null);
        }
    }
}