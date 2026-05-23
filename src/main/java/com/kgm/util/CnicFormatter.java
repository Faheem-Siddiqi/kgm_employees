package com.kgm.util;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public final class CnicFormatter {
    public static final String FORMAT_EXAMPLE = "12345-1234567-4";
    private static final int MAX_DIGITS = 13;

    private CnicFormatter() {
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static String format(String value) {
        String digits = digitsOnly(value);
        if (digits.length() > MAX_DIGITS) {
            digits = digits.substring(0, MAX_DIGITS);
        }
        if (digits.length() <= 5) {
            return digits;
        }
        if (digits.length() <= 12) {
            return digits.substring(0, 5) + "-" + digits.substring(5);
        }
        return digits.substring(0, 5) + "-" + digits.substring(5, 12) + "-" + digits.substring(12);
    }

    public static boolean isValid(String value) {
        return value != null && value.trim().matches("\\d{5}-\\d{7}-\\d");
    }

    public static void installFormatter(JTextField field) {
        if (field == null || !(field.getDocument() instanceof AbstractDocument document)) {
            return;
        }
        document.setDocumentFilter(new CnicDocumentFilter());
        field.setToolTipText("CNIC format: " + FORMAT_EXAMPLE);
    }

    private static class CnicDocumentFilter extends DocumentFilter {
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
