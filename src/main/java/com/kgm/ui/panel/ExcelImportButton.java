package com.kgm.ui.panel;

import com.kgm.ui.styling.ButtonStateHelper;

import javax.swing.*;
import java.awt.*;

public class ExcelImportButton extends JButton {

    private static final Color EXCEL_GREEN = new Color(22, 163, 74);
    private static final Color HOVER_GREEN = new Color(21, 128, 61);

    public ExcelImportButton(Runnable onClick) {
        super("Excel Services");

        setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);
        setForeground(Color.WHITE);
        setBackground(EXCEL_GREEN);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(120, 32));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        ButtonStateHelper.install(this);

        addActionListener(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!isEnabled()) {
                    return;
                }
                setBackground(HOVER_GREEN);
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!isEnabled()) {
                    return;
                }
                setBackground(EXCEL_GREEN);
                repaint();
            }
        });
    }

    // ================= SAP ROUND STYLE =================
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
    }

    // ================= OPTIONAL WRAPPER (TOP RIGHT) =================
    public static JPanel wrapTopRight(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);
        panel.add(button);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        return panel;
    }
}
