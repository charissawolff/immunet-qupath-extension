package org.computational_immunology.ext.ImmuNet.ui;

import java.awt.geom.Point2D;

import javafx.beans.value.ObservableBooleanValue;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SelectedSlide;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;

import qupath.lib.gui.viewer.QuPathViewer;

public class TileHoverController {

    private final SelectedDataStore selectedDataStore;
    private final TileHoverOverlay overlay;
    private final ObservableBooleanValue enabled;

    private final EventHandler<MouseEvent> mouseMovedHandler = this::handleMouseMoved;
    private final EventHandler<MouseEvent> mouseExitedHandler = this::handleMouseExited;
    private final EventHandler<MouseEvent> mouseClickedHandler = this::handleMouseClicked;

    private QuPathViewer viewer;

    public TileHoverController(SelectedDataStore selectedDataStore, TileHoverOverlay overlay) {
        this.selectedDataStore = selectedDataStore;
        this.overlay = overlay;
        this.enabled = enabled;
        applyEnabled(enabled.get());
        this.enabled.addListener((obs, wasEnabled, isEnabled) -> applyEnabled(isEnabled));
    }

    private void applyEnabled(boolean isEnabled) {
        if (isEnabled) {
            setShow();
        } else {
            setDontShow();
            overlay.setHoveredTileMetadata(null);
            if (viewer != null) {
                viewer.repaint();
            }
        }
    }

    /**
     * Called directly by whatever just finished loading a slide
     * attaches to the viewer if needed, clears any stale hover left over from the previous
     * slide, and replaces the selected slide (which itself clears the stale tile selection).
     */
    public void setSlide(SelectedSlide slide, QuPathViewer viewer) {
        //todo: why is it updating the datastore with a slide? remove this
        attachTo(viewer);
        overlay.setHoveredTileMetadata(null);
        selectedDataStore.setSelectedSlide(slide);
        viewer.repaint();
    }

    public void setDontShow(){
        overlay.setOpacity(0.0);
    }

    public void setShow(){
        overlay.setOpacity(1.0);
    }

    public void attachTo(QuPathViewer viewer) {
        if (this.viewer == viewer) {
            return;
        }
        detach();
        this.viewer = viewer;
        viewer.getCustomOverlayLayers().add(overlay);
        viewer.getView().addEventHandler(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        viewer.getView().addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedHandler);
        viewer.getView().addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedHandler);
    }

    public void detach() {
        if (viewer == null) {
            return;
        }
        viewer.getCustomOverlayLayers().remove(overlay);
        viewer.getView().removeEventHandler(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        viewer.getView().removeEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedHandler);
        viewer.getView().removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedHandler);
        viewer = null;
    }

    private void handleMouseMoved(MouseEvent e) {
        TileMetadata tile = tileAtEvent(e);
        if (overlay.setHoveredTileMetadata(tile)) {
            viewer.repaint();
        }
    }

    private void handleMouseExited(MouseEvent e) {
        if (overlay.setHoveredTileMetadata(null)) {
            viewer.repaint();
        }
    }

    private void handleMouseClicked(MouseEvent e) {
        if (e.isConsumed() || !e.isStillSincePress() || e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        SelectedSlide slide = selectedDataStore.getSelectedSlide();
        TileMetadata tile = tileAtEvent(e);
        if (slide == null || tile == null) {
            return;
        }
        selectedDataStore.setSelectedTile(tile);
        viewer.repaint();
        ImmuNetLog.log("Selected tile {} in {}/{}", tile.getCode(), slide.getDatasetName(), slide.getSlideName());
    }

    private TileMetadata tileAtEvent(MouseEvent e) {
        SelectedSlide slide = selectedDataStore.getSelectedSlide();
        if (slide == null) {
            return null;
        }
        Point2D imagePoint = viewer.componentPointToImagePoint(e.getX(), e.getY(), null, false);
        return slide.tileAt(imagePoint.getX(), imagePoint.getY()).orElse(null);
    }
}
