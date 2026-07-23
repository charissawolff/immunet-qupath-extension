package org.computational_immunology.ext.ImmuNet.ui;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.QuPathViewerListener;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;

/**
 * Clears cached composite level tiles for regions that were visible before but
 * have scrolled off-screen. QuPath's would eventually remove
 * them anyway, but this crashing when holding many full-resolution tiles at
 * once.
 * Only exists for as long as a slide is open. DatasetSelectorTab creates and registers one on
 * the first slide load, and reuses it for every load after
 * that, rather than one being there whole application.
 */
public class SlideViewerCleaner implements QuPathViewerListener {

    private Shape previousShape;

    @Override
    public void visibleRegionChanged(QuPathViewer viewer, Shape shape) {
        ImageServer<BufferedImage> server = viewer.getServer();
        if (!(server instanceof SparseImageServer sparseServer)) {
            previousShape = shape;
            return;
        }

        if (previousShape != null) {
            double[] downsamples = sparseServer.getMetadata().getPreferredDownsamplesArray();
            double compositeDownsample = Arrays.stream(downsamples).min().orElse(1.0);
            double thumbDownsample = Arrays.stream(downsamples).max().orElse(compositeDownsample);

            //This means that if we load a zoomed in, and then load out, that zoomed in portion will be cached. ONly until we
            // zoom into another region in the slide, does it get removed. I do not know how else to fix this issue.
            if (viewer.getDownsampleFactor() < thumbDownsample) {
                SparseImageServer.SparseImageServerManager manager = sparseServer.getManager();
                for (ImageRegion region : manager.getRegions()) {
                    boolean wasVisible = previousShape.intersects(region.getX(), region.getY(), region.getWidth(), region.getHeight());
                    boolean isVisible = shape.intersects(region.getX(), region.getY(), region.getWidth(), region.getHeight());
                    if (wasVisible && !isVisible) {
                        clearCompositeCache(viewer, sparseServer, region, compositeDownsample);
                    }
                }
            }
        }

        previousShape = shape;
    }

    private void clearCompositeCache(QuPathViewer viewer, SparseImageServer sparseServer,
                                      ImageRegion region, double compositeDownsample) {
        try {
            RegionRequest request = RegionRequest.createInstance(
                    sparseServer.getPath(), compositeDownsample,
                    region.getX(), region.getY(), region.getWidth(), region.getHeight(),
                    viewer.getZPosition(), viewer.getTPosition());
            ImmuNetLog.log("Clearing composite cache for region that left the viewport");
            viewer.getImageRegionStore().clearCacheForRequestOverlap(request);
        } catch (Exception e) {
            ImmuNetLog.error("Could not clear composite cache for region that left the viewport", e);
        }
    }

    /**
     * The previous shape is in the old slide's coordinate space and is meaningless for a new one.
     * Public so DatasetSelectorTab can also call this directly when reusing an existing instance
     * for a newly-selected slide, not just when QuPath itself fires imageDataChanged.
     */
    public void reset() {
        previousShape = null;
    }

    @Override
    public void imageDataChanged(QuPathViewer viewer, ImageData<BufferedImage> imageDataOld, ImageData<BufferedImage> imageDataNew) {
        reset();
    }

    @Override
    public void selectedObjectChanged(QuPathViewer viewer, PathObject pathObjectSelected) {}

    @Override
    public void viewerClosed(QuPathViewer viewer) {}
}
