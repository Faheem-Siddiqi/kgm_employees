package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;

public final class OtherDetailsPanelHelper {
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color SECTION_BORDER = new Color(230, 230, 230);
    private static final Color HEADER_TEXT = new Color(60, 60, 60);

    private OtherDetailsPanelHelper() {
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
        return FormPanelHelper.createFormContent(content);
    }

    public static JPanel createRootPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(PAGE_BACKGROUND);
        root.setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(24, 24, 24, 24)
        ));
        return root;
    }

    public static JPanel createSectionPanel() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PAGE_BACKGROUND);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(5),
                new EmptyBorder(12, 12, 12, 12)));
        return section;
    }

    public static JPanel createBreadcrumbPanel() {
        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        breadcrumb.setBackground(PAGE_BACKGROUND);
        breadcrumb.setAlignmentX(Component.LEFT_ALIGNMENT);
        breadcrumb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        breadcrumb.setBorder(new EmptyBorder(0, 0, 16, 0));
        return breadcrumb;
    }

    public static JButton createBreadcrumbLink(String text) {
        JButton link = new JButton(text);
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        link.setOpaque(false);
        link.setForeground(EmployeeInductionHelper.PRIMARY);
        link.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setBorder(new EmptyBorder(4, 0, 4, 0));
        return link;
    }

    public static JLabel createBreadcrumbSeparator() {
        JLabel separator = new JLabel("/");
        separator.setForeground(EmployeeInductionHelper.PRIMARY);
        separator.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        separator.setBorder(new EmptyBorder(4, 2, 4, 2));
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
        return FormPanelHelper.createFieldLabel(text);
    }

    public static JTextField createField(String value) {
        JTextField field = new JTextField(value);
        FormPanelHelper.styleInput(field);
        return field;
    }

    public static com.kgm.ui.component.UniversalDatePicker createDateField(Date value) {
        com.kgm.ui.component.UniversalDatePicker datePicker = new com.kgm.ui.component.UniversalDatePicker(value);
        FormPanelHelper.styleInput(datePicker);
        return datePicker;
    }

    public static JPanel createGridFiller() {
        JPanel panel = new JPanel();
        panel.setBackground(PAGE_BACKGROUND);
        return panel;
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
