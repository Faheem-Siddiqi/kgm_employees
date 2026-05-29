package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.util.EmployeeBasicFieldUtil;
import java.awt.*;
import java.util.Date;

public final class EmployeeAdditionalDetailsPanelHelper {
    private static final int INPUT_MIN_WIDTH = 260;
    private static final int INPUT_HEIGHT = 34;
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color SECTION_BORDER = new Color(230, 230, 230);
    private static final Color HEADER_TEXT = new Color(60, 60, 60);

    private EmployeeAdditionalDetailsPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    public static JComponent createContent(JComponent content) {
        return EmployeeRegistrationFormPanelHelper.createFormContent(content);
    }

    public static JPanel createRootPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(PAGE_BACKGROUND);
        root.setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(20, 20, 22, 20)
        ));
        return root;
    }

    public static JPanel createSectionPanel() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PAGE_BACKGROUND);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(5),
                new EmptyBorder(10, 10, 10, 10)));
        return section;
    }

    public static JPanel createSectionsContainer() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(PAGE_BACKGROUND);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        return container;
    }

    public static JLabel createEmptyStateLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(EmployeeRegistrationViewHelper.TEXT_SECONDARY);
        label.setBorder(new EmptyBorder(18, 4, 18, 4));
        return label;
    }

    public static JPanel createBreadcrumbPanel() {
        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        breadcrumb.setBackground(PAGE_BACKGROUND);
        breadcrumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        breadcrumb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        breadcrumb.setBorder(new EmptyBorder(0, 0, 4, 0));
        return breadcrumb;
    }

    public static JScrollPane createBreadcrumbScroller(JPanel breadcrumb) {
        JScrollPane scrollPane = new JScrollPane(breadcrumb);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 7));
        scrollPane.setPreferredSize(new Dimension(1, 48));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scrollPane;
    }

    public static JLabel createSearchStatusLabel(String text) {
        return EmployeeDocumentUploadPanelHelper.createUploadedCountLabel(text);
    }

    public static JPanel createSearchHeader(JLabel statusLabel, JPanel searchPanel) {
        JPanel header = EmployeeDocumentUploadPanelHelper.createTopPanel();
        header.add(EmployeeDocumentUploadPanelHelper.createSummaryPanel(statusLabel, null, null));
        header.add(Box.createVerticalStrut(12));
        header.add(searchPanel);
        return header;
    }

    public static JPanel createSearchPanel(JTextField searchField, JButton clearButton, JButton searchButton) {
        return EmployeeDocumentUploadPanelHelper.createSearchPanel(searchField, clearButton, searchButton);
    }

    public static void styleSearchField(JTextField field) {
        EmployeeDocumentUploadPanelHelper.styleSearchField(field);
    }

    public static void styleSearchButton(JButton button) {
        EmployeeDocumentUploadPanelHelper.styleSearchButton(button);
    }

    public static void styleClearButton(JButton button) {
        EmployeeDocumentUploadPanelHelper.styleClearButton(button);
    }

    public static void updateClearButtonState(JButton button, boolean enabled) {
        EmployeeDocumentUploadPanelHelper.updateClearButtonState(button, enabled);
    }

    public static JButton createBreadcrumbLink(String text) {
        JButton link = new JButton(text);
        link.setContentAreaFilled(true);
        link.setBorderPainted(true);
        link.setFocusPainted(false);
        link.setOpaque(true);
        link.setBackground(new Color(248, 250, 252));
        link.setForeground(EmployeeRegistrationViewHelper.PRIMARY);
        link.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EmployeeRegistrationViewHelper.BORDER),
                new EmptyBorder(6, 12, 6, 12)
        ));
        return link;
    }

    public static JLabel createBreadcrumbSeparator() {
        JLabel separator = new JLabel("");
        separator.setPreferredSize(new Dimension(0, 1));
        return separator;
    }

    public static JPanel createReturnToTopPanel(Runnable action) {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        container.setBackground(PAGE_BACKGROUND);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        container.setBorder(new EmptyBorder(14, 0, 0, 0));

        JButton link = createBreadcrumbLink("Return to top");
        link.addActionListener(e -> action.run());
        container.add(link);
        return container;
    }

    public static JLabel createSectionHeader(String title) {
        JLabel header = new JLabel(title);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(HEADER_TEXT);
        return header;
    }

    public static JPanel createGridPanel() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(PAGE_BACKGROUND);
        return grid;
    }

    public static JPanel createFieldPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 3));
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
    }

    public static JLabel createFieldLabel(String text) {
        return EmployeeRegistrationFormPanelHelper.createFieldLabel(text);
    }

    public static JTextField createField(String value) {
        JTextField field = new JTextField(value);
        EmployeeRegistrationFormPanelHelper.styleInput(field);
        applySingleLineMinimum(field);
        return field;
    }

    public static JComboBox<String> createDropdownField(String[] options, String value, boolean allowCustomValue) {
        JComboBox<String> combo = new JComboBox<>(options);
        DropdownFieldSupport.configure(combo, allowCustomValue);
        DropdownFieldSupport.setPlaceholder(combo, EmployeeBasicFieldUtil.dropdownPlaceholder(allowCustomValue));
        EmployeeRegistrationFormPanelHelper.styleInput(combo);
        applyDropdownMinimum(combo);
        if (value != null && !value.isBlank()) {
            DropdownFieldSupport.setValue(combo, value);
        } else if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
        return combo;
    }

    public static com.kgm.ui.component.UniversalDatePicker createDateField(Date value) {
        com.kgm.ui.component.UniversalDatePicker datePicker = new com.kgm.ui.component.UniversalDatePicker(value);
        EmployeeRegistrationFormPanelHelper.styleInput(datePicker);
        applySingleLineMinimum(datePicker);
        return datePicker;
    }

    public static JPanel createGridFiller() {
        JPanel panel = new JPanel();
        panel.setBackground(PAGE_BACKGROUND);
        panel.setMinimumSize(new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT));
        panel.setPreferredSize(new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT));
        return panel;
    }

    private static void applySingleLineMinimum(JComponent input) {
        Dimension minimum = new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT);
        Dimension preferred = input.getPreferredSize();
        int preferredWidth = Math.max(INPUT_MIN_WIDTH, preferred == null ? INPUT_MIN_WIDTH : preferred.width);
        int preferredHeight = Math.max(INPUT_HEIGHT, preferred == null ? INPUT_HEIGHT : preferred.height);
        input.setMinimumSize(minimum);
        input.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
    }

    private static void applyDropdownMinimum(JComboBox<String> combo) {
        applySingleLineMinimum(combo);
        Component editor = combo.getEditor() == null ? null : combo.getEditor().getEditorComponent();
        if (editor instanceof JComponent editorComponent) {
            editorComponent.setMinimumSize(new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT));
            editorComponent.setPreferredSize(new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT));
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SECTION_BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }
}

