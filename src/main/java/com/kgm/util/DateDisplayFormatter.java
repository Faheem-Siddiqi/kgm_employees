package com.kgm.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DateDisplayFormatter {
    public static final String DISPLAY_PATTERN = "dd-MM-yyyy";

    private static final List<String> READ_PATTERNS = List.of(
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy H:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy H:mm:ss",
            "dd-MM-yyyy HH:mm",
            "M/d/yyyy HH:mm:ss",
            "M/d/yyyy H:mm:ss",
            "M/d/yy HH:mm:ss",
            "M/d/yy H:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd H:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd H:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "M/d/yyyy",
            "M/d/yy"
    );

    private DateDisplayFormatter() {
    }

    public static String format(Date date) {
        if (date == null) {
            return "";
        }
        return formatter(DISPLAY_PATTERN).format(date);
    }

    public static String format(String value) {
        if (isBlankValue(value)) {
            return "";
        }
        Date parsed = parse(value);
        return parsed == null ? value.trim() : format(parsed);
    }

    public static Date parse(String value) {
        if (isBlankValue(value)) {
            return null;
        }

        String text = value.trim();
        for (String pattern : READ_PATTERNS) {
            SimpleDateFormat format = formatter(pattern);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(text, position);
            if (parsed != null && position.getIndex() == text.length()) {
                return parsed;
            }
        }
        return null;
    }

    private static SimpleDateFormat formatter(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setLenient(false);
        return format;
    }

    private static boolean isBlankValue(String value) {
        if (value == null) {
            return true;
        }
        String text = value.trim();
        return text.isEmpty()
                || text.equalsIgnoreCase("N/A")
                || text.equalsIgnoreCase("NA")
                || text.equalsIgnoreCase("NULL")
                || text.equals("-");
    }
}
