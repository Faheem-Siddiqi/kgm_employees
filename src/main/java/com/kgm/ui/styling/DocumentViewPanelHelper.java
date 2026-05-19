package com.kgm.ui.styling;

import javax.swing.*;
import java.awt.*;

public final class DocumentViewPanelHelper {
    private DocumentViewPanelHelper() {
    }

    public static void stylePanel(JPanel panel) {
        DocumentPanelHelper.stylePanel(panel);
    }

    public static JPanel createTopPanel() {
        return DocumentPanelHelper.createTopPanel();
    }

    public static JLabel createUploadedCountLabel(String text) {
        return DocumentPanelHelper.createUploadedCountLabel(text);
    }

    public static JLabel createSizeLabel() {
        return DocumentPanelHelper.createSizeLabel();
    }

    public static JPanel createRendererPanel() {
        return DocumentPanelHelper.createRendererPanel();
    }

    public static void styleRendererPanel(JPanel panel) {
        DocumentPanelHelper.styleRendererPanel(panel);
    }

    public static JPanel createEditorPanel() {
        return DocumentPanelHelper.createEditorPanel();
    }

    public static JButton createActionLink(String text) {
        return DocumentPanelHelper.createActionLink(text);
    }

    public static void styleViewLink(JButton button, boolean uploaded) {
        DocumentPanelHelper.styleViewLink(button, uploaded);
    }

    public static void styleActionCell(JPanel panel, boolean selected) {
        DocumentPanelHelper.styleActionCell(panel, selected);
    }

    public static JPanel createActionButtonsPanel() {
        return DocumentPanelHelper.createActionButtonsPanel();
    }

    public static GridBagConstraints actionCellConstraints() {
        return DocumentPanelHelper.actionCellConstraints();
    }

    public static void stylePreviewFrame(JFrame frame, Component relativeTo) {
        DocumentPanelHelper.stylePreviewFrame(frame, relativeTo);
    }
}
