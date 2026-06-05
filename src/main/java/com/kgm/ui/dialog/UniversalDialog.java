package com.kgm.ui.dialog;

import com.kgm.ui.styling.UniversalDialogHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
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

        public Color accent() {
            return UniversalDialogHelper.accentFor(this);
        }
    }

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
        JDialog dialog = new JDialog(
                owner,
                UniversalDialogHelper.displayTitle(type, title),
                Dialog.ModalityType.APPLICATION_MODAL
        );
        int[] selected = {-1};
        int dialogWidth = UniversalDialogHelper.dialogWidth(owner);
        int maxBodyHeight = UniversalDialogHelper.maxBodyHeight(owner);

        prepareDialog(dialog);
        dialog.setContentPane(content(
                dialog,
                selected,
                type,
                title,
                message,
                primaryOption,
                secondaryOptions,
                dialogWidth,
                maxBodyHeight
        ));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.min(360, dialogWidth), 196));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return selected[0];
    }

    public static int formOption(
            Component parent,
            Type type,
            String title,
            JComponent form,
            String primaryOption,
            String... secondaryOptions
    ) {
        Window owner = owner(parent);
        JDialog dialog = new JDialog(
                owner,
                UniversalDialogHelper.displayTitle(type, title),
                Dialog.ModalityType.APPLICATION_MODAL
        );
        int[] selected = {-1};
        int dialogWidth = UniversalDialogHelper.dialogWidth(owner);
        int maxBodyHeight = UniversalDialogHelper.maxBodyHeight(owner);

        prepareDialog(dialog);
        dialog.setContentPane(formContent(
                dialog,
                selected,
                type,
                title,
                form,
                primaryOption,
                secondaryOptions,
                dialogWidth,
                maxBodyHeight
        ));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.min(360, dialogWidth), 196));
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
            String[] secondaryOptions,
            int dialogWidth,
            int maxBodyHeight
    ) {
        JPanel root = new JPanel(new BorderLayout());
        UniversalDialogHelper.styleRoot(root);
        root.add(header(dialog, type, title), BorderLayout.NORTH);
        root.add(body(type, message, dialogWidth, maxBodyHeight), BorderLayout.CENTER);
        root.add(footer(dialog, selected, type.accent(), primaryOption, secondaryOptions), BorderLayout.SOUTH);
        return root;
    }

    private static JPanel formContent(
            JDialog dialog,
            int[] selected,
            Type type,
            String title,
            JComponent form,
            String primaryOption,
            String[] secondaryOptions,
            int dialogWidth,
            int maxBodyHeight
    ) {
        JPanel root = new JPanel(new BorderLayout());
        UniversalDialogHelper.styleRoot(root);
        root.add(header(dialog, type, title), BorderLayout.NORTH);
        root.add(formBody(form, dialogWidth, maxBodyHeight), BorderLayout.CENTER);
        root.add(footer(dialog, selected, type.accent(), primaryOption, secondaryOptions), BorderLayout.SOUTH);
        return root;
    }

    private static void prepareDialog(JDialog dialog) {
        UniversalDialogHelper.styleDialogWindow(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private static JPanel header(JDialog dialog, Type type, String title) {
        return UniversalDialogHelper.createHeader(type, title, dialog::dispose);
    }

    private static JComponent body(Type type, String message, int dialogWidth, int maxBodyHeight) {
        String text = message == null || message.isBlank() ? "No details were provided." : message.trim();
        List<String> sections = messageSections(text);

        JPanel panel = UniversalDialogHelper.createBodyPanel();
        int alertWidth = UniversalDialogHelper.alertWidth(dialogWidth);

        for (int index = 0; index < sections.size(); index++) {
            JPanel messageBox = messageBox(type, sections.get(index), alertWidth);
            messageBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(messageBox);
            if (index < sections.size() - 1) {
                panel.add(Box.createVerticalStrut(10));
            }
        }

        return UniversalDialogHelper.createBodyScroll(
                panel,
                preferredMessageHeight(sections, alertWidth),
                dialogWidth,
                maxBodyHeight
        );
    }

    private static JComponent formBody(JComponent form, int dialogWidth, int maxBodyHeight) {
        JPanel panel = UniversalDialogHelper.createBodyPanel();
        JComponent content = form == null ? new JPanel() : form;
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(content);
        int height = UniversalDialogHelper.bodyHeightWithPadding(
                Math.max(140, content.getPreferredSize().height)
        );
        return UniversalDialogHelper.createBodyScroll(panel, height, dialogWidth, maxBodyHeight);
    }

    private static JPanel messageBox(Type type, String message, int alertWidth) {
        JPanel row = UniversalDialogHelper.createMessageRow(type);

        JLabel badge = UniversalDialogHelper.createBadge(type, type.accent());

        String[] parts = headingAndBody(message);
        JPanel textPanel = UniversalDialogHelper.createMessageTextPanel();

        if (!parts[0].isEmpty()) {
            JLabel heading = UniversalDialogHelper.createHeading(parts[0]);
            textPanel.add(heading);
            textPanel.add(Box.createVerticalStrut(4));
        }

        int textWidth = UniversalDialogHelper.messageTextWidth(alertWidth);
        int wrapColumns = UniversalDialogHelper.wrapColumns(textWidth);
        int contentRows = Math.max(1, wrappedRows(parts[1], wrapColumns));
        boolean scrollableText = isScrollableSection(parts[0]) && contentRows > SCROLLABLE_SECTION_ROWS;
        int visibleRows = scrollableText ? SCROLLABLE_SECTION_ROWS : contentRows;

        JTextArea text = UniversalDialogHelper.createMessageText(parts[1], row.getBackground(), contentRows);

        int lineHeight = text.getFontMetrics(text.getFont()).getHeight();
        int textHeight = Math.max(42, visibleRows * lineHeight + 4);
        int contentHeight = Math.max(textHeight, contentRows * lineHeight + 4);
        int headingHeight = parts[0].isEmpty() ? 0 : 22;
        Dimension textSize = new Dimension(textWidth, contentHeight);
        UniversalDialogHelper.setFixedSize(text, textSize);

        if (scrollableText) {
            Dimension scrollSize = new Dimension(textWidth, UniversalDialogHelper.sectionScrollHeight(textHeight));
            JScrollPane sectionScroll = UniversalDialogHelper.createSectionScroll(text, row.getBackground(), scrollSize);
            textPanel.add(sectionScroll);
        } else {
            Dimension visibleTextSize = new Dimension(textWidth, textHeight);
            UniversalDialogHelper.setFixedSize(text, visibleTextSize);
            textPanel.add(text);
        }
        Dimension panelSize = new Dimension(
                textWidth,
                (scrollableText ? UniversalDialogHelper.sectionScrollHeight(textHeight) : textHeight) + headingHeight
        );
        UniversalDialogHelper.setFixedSize(textPanel, panelSize);

        int boxHeight = UniversalDialogHelper.alertHeight(
                scrollableText ? UniversalDialogHelper.sectionScrollHeight(textHeight) : textHeight,
                headingHeight
        );
        Dimension boxSize = new Dimension(alertWidth, boxHeight);
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

    private static int preferredMessageHeight(List<String> sections, int alertWidth) {
        int height = UniversalDialogHelper.bodyHeightWithPadding(0);
        int textWidth = UniversalDialogHelper.messageTextWidth(alertWidth);
        int wrapColumns = UniversalDialogHelper.wrapColumns(textWidth);
        for (String section : sections) {
            String[] parts = headingAndBody(section);
            int headingHeight = parts[0].isEmpty() ? 0 : 22;
            int rows = visibleRows(parts[0], parts[1], wrapColumns);
            int textHeight = Math.max(42, rows * 19);
            boolean scrollableText = isScrollableSection(parts[0]) && wrappedRows(parts[1], wrapColumns) > SCROLLABLE_SECTION_ROWS;
            height += UniversalDialogHelper.alertHeight(
                    scrollableText ? UniversalDialogHelper.sectionScrollHeight(textHeight) : textHeight,
                    headingHeight
            );
        }
        height += Math.max(0, sections.size() - 1) * 10;
        return height;
    }

    private static int visibleRows(String heading, String body, int wrapColumns) {
        int rows = Math.max(1, wrappedRows(body, wrapColumns));
        if (isScrollableSection(heading)) {
            return Math.min(SCROLLABLE_SECTION_ROWS, rows);
        }
        return rows;
    }

    private static boolean isScrollableSection(String heading) {
        return "Skipped rows".equalsIgnoreCase(heading)
                || "Rows to review".equalsIgnoreCase(heading)
                || "Files to review".equalsIgnoreCase(heading);
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
            sections.add("No details were provided.");
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
