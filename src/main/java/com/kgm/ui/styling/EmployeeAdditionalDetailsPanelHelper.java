package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import com.kgm.ui.component.DropdownFieldSupport;
import com.kgm.util.EmployeeBasicFieldUtil;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;

public final class EmployeeAdditionalDetailsPanelHelper {
    private static final int INPUT_MIN_WIDTH = 260;
    private static final int INPUT_HEIGHT = 34;
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color SECTION_BORDER = new Color(226, 232, 240);
    private static final Color HEADER_TEXT = new Color(2, 8, 23);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color CHIP_BG = new Color(37, 99, 235);
    private static final Color CHIP_BG_HOVER = new Color(29, 78, 216);
    private static final Color CHIP_BG_ACTIVE = new Color(30, 64, 175);
    private static final Color CHIP_TEXT = Color.WHITE;
    private static final Color CHIP_TEXT_ACTIVE = Color.WHITE;
    private static final Color CHIP_BORDER = new Color(37, 99, 235);
    private static final Color CHIP_BORDER_ACTIVE = new Color(30, 64, 175);
    private static final String CHIP_ACTIVE_KEY = "kgm.breadcrumb.active";
    private static final String CHIP_BASE_BG_KEY = "kgm.chip.baseBg";
    private static final String CHIP_HOVER_BG_KEY = "kgm.chip.hoverBg";
    private static final String CHIP_BORDER_KEY = "kgm.chip.border";
    private static final int CHIP_RADIUS = 2;
    private static final int CHIP_HEIGHT = 28;
    private static final int CHIP_MIN_WIDTH = 56;
    private static final int CHIP_HORIZONTAL_PADDING = 12;

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
                new RoundedBorder(8),
                new EmptyBorder(16, 16, 18, 16)
        ));
        return root;
    }

    public static JPanel createSectionPanel() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PAGE_BACKGROUND);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8),
                new EmptyBorder(14, 14, 14, 14)));
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
        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        breadcrumb.setBackground(PAGE_BACKGROUND);
        breadcrumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        breadcrumb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        breadcrumb.setBorder(new EmptyBorder(0, 0, 8, 0));
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
        HomeStatsChartHelper.styleHorizontalScrollBar(scrollPane.getHorizontalScrollBar());
        scrollPane.setPreferredSize(new Dimension(1, 56));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scrollPane;
    }

    public static JLabel createSearchStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    public static JPanel createSearchHeader(JLabel statusLabel, JPanel searchPanel) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(true);
        header.setBackground(PAGE_BACKGROUND);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(statusLabel);
        header.add(Box.createVerticalStrut(12));
        header.add(searchPanel);
        return header;
    }

    public static JPanel createSearchPanel(JTextField searchField, JButton clearButton, JButton searchButton) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(PAGE_BACKGROUND);
        searchBox.setPreferredSize(new Dimension(360, 36));
        searchBox.setMinimumSize(new Dimension(260, 36));
        searchBox.setBorder(new CompoundBorder(
                new RoundedBorder(8),
                new EmptyBorder(0, 10, 0, 4)
        ));
        searchBox.add(searchField, BorderLayout.CENTER);
        searchBox.add(clearButton, BorderLayout.EAST);

        row.add(searchBox);
        if (searchButton != null) {
            row.add(Box.createHorizontalStrut(10));
            row.add(searchButton);
        }
        return row;
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
        JButton link = new RoundedChipButton(text, CHIP_RADIUS);
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        link.setOpaque(false);
        link.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setBorder(new EmptyBorder(5, CHIP_HORIZONTAL_PADDING, 5, CHIP_HORIZONTAL_PADDING));
        link.setMargin(new Insets(0, 0, 0, 0));
        link.setHorizontalAlignment(SwingConstants.CENTER);
        link.setVerticalAlignment(SwingConstants.CENTER);
        setChipVisual(link, CHIP_BG, CHIP_TEXT, CHIP_BORDER);
        link.putClientProperty(CHIP_BASE_BG_KEY, CHIP_BG);
        link.putClientProperty(CHIP_HOVER_BG_KEY, CHIP_BG_HOVER);
        refreshChipSize(link);
        installChipHover(link);
        return link;
    }

    public static JButton createMissingFieldsCta(String text) {
        return createBreadcrumbLink(text);
    }

    public static void refreshChipSize(JButton button) {
        if (button == null) {
            return;
        }
        int width = Math.max(
                CHIP_MIN_WIDTH,
                button.getFontMetrics(button.getFont()).stringWidth(button.getText()) + (CHIP_HORIZONTAL_PADDING * 2) + 4
        );
        button.setPreferredSize(new Dimension(width, CHIP_HEIGHT));
        button.setMinimumSize(new Dimension(Math.min(width, CHIP_MIN_WIDTH), CHIP_HEIGHT));
    }

    public static void setBreadcrumbActive(JButton button, boolean active) {
        if (button == null) {
            return;
        }
        button.putClientProperty(CHIP_ACTIVE_KEY, active);
        if (active) {
            setChipVisual(button, CHIP_BG_ACTIVE, CHIP_TEXT_ACTIVE, CHIP_BORDER_ACTIVE);
        } else {
            setChipVisual(button, CHIP_BG, CHIP_TEXT, CHIP_BORDER);
        }
        button.setFont(new Font("Segoe UI Semibold", active ? Font.BOLD : Font.PLAIN, 12));
        button.setBorder(new EmptyBorder(5, CHIP_HORIZONTAL_PADDING, 5, CHIP_HORIZONTAL_PADDING));
        refreshChipSize(button);
        button.repaint();
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
        header.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
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

    private static void installChipHover(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                if (!button.isEnabled() || isActive(button)) {
                    return;
                }
                Object hover = button.getClientProperty(CHIP_HOVER_BG_KEY);
                if (hover instanceof Color color) {
                    button.setBackground(color);
                    button.repaint();
                }
            }

            public void mouseExited(MouseEvent event) {
                if (!button.isEnabled() || isActive(button)) {
                    return;
                }
                Object base = button.getClientProperty(CHIP_BASE_BG_KEY);
                if (base instanceof Color color) {
                    button.setBackground(color);
                    button.repaint();
                }
            }
        });
    }

    private static boolean isActive(JButton button) {
        return Boolean.TRUE.equals(button.getClientProperty(CHIP_ACTIVE_KEY));
    }

    private static void setChipVisual(JButton button, Color background, Color foreground, Color border) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.putClientProperty(CHIP_BORDER_KEY, border);
    }

    private static class RoundedChipButton extends JButton {
        private final int radius;

        RoundedChipButton(String text, int radius) {
            super(text);
            this.radius = radius;
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            Object border = getClientProperty(CHIP_BORDER_KEY);
            if (border instanceof Color color) {
                g2.setColor(color);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(graphics);
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
            return new Insets(1, 1, 1, 1);
        }
    }

}

