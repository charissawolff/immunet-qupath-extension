package org.computational_immunology.ext.ImmuNet.ui.overlays;

import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.overlays.PathOverlay;

public abstract class OverlayController {
    private QuPathViewer viewer;
    private final PathOverlay overlay;

    protected OverlayController(PathOverlay overlay) {
        this.overlay = overlay;
    }

    public PathOverlay getOverlay() {
        return overlay;
    }

    public void attachAndRequestRepaint(QuPathViewer viewer) {
        attachTo(viewer);
        getViewer().repaint();
    }

    public final void attachTo(QuPathViewer viewer) {
        if (this.viewer == viewer) return;
        detach();
        this.viewer = viewer;
        viewer.getCustomOverlayLayers().add(overlay);
        onAttach(viewer);
    }

    public final void detach() {
        if (viewer == null) return;
        onDetach(viewer);
        viewer.getCustomOverlayLayers().remove(overlay);
        viewer = null;
    }

    protected QuPathViewer getViewer() { return viewer; }
    protected void onAttach(QuPathViewer viewer) {}
    protected void onDetach(QuPathViewer viewer) {}
}