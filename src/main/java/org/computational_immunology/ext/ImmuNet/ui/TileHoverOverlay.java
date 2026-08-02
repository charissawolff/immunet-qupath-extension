package org.computational_immunology.ext.ImmuNet.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;

import qupath.lib.color.ColorToolsAwt;
import qupath.lib.gui.viewer.OverlayOptions;
import qupath.lib.gui.viewer.PathObjectPainter;
import qupath.lib.gui.viewer.overlays.AbstractOverlay;
import qupath.lib.images.ImageData;
import qupath.lib.regions.ImageRegion;

public class TileHoverOverlay extends AbstractOverlay {

    private final SelectedDataStore selectedDataStore;
    private volatile TileMetadata hoveredTileMetadata;

    public TileHoverOverlay(OverlayOptions options, SelectedDataStore selectedDataStore) {
        super(options);
        this.selectedDataStore = selectedDataStore;
    }

    // returns false if this tile was already the hovered one, so the caller only repaints on an actual change
    public boolean setHoveredTileMetadata(TileMetadata tile) {
        if (Objects.equals(this.hoveredTileMetadata, tile)) {
            return false;
        }
        this.hoveredTileMetadata = tile;
        return true;
    }

    @Override
    public void paintOverlay(Graphics2D g2d, ImageRegion imageRegion, double downsampleFactor, ImageData<BufferedImage> imageData, boolean paintCompletely) {
        if (!isVisible() || imageData == null) {
            return;
        }

        TileMetadata hovered = hoveredTileMetadata;
        TileMetadata selected = selectedDataStore.getSelectedTile();
        if (hovered == null && selected == null) {
            return;
        }

        Graphics2D g = (Graphics2D) g2d.create();

        Color wash = ColorToolsAwt.getColorWithOpacity(Color.WHITE, 0.2);
        if (selected != null) {
            //if i select a tile, then i want to see it highlighted with a red border in the overlay
            Shape selectedShape = tileShape(selected);
            Stroke stroke = PathObjectPainter.getCachedStroke(Math.max(downsampleFactor, 1) * 2);
            PathObjectPainter.paintShape(selectedShape, g, getPreferredOverlayColor(), stroke, wash);
        }
        if (hovered != null && !hovered.equals(selected)) {
            // if i hover over a tile, then i want to see it highlighted, so i can see where it is on the slide
            Shape hoveredShape = tileShape(hovered);
            PathObjectPainter.paintShape(hoveredShape, g, null, null, wash);
        }

        g.dispose();
    }

    private static Shape tileShape(TileMetadata tile) {
        return new Rectangle2D.Double(tile.getX(), tile.getY(), tile.getWidth(), tile.getHeight());
    }
}
