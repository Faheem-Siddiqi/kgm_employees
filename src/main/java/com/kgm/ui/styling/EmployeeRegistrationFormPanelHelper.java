package com.kgm.ui.styling;

import com.kgm.ui.component.DropdownFieldSupport;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

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
                new EmptyBorder(18, 18, 18, 18)
        ));
        return root;
    }

    public static JPanel createPhotoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(218, 278));
        panel.setBackground(PAGE_BACKGROUND);
        panel.setBorder(new EmptyBorder(2, 0, 2, 12));
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

   public static PhotoPreviewLabel createPhotoPreview(String text) {
    PhotoPreviewLabel label = new PhotoPreviewLabel(text);
    Dimension size = new Dimension(PHOTO_SIZE, PHOTO_SIZE);
    label.setPreferredSize(size);
    label.setMinimumSize(size);
    label.setMaximumSize(size);
    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        panel.setBorder(new EmptyBorder(8, 0, 0, 0));
        return panel;
    }

    public static JLabel createPhotoInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setBorder(new EmptyBorder(4, 0, 0, 0));
        return label;
    }

    public static final class PhotoPreviewLabel extends JLabel {
        private static final Color PHOTO_BACKGROUND = new Color(248, 250, 252);
        private static final Color PLACEHOLDER_ICON = new Color(148, 163, 184);
        private static final Color PLACEHOLDER_TEXT = new Color(100, 116, 139);
        private BufferedImage image;
        private String placeholder;

        private PhotoPreviewLabel(String placeholder) {
            this.placeholder = cleanPlaceholder(placeholder);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
            setForeground(PLACEHOLDER_TEXT);
            setBorder(new RoundedPhotoBorder(8));
            setToolTipText("Choose employee photo");
        }

        public void setPreviewImage(BufferedImage image) {
            this.image = image;
            super.setText("");
            setIcon(null);
            repaint();
        }

        public void clearPreviewImage(String placeholder) {
            this.image = null;
            this.placeholder = cleanPlaceholder(placeholder);
            super.setText("");
            setIcon(null);
            repaint();
        }

        @Override
        public void setText(String text) {
            if (text != null && !text.isBlank()) {
                placeholder = cleanPlaceholder(text);
            }
            super.setText("");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Shape clip = new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 8, 8);
            g2.setColor(PHOTO_BACKGROUND);
            g2.fill(clip);
            g2.setClip(clip);
            if (image == null) {
                drawPlaceholder(g2, width, height);
            } else {
                drawCoverImage(g2, width, height);
            }
            g2.dispose();
        }

        private void drawCoverImage(Graphics2D g2, int width, int height) {
            double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
            int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int x = (width - drawWidth) / 2;
            int y = (height - drawHeight) / 2;
            g2.drawImage(image, x, y, drawWidth, drawHeight, null);
        }

        private void drawPlaceholder(Graphics2D g2, int width, int height) {
            int centerX = width / 2;
            int centerY = height / 2 - 8;
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(PLACEHOLDER_ICON);
            g2.drawOval(centerX - 20, centerY - 32, 40, 40);
            g2.drawRoundRect(centerX - 42, centerY + 8, 84, 46, 28, 28);

            String text = placeholder == null || placeholder.isBlank() ? "Photo" : placeholder;
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int textX = centerX - metrics.stringWidth(text) / 2;
            g2.setFont(getFont());
            g2.setColor(PLACEHOLDER_TEXT);
            g2.drawString(text, Math.max(8, textX), centerY + 76);
        }

        private static String cleanPlaceholder(String value) {
            String clean = value == null
                    ? ""
                    : value.replaceAll("<[^>]*>", "").replace("*", "").trim();
            return clean.isBlank() ? "Photo" : clean;
        }
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
        comboBox.setFocusable(comboBox.isEditable());
        comboBox.setRequestFocusEnabled(comboBox.isEditable());
        if (comboBox.isEditable()
                && comboBox.getEditor().getEditorComponent() instanceof JComponent editor) {
            editor.setFocusable(true);
            editor.setRequestFocusEnabled(true);
            editor.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            editor.setBackground(PAGE_BACKGROUND);
        }
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
        if (comboBox.isEditable()) {
            installEditableDropdownSupport(comboBox);
        }
    }

    @SuppressWarnings("unchecked")
    private static void installEditableDropdownSupport(JComboBox<?> comboBox) {
        DropdownFieldSupport.configure((JComboBox<String>) comboBox, true);
    }

}

