package com.kgm.ui.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class UniversalTextArea extends JScrollPane {
    private static final Color BORDER = new Color(200, 200, 200);
    private static final Color BACKGROUND = Color.WHITE;
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final JTextArea textArea;

    public UniversalTextArea() {
        this("");
    }

    public UniversalTextArea(String value) {
        super(new JTextArea(value == null ? "" : value));
        textArea = (JTextArea) getViewport().getView();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FONT);
        textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        textArea.setBackground(BACKGROUND);

        setBorder(new LineBorder(BORDER));
        setBackground(BACKGROUND);
        getViewport().setBackground(BACKGROUND);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setPreferredSize(new Dimension(300, 96));
        setMinimumSize(new Dimension(240, 84));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String value) {
        textArea.setText(value == null ? "" : value);
    }

    public void setEditable(boolean editable) {
        textArea.setEditable(editable);
    }

    public boolean isEditable() {
        return textArea.isEditable();
    }

    public JTextArea textArea() {
        return textArea;
    }
}
