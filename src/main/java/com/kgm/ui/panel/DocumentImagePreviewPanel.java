package com.kgm.ui.panel;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class DocumentImagePreviewPanel extends JPanel implements Scrollable {
    private static final int PADDING = 18;
    private final BufferedImage image;

    public DocumentImagePreviewPanel(BufferedImage image) {
        this.image = image;
        setBackground(Color.WHITE);
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(760, 520);
        }
        return new Dimension(
                Math.max(760, image.getWidth() + (PADDING * 2)),
                Math.max(520, image.getHeight() + (PADDING * 2))
        );
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null) {
            return;
        }

        double availableWidth = Math.max(1, getWidth() - (PADDING * 2));
        double availableHeight = Math.max(1, getHeight() - (PADDING * 2));
        double scale = Math.min(1.0, Math.min(
                availableWidth / image.getWidth(),
                availableHeight / image.getHeight()
        ));
        int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int x = Math.max(PADDING, (getWidth() - drawWidth) / 2);
        int y = Math.max(PADDING, (getHeight() - drawHeight) / 2);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(image, x, y, drawWidth, drawHeight, null);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(820, 600);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(64, visibleRect.height - 64);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return true;
    }
}
