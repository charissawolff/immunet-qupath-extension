package org.computational_immunology.ext.ImmuNet.ui.overlays;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.computational_immunology.ext.ImmuNet.core.SelectedSlide;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.gui.viewer.QuPathViewer;

public class TileHoverController extends OverlayController {
    private final TileHoverOverlay overlay;
    private Consumer<TileMetadata> onTileClicked;
    private final ObservableBooleanValue enabled;
    private ReadOnlyObjectProperty<SelectedSlide> selectedSlideProperty;

    private final EventHandler<MouseEvent> mouseMovedHandler = this::handleMouseMoved;
    private final EventHandler<MouseEvent> mouseExitedHandler = this::handleMouseExited;
    private final EventHandler<MouseEvent> mouseClickedHandler = this::handleMouseClicked;
    private final ChangeListener<SelectedSlide>slideChangeListener;

    public TileHoverController(ReadOnlyObjectProperty<SelectedSlide> selectedSlideProperty, TileHoverOverlay overlay, ObservableBooleanValue enabled) {
        super(overlay);
        this.selectedSlideProperty = selectedSlideProperty;
        this.overlay = overlay;
        slideChangeListener = (obs, oldSlide, newSlide) -> {
            overlay.clearSelected();
            overlay.clearHovered();
        };
        this.enabled = enabled;
        applyEnabled(enabled.get());
        this.enabled.addListener((obs, wasEnabled, isEnabled) -> applyEnabled(isEnabled));
    }

    private void applyEnabled(boolean isEnabled) {
        if (isEnabled) {
            setShow();
        } else {
            setDontShow();
        }
    }

    public void setDontShow() {
        overlay.setOpacity(0.0);
    }

    public void setShow() {
        overlay.setOpacity(1.0);
    }

    protected void onAttach(QuPathViewer viewer) {
        viewer.getView().addEventHandler(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        viewer.getView().addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedHandler);
        viewer.getView().addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedHandler);
        selectedSlideProperty.addListener(slideChangeListener);
    }

    protected void onDetach(QuPathViewer viewer) {
        viewer.getView().removeEventHandler(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        viewer.getView().removeEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedHandler);
        viewer.getView().removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedHandler);
        selectedSlideProperty.removeListener(slideChangeListener);
    }

    private void handleMouseMoved(MouseEvent e) {
        TileMetadata tile = tileAtEvent(e);
        if (tile != null) {
            overlay.setHovered(tile.getPixelX(), tile.getPixelY(), tile.getPixelWidth(), tile.getPixelHeight());
        } else {
            overlay.clearHovered();
        }
        getViewer().repaint();
    }

    private void handleMouseExited(MouseEvent e) {
        overlay.clearHovered();
        getViewer().repaint();
    }

    private void handleMouseClicked(MouseEvent e) {
        if (e.isConsumed() || !e.isStillSincePress() || e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        SelectedSlide slide = selectedSlideProperty.get();
        TileMetadata tile = tileAtEvent(e);
        if (slide == null || tile == null) {
            return;
        }
        if (onTileClicked != null){
            onTileClicked.accept(tile);
        }
        overlay.setSelected(tile.getPixelX(), tile.getPixelY(), tile.getPixelWidth(), tile.getPixelHeight());
        getViewer().repaint();
    }

    private TileMetadata tileAtEvent(MouseEvent e) {
        SelectedSlide slide = selectedSlideProperty.get();
        if (slide == null) {
            return null;
        }
        Point2D imagePoint = getViewer().componentPointToImagePoint(e.getX(), e.getY(), null, false);
        return slide.tileAt(imagePoint.getX(), imagePoint.getY()).orElse(null);
    }


    public void setOnTileClicked(Consumer<TileMetadata> onTileClicked ){
        this.onTileClicked = onTileClicked;
    }
}