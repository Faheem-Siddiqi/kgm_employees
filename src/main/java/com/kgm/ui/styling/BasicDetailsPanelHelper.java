package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class BasicDetailsPanelHelper {
    public static final int PHOTO_SIZE = 200;
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PHOTO_BORDER = new Color(210, 210, 210);
    private static final Color FORM_BORDER = new Color(235, 235, 235);
    private static final Color LABEL_TEXT = new Color(70, 70, 70);
    private static final Color LINK_BLUE = new Color(0, 102, 204);

    private BasicDetailsPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        FormPanelHelper.stylePanel(panel);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
    }

    public static JComponent createFormContent(JComponent form) {
        return FormPanelHelper.createFormContent(form);
    }

    public static JPanel createFormRoot() {
        return FormPanelHelper.createFormRoot();
    }

    public static JPanel createPhotoPanel() {
        return FormPanelHelper.createPhotoPanel();
    }

    public static JLabel createPhotoPreview(String text) {
        return FormPanelHelper.createPhotoPreview(text);
    }

    public static void styleUploadLabel(JLabel label) {
        FormPanelHelper.styleUploadLabel(label);
    }

    public static JPanel createPhotoInfoPanel() {
        return FormPanelHelper.createPhotoInfoPanel();
    }

    public static JLabel createPhotoInfoLabel(String text) {
        return FormPanelHelper.createPhotoInfoLabel(text);
    }

    public static JPanel createRightFormPanel() {
        return FormPanelHelper.createRightFormPanel();
    }

    public static void styleTextArea(JTextArea textArea) {
        FormPanelHelper.styleAddressArea(textArea);
    }

    public static JScrollPane createTextAreaScrollPane(JTextArea textArea) {
        return FormPanelHelper.createAddressScrollPane(textArea);
    }

    public static void styleFormField(JPanel panel) {
        FormPanelHelper.styleFormField(panel);
    }

    public static JLabel createFieldLabel(String text) {
        return FormPanelHelper.createFieldLabel(text);
    }

    public static void styleInput(JComponent input) {
        FormPanelHelper.styleInput(input);
    }
}
