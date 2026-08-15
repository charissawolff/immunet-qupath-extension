package org.computational_immunology.ext.ImmuNet.ui.overlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import qupath.lib.color.ColorToolsAwt;
import qupath.lib.gui.viewer.OverlayOptions;
import qupath.lib.gui.viewer.PathObjectPainter;
import qupath.lib.gui.viewer.overlays.AbstractOverlay;
import qupath.lib.images.ImageData;
import qupath.lib.regions.ImageRegion;

public class TileHoverOverlay extends AbstractOverlay {

    private final ObjectProperty<Rectangle2D> hoveredBounds = new SimpleObjectProperty<>();
    private final ObjectProperty<Rectangle2D> selectedBounds = new SimpleObjectProperty<>();

    public TileHoverOverlay(OverlayOptions options) {
        super(options);
    }

    public void setHovered(double x, double y, double width, double height) {
        hoveredBounds.set(new Rectangle2D.Double(x, y, width, height));
    }

    public void clearHovered() {
        hoveredBounds.set(null);
    }

    public void setSelected(double x, double y, double width, double height) {
        selectedBounds.set(new Rectangle2D.Double(x, y, width, height));
    }

    public void clearSelected() {
        selectedBounds.set(null);
    }

    @Override
    public void paintOverlay(Graphics2D g2d, ImageRegion imageRegion, double downsampleFactor, ImageData<BufferedImage> imageData, boolean paintCompletely) {
        if (!isVisible() || imageData == null) {
            return;
        }

        Graphics2D g = (Graphics2D) g2d.create();
        Color wash = ColorToolsAwt.getColorWithOpacity(Color.WHITE, 0.2);

        Rectangle2D selected = selectedBounds.get();
        if (selected != null) {
            // if i select a tile, then i want to see it highlighted with a border in the overlay
            Stroke stroke = PathObjectPainter.getCachedStroke(Math.max(downsampleFactor, 1) * 2);
            PathObjectPainter.paintShape(selected, g, getPreferredOverlayColor(), stroke, wash);
        }

        Rectangle2D hovered = hoveredBounds.get();
        if (hovered != null) {
            // if i hover over a tile, then i want to see it highlighted, so i can see where it is on the slide
            PathObjectPainter.paintShape(hovered, g, null, null, wash);
        }

        g.dispose();
    }
}