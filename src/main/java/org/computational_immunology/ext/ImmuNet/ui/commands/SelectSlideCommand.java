package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SlideImageServer;
import org.computational_immunology.ext.ImmuNet.core.TileImageServer;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.fx.utils.FXUtils;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.SparseImageServer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Captures the selected dataset/slide once at click time, then loads it in a cancellable
 * background Task.
 *
 * <p>Callers that may supersede this command (e.g. selecting a different slide) should hold onto
 * {@code getTask()} and call {@code cancel()} on it before starting a new one.
 */

public class SelectSlideCommand extends AbstractAsyncCommand<SparseImageServer> {

    private final String datasetName;
    private final String slideName;
    private final ImageRequestHandler imageRequestHandler;
    private final double compositeSwitchDownsample;
    private volatile ExecutorService prefetchExecutor;
    private List<TileMetadata> tilesMetadata;

    public SelectSlideCommand(String datasetName, String slideName, double compositeSwitchDownsample, ImageRequestHandler imageRequestHandler) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.compositeSwitchDownsample = compositeSwitchDownsample;
        this.imageRequestHandler = imageRequestHandler;
    }

    @Override
    protected SparseImageServer execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Fetching slide metadata...");
        tilesMetadata = imageRequestHandler.getAllTileMetadatas(datasetName, slideName);
        SparseImageServer sparseServer = SlideImageServer.build(tilesMetadata, datasetName, slideName, compositeSwitchDownsample, imageRequestHandler);
        List<TileImageServer> allThumbServers = SlideImageServer.getThumbServers(sparseServer);

        if (task.isCancelled()) {
            // check if the task was cancelled before starting the executor, to avoid that the user cancels 
            // the task and the executor still runs in the background
            return null;
        }
        prefetchThumbnails(allThumbServers, progressReporter);
        progressReporter.accept("Drawing slide...");

        attachToViewer(sparseServer);
        return sparseServer;
    };

    private void prefetchThumbnails(List<TileImageServer> allThumbServers, Consumer<String> progressReporter) throws InterruptedException {
        int amountTiles = allThumbServers.size();
        AtomicInteger completedCount = new AtomicInteger(0);

        // initialize countdownlatch to make it possible to cancel midway of the executor
        CountDownLatch latch = new CountDownLatch(amountTiles);

        prefetchExecutor = Executors.newFixedThreadPool(64);
        for (TileImageServer thumbServer : allThumbServers) {
            prefetchExecutor.submit(() -> {
                try {
                    thumbServer.getDefaultThumbnail(0, 0);
                } catch (IOException e) {
                    ImmuNetLog.error("Prefetch failed for a thumb tile", e);
                } finally {
                    latch.countDown();
                    int n = completedCount.incrementAndGet();
                    progressReporter.accept("Loading tile " + n + "/" + amountTiles );
                }
            });
        }
        try {
            latch.await(); 
        } catch (InterruptedException e) {
            prefetchExecutor.shutdownNow();
            throw e;
        }
        prefetchExecutor.close();
    }

    private void attachToViewer(SparseImageServer sparseServer) throws IOException {
        // Attach the slide to the viewer here, on the FX thread but blocking this background
        // thread until it's done. That way, a failure to display becomes a real Task failure
        // (thrown below) instead of an uncaught exception later on the FX thread.
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer == null) {
            return;
        }
        Throwable displayError = FXUtils.callOnApplicationThread(() -> {
            // Re-check cancellation on the FX thread itself: if cancel() was called while we
            // were waiting here, skip attaching an already-cancelled slide to the viewer.
            if (task.isCancelled()) {
                return null;
            }
            try {
                viewer.setImageData(new ImageData<>(sparseServer));
                return null;
            } catch (Exception | UnsatisfiedLinkError e) {
                // setImageData rethrows UnsatisfiedLinkError as well as Exception, and an
                // Error would otherwise escape uncaught here and pop up QuPath's own dialog
                return e;
            }
        });
        if (displayError != null) {
            throw new IOException("Could not set image data for " + datasetName + "/" + slideName, displayError);
        }
    }

    @Override
    protected void onSuccess(SparseImageServer result) {
        ImmuNetLog.log("Successfully opened {}/{}", datasetName, slideName);
    }

    @Override
    protected void onCancellation() {
        ImmuNetLog.log("Cancelled while opening {}/{}", datasetName, slideName);
    }

    @Override
    protected void onFailure(Throwable exception) {
        ImmuNetLog.error("Failed to open " + datasetName + "/" + slideName, exception);
    }

    /**
     * @return the tile metadata fetched while loading the slide, or null if the load hasn't
     * succeeded yet. Lets other commands (see SlideLoadWorkflow) reuse it instead of re-fetching.
     */
    public List<TileMetadata> getTilesMetadata() {
        return tilesMetadata == null ? null : Collections.unmodifiableList(tilesMetadata);
    }

    public boolean isFullyStopped() {
        // check if the executor is null or terminated, which means all tasks are done or cancelled 
        ExecutorService e = prefetchExecutor;
        return e == null || e.isTerminated();
    }

}
