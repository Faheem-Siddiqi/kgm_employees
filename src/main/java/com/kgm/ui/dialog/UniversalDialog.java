package com.kgm.ui.dialog;

import com.kgm.ui.styling.UniversalDialogHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class UniversalDialog {
    public enum Type {
        INFO(UniversalDialogHelper.INFO_ACCENT),
        SUCCESS(UniversalDialogHelper.SUCCESS_ACCENT),
        WARNING(UniversalDialogHelper.WARNING_ACCENT),
        ERROR(UniversalDialogHelper.ERROR_ACCENT);

        private final Color accent;

        Type(Color accent) {
            this.accent = accent;
        }
    }

    private static final int WRAP_COLUMNS = 54;
    private static final int SCROLLABLE_SECTION_ROWS = 10;
    public static final String SECTION_SEPARATOR = "\n\n::kgm-dialog-section::\n\n";

    private UniversalDialog() {
    }

    public static void message(Component parent, Type type, String title, String message) {
        option(parent, type, title, message, "OK");
    }

    public static int option(
            Component parent,
            Type type,
            String title,
            String message,
            String primaryOption,
            String... secondaryOptions
    ) {
        Window owner = owner(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        int[] selected = {-1};

        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content(dialog, selected, type, title, message, primaryOption, secondaryOptions));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(430, 220));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return selected[0];
    }

    private static JPanel content(
            JDialog dialog,
            int[] selected,
            Type type,
            String title,
            String message,
            String primaryOption,
            String[] secondaryOptions
    ) {
        JPanel root = new JPanel(new BorderLayout());
        UniversalDialogHelper.styleRoot(root);
        root.add(header(title, type.accent), BorderLayout.NORTH);
        root.add(body(type, message), BorderLayout.CENTER);
        root.add(footer(dialog, selected, type.accent, primaryOption, secondaryOptions), BorderLayout.SOUTH);
        return root;
    }

    private static JPanel header(String title, Color accent) {
        return UniversalDialogHelper.createHeader(title, accent);
    }

    private static JComponent body(Type type, String message) {
        String text = message == null || message.isBlank() ? "-" : message.trim();
        List<String> sections = messageSections(text);

        JPanel panel = UniversalDialogHelper.createBodyPanel();

        for (int index = 0; index < sections.size(); index++) {
            JPanel messageBox = messageBox(type, sections.get(index));
            messageBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(messageBox);
            if (index < sections.size() - 1) {
                panel.add(Box.createVerticalStrut(10));
            }
        }

        return UniversalDialogHelper.createBodyScroll(panel, preferredMessageHeight(sections));
    }

    private static JPanel messageBox(Type type, String message) {
        JPanel row = UniversalDialogHelper.createMessageRow(type);

        JLabel badge = UniversalDialogHelper.createBadge(type, type.accent);

        String[] parts = headingAndBody(message);
        JPanel textPanel = UniversalDialogHelper.createMessageTextPanel();

        if (!parts[0].isEmpty()) {
            JLabel heading = UniversalDialogHelper.createHeading(parts[0]);
            textPanel.add(heading);
            textPanel.add(Box.createVerticalStrut(4));
        }

        int contentRows = Math.max(1, wrappedRows(parts[1], WRAP_COLUMNS));
        boolean scrollableText = isScrollableSection(parts[0]) && contentRows > SCROLLABLE_SECTION_ROWS;
        int visibleRows = scrollableText ? SCROLLABLE_SECTION_ROWS : contentRows;

        JTextArea text = UniversalDialogHelper.createMessageText(parts[1], row.getBackground(), contentRows);

        int lineHeight = text.getFontMetrics(text.getFont()).getHeight();
        int textHeight = Math.max(42, visibleRows * lineHeight + 4);
        int contentHeight = Math.max(textHeight, contentRows * lineHeight + 4);
        int headingHeight = parts[0].isEmpty() ? 0 : 22;
        Dimension textSize = new Dimension(UniversalDialogHelper.MESSAGE_TEXT_WIDTH, contentHeight);
        UniversalDialogHelper.setFixedSize(text, textSize);

        if (scrollableText) {
            Dimension scrollSize = new Dimension(UniversalDialogHelper.MESSAGE_TEXT_WIDTH, textHeight);
            JScrollPane sectionScroll = UniversalDialogHelper.createSectionScroll(text, row.getBackground(), scrollSize);
            textPanel.add(sectionScroll);
        } else {
            Dimension visibleTextSize = new Dimension(UniversalDialogHelper.MESSAGE_TEXT_WIDTH, textHeight);
            UniversalDialogHelper.setFixedSize(text, visibleTextSize);
            textPanel.add(text);
        }
        Dimension panelSize = new Dimension(UniversalDialogHelper.MESSAGE_TEXT_WIDTH, textHeight + headingHeight);
        UniversalDialogHelper.setFixedSize(textPanel, panelSize);

        int boxHeight = textHeight + headingHeight + 22;
        Dimension boxSize = new Dimension(UniversalDialogHelper.MESSAGE_BOX_WIDTH, boxHeight);
        UniversalDialogHelper.setFixedSize(row, boxSize);

        row.add(badge, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);
        return row;
    }

    private static JPanel footer(
            JDialog dialog,
            int[] selected,
            Color accent,
            String primaryOption,
            String[] secondaryOptions
    ) {
        JPanel footer = UniversalDialogHelper.createFooter();

        for (int index = secondaryOptions.length - 1; index >= 0; index--) {
            String option = secondaryOptions[index];
            int optionIndex = index + 1;
            JButton secondary = secondaryButton(option);
            secondary.addActionListener(event -> {
                selected[0] = optionIndex;
                dialog.dispose();
            });
            footer.add(secondary);
        }

        JButton primary = primaryButton(primaryOption, accent);
        primary.addActionListener(event -> {
            selected[0] = 0;
            dialog.dispose();
        });
        footer.add(primary);
        dialog.getRootPane().setDefaultButton(primary);
        return footer;
    }

    private static JButton primaryButton(String text, Color accent) {
        return UniversalDialogHelper.primaryButton(text, accent);
    }

    private static JButton secondaryButton(String text) {
        return UniversalDialogHelper.secondaryButton(text);
    }

    private static int preferredMessageHeight(List<String> sections) {
        int height = 36;
        for (String section : sections) {
            String[] parts = headingAndBody(section);
            int headingHeight = parts[0].isEmpty() ? 0 : 22;
            int rows = visibleRows(parts[0], parts[1]);
            height += 22 + headingHeight + Math.max(42, rows * 19);
        }
        height += Math.max(0, sections.size() - 1) * 10;
        return Math.min(420, height);
    }

    private static int visibleRows(String heading, String body) {
        int rows = Math.max(1, wrappedRows(body, WRAP_COLUMNS));
        if (isScrollableSection(heading)) {
            return Math.min(SCROLLABLE_SECTION_ROWS, rows);
        }
        return rows;
    }

    private static boolean isScrollableSection(String heading) {
        return "Skipped rows".equalsIgnoreCase(heading)
                || "Rows to review".equalsIgnoreCase(heading);
    }

    private static int wrappedRows(String message, int columns) {
        int rows = 0;
        for (String line : message.split("\\R", -1)) {
            rows += Math.max(1, line.length() / columns + 1);
        }
        return rows;
    }

    private static List<String> messageSections(String message) {
        List<String> sections = new ArrayList<>();
        for (String section : message.split(Pattern.quote(SECTION_SEPARATOR), -1)) {
            String trimmed = section.trim();
            if (!trimmed.isEmpty()) {
                sections.add(trimmed);
            }
        }
        if (sections.isEmpty()) {
            sections.add("-");
        }
        return sections;
    }

    private static String[] headingAndBody(String message) {
        String[] lines = message.split("\\R", 2);
        if (lines.length < 2 || lines[1].trim().isEmpty()) {
            return new String[]{"", message};
        }
        return new String[]{lines[0].trim(), lines[1].trim()};
    }

    private static Window owner(Component parent) {
        if (parent instanceof Window window) {
            return window;
        }
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }
}
