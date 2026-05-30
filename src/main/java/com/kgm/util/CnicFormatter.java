package com.kgm.util;

import javax.swing.JTextField;

public final class CnicFormatter {
    public static final String FORMAT_EXAMPLE = "any CNIC/NID text";

    private CnicFormatter() {
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static String format(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static void installFormatter(JTextField field) {
        if (field == null) {
            return;
        }
        field.setToolTipText("CNIC/NID can contain text, numbers, or dashes.");
    }
}
