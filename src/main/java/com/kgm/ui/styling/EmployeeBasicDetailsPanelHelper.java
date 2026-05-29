package com.kgm.ui.styling;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class EmployeeBasicDetailsPanelHelper {
    public static final int PHOTO_SIZE = 200;
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final int INPUT_MIN_WIDTH = 240;
    private static final int INPUT_HEIGHT = 34;

    private static final Color PAGE_BACKGROUND = Color.WHITE;
    private static final Color PHOTO_BORDER = new Color(210, 210, 210);
    private static final Color FORM_BORDER = new Color(235, 235, 235);
    private static final Color LABEL_TEXT = new Color(70, 70, 70);
    private static final Color LINK_BLUE = new Color(0, 102, 204);

    private EmployeeBasicDetailsPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        EmployeeRegistrationFormPanelHelper.stylePanel(panel);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(PAGE_BACKGROUND);
    }

    public static JComponent createFormContent(JComponent form) {
        return EmployeeRegistrationFormPanelHelper.createFormContent(form);
    }

    public static JPanel createFormRoot() {
        return EmployeeRegistrationFormPanelHelper.createFormRoot();
    }

    public static JPanel createPhotoPanel() {
        return EmployeeRegistrationFormPanelHelper.createPhotoPanel();
    }

    public static JLabel createPhotoPreview(String text) {
        return EmployeeRegistrationFormPanelHelper.createPhotoPreview(text);
    }

    public static void styleUploadLabel(JLabel label) {
        EmployeeRegistrationFormPanelHelper.styleUploadLabel(label);
    }

    public static JPanel createPhotoInfoPanel() {
        return EmployeeRegistrationFormPanelHelper.createPhotoInfoPanel();
    }

    public static JLabel createPhotoInfoLabel(String text) {
        return EmployeeRegistrationFormPanelHelper.createPhotoInfoLabel(text);
    }

    public static JPanel createRightFormPanel() {
        return EmployeeRegistrationFormPanelHelper.createRightFormPanel();
    }

    public static void styleTextArea(JTextArea textArea) {
        EmployeeRegistrationFormPanelHelper.styleAddressArea(textArea);
    }

    public static JScrollPane createTextAreaScrollPane(JTextArea textArea) {
        return EmployeeRegistrationFormPanelHelper.createAddressScrollPane(textArea);
    }

    public static void styleFormField(JPanel panel) {
        EmployeeRegistrationFormPanelHelper.styleFormField(panel);
    }

    public static JLabel createFieldLabel(String text) {
        return EmployeeRegistrationFormPanelHelper.createFieldLabel(text);
    }

    public static void styleInput(JComponent input) {
        EmployeeRegistrationFormPanelHelper.styleInput(input);
    }

    public static JPanel createGridFiller() {
        JPanel panel = new JPanel();
        panel.setBackground(PAGE_BACKGROUND);
        panel.setMinimumSize(new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT));
        panel.setPreferredSize(new Dimension(INPUT_MIN_WIDTH, INPUT_HEIGHT));
        return panel;
    }
}

