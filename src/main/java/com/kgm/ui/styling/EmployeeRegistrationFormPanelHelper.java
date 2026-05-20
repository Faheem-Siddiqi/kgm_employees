package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

public final class EmployeeRegistrationFormPanelHelper {
    public static final int PHOTO_SIZE = 200;
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private static final int TAB_CONTENT_INSET = 28;
    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PHOTO_BORDER = new Color(210, 210, 210);
    private static final Color CARD_BORDER = new Color(220, 220, 220);
    private static final Color FIELD_BORDER = new Color(200, 200, 200);
    private static final Color LABEL_TEXT = new Color(70, 70, 70);
    private static final Color LINK_BLUE = new Color(0, 102, 204);

    private EmployeeRegistrationFormPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(PAGE_BACKGROUND);
    }

    public static JComponent createFormContent(JComponent form) {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(PAGE_BACKGROUND);
        content.setBorder(new EmptyBorder(14, TAB_CONTENT_INSET, 0, TAB_CONTENT_INSET));
        content.add(form, BorderLayout.NORTH);
        return content;
    }

    public static JPanel createFormRoot() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(PAGE_BACKGROUND);
        root.setBorder(new CompoundBorder(
                new RoundedBorder(16),
                new EmptyBorder(24, 24, 24, 24)
        ));
        return root;
    }

    public static JPanel createPhotoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(240, 300));
        panel.setBackground(PAGE_BACKGROUND);
        panel.setBorder(new EmptyBorder(4, 0, 4, 18));
        return panel;
    }

    private static class RoundedPhotoBorder extends AbstractBorder {
    private final int radius;

    RoundedPhotoBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setColor(PHOTO_BORDER);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);

        g2.dispose();
    }

    @Override
public Insets getBorderInsets(Component c) {
    return new Insets(5, 5, 5, 5);
}
}

   public static JLabel createPhotoPreview(String text) {
    JLabel label = new JLabel(text, SwingConstants.CENTER);

    label.setPreferredSize(new Dimension(220, 220));

    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);

    label.setBorder(new CompoundBorder(
            new RoundedPhotoBorder(8),
            new EmptyBorder(5, 5, 5, 5)
    ));

    return label;
}

    public static void styleUploadLabel(JLabel label) {
        label.setForeground(LINK_BLUE);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static JPanel createPhotoInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PAGE_BACKGROUND);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));
        return panel;
    }

    public static JLabel createPhotoInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
          label.setBorder(new EmptyBorder(4, 0, 0, 0));
        return label;
    }

    public static JPanel createRightFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PAGE_BACKGROUND);
        panel.setBorder(new EmptyBorder(0, 8, 0, 0));
        return panel;
    }

    public static void styleAddressArea(JTextArea textArea) {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(INPUT_FONT);
        textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    public static JScrollPane createAddressScrollPane(JTextArea textArea) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(new LineBorder(FIELD_BORDER));
        scrollPane.setPreferredSize(new Dimension(300, 96));
        scrollPane.setMinimumSize(new Dimension(240, 84));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    public static void styleFormField(JPanel panel) {
        panel.setLayout(new BorderLayout(6, 4));
        panel.setBackground(PAGE_BACKGROUND);
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(LABEL_TEXT);
        return label;
    }

    public static void styleInput(JComponent input) {
        input.setFont(INPUT_FONT);
        input.setPreferredSize(new Dimension(300, 34));
        input.setMinimumSize(new Dimension(240, 34));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        input.setBackground(PAGE_BACKGROUND);

        if (input instanceof JScrollPane) {
            input.setPreferredSize(new Dimension(300, 96));
            input.setMinimumSize(new Dimension(240, 84));
            input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            return;
        }

        if (input instanceof JComboBox<?>) {
            JComboBox<?> comboBox = (JComboBox<?>) input;
            styleComboBox(comboBox);
            return;
        }

        if (input instanceof JTextField) {
            input.setBorder(fieldBorder());
        }
    }

    private static CompoundBorder fieldBorder() {
        return new CompoundBorder(
                new LineBorder(FIELD_BORDER),
                new EmptyBorder(6, 8, 6, 8)
        );
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        public Insets getBorderInsets(Component component) {
            return new Insets(10, 10, 10, 10);
        }
    }

    private static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setUI(new BasicComboBoxUI() {
            protected JButton createArrowButton() {
                JButton button = new BasicArrowButton(
                        BasicArrowButton.SOUTH,
                        PAGE_BACKGROUND,
                        FIELD_BORDER,
                        new Color(90, 90, 90),
                        PAGE_BACKGROUND
                );
                button.setBorder(new EmptyBorder(0, 0, 0, 0));
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                return button;
            }

            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(PAGE_BACKGROUND);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        comboBox.setBorder(fieldBorder());
        comboBox.setBackground(PAGE_BACKGROUND);
        comboBox.setOpaque(true);
        comboBox.setFocusable(false);
        comboBox.setRequestFocusEnabled(false);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );
                label.setBorder(new EmptyBorder(0, 8, 0, 8));
                label.setBackground(isSelected ? new Color(235, 244, 255) : PAGE_BACKGROUND);
                label.setForeground(new Color(35, 43, 54));
                return label;
            }
        });
    }

}

