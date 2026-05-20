package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;

public final class EmployeeDocumentViewPanelHelper {
    private EmployeeDocumentViewPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        EmployeeDocumentUploadPanelHelper.stylePanel(panel);
    }

    public static JPanel createTopPanel() {
        return EmployeeDocumentUploadPanelHelper.createTopPanel();
    }

    public static JLabel createUploadedCountLabel(String text) {
        return EmployeeDocumentUploadPanelHelper.createUploadedCountLabel(text);
    }

    public static JLabel createSizeLabel() {
        return EmployeeDocumentUploadPanelHelper.createSizeLabel();
    }

    public static JPanel createRendererPanel() {
        return EmployeeDocumentUploadPanelHelper.createRendererPanel();
    }

    public static void styleRendererPanel(JPanel panel) {
        EmployeeDocumentUploadPanelHelper.styleRendererPanel(panel);
    }

    public static JPanel createEditorPanel() {
        return EmployeeDocumentUploadPanelHelper.createEditorPanel();
    }

    public static JButton createActionLink(String text) {
        return EmployeeDocumentUploadPanelHelper.createActionLink(text);
    }

    public static void styleViewLink(JButton button, boolean uploaded) {
        EmployeeDocumentUploadPanelHelper.styleViewLink(button, uploaded);
    }

    public static void styleActionCell(JPanel panel, boolean selected) {
        EmployeeDocumentUploadPanelHelper.styleActionCell(panel, selected);
    }

    public static JPanel createActionButtonsPanel() {
        return EmployeeDocumentUploadPanelHelper.createActionButtonsPanel();
    }

    public static GridBagConstraints actionCellConstraints() {
        return EmployeeDocumentUploadPanelHelper.actionCellConstraints();
    }

    public static void stylePreviewFrame(JFrame frame, Component relativeTo) {
        EmployeeDocumentUploadPanelHelper.stylePreviewFrame(frame, relativeTo);
    }
}

