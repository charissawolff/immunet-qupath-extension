package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SlideImageServer;
import org.computational_immunology.ext.ImmuNet.core.TileImageServer;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import javafx.concurrent.Task;

import qupath.fx.utils.FXUtils;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.SparseImageServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures the selected dataset/slide once at click time, then loads it in a cancellable
 * background Task. 
 */
public class SelectSlideCommand {

    private final String datasetName;
    private final String slideName;
    private final ImageRequestHandler imageRequestHandler;
    private final double compositeSwitchDownsample;
    private Task<SparseImageServer> task;
    private volatile ExecutorService prefetchExecutor;
    private Runnable onDone;
    private List<TileMetadata> tilesMetadata;

    public SelectSlideCommand(String datasetName, String slideName, double compositeSwitchDownsample, ImageRequestHandler imageRequestHandler) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.compositeSwitchDownsample = compositeSwitchDownsample;
        this.imageRequestHandler = imageRequestHandler;
    }

    public void build() {
        task = new Task<>() {
            {
                updateMessage("Opening...");
            }
            @Override
            protected SparseImageServer call() throws Exception {
                updateMessage("Fetching slide metadata...");
                tilesMetadata = imageRequestHandler.getAllTileMetadatas(datasetName, slideName);
                SparseImageServer sparseServer = SlideImageServer.build(tilesMetadata, datasetName, slideName, compositeSwitchDownsample, imageRequestHandler);
                List<TileImageServer> allThumbServers = SlideImageServer.getThumbServers(sparseServer);
                int amountTiles = allThumbServers.size();
                AtomicInteger completedCount = new AtomicInteger(0);

                // initialize countdownlatch to make it possible to cancel midway of the executor
                CountDownLatch latch = new CountDownLatch(amountTiles);

                if (isCancelled()) {
                    // check if the task was cancelled before starting the executor, to avoid that the user cancels 
                    // the task and the executor still runs in the background
                    return null;
                }
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
                            updateMessage("Loading tile " + n + "/" + amountTiles );
                        }
                    });
                }
                try {
                    latch.await(); 
                } catch (InterruptedException e) {
                    prefetchExecutor.shutdownNow();
                    throw e;
                }
                updateMessage("Drawing slide...");
                prefetchExecutor.close();

                // Attach the slide to the viewer here, on the FX thread but blocking this background
                // thread until it's done. That way, a failure to display becomes a real Task failure
                // (thrown below) instead of an uncaught exception later on the FX thread.
                QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
                if (viewer != null) {
                    Throwable displayError = FXUtils.callOnApplicationThread(() -> {
                        // Re-check cancellation on the FX thread itself: if cancel() was called while we
                        // were waiting here, skip attaching an already-cancelled slide to the viewer.
                        if (isCancelled()) {
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
                return sparseServer;
            }
        };
    }

    public void start() {
         task.setOnSucceeded(event -> {
            ImmuNetLog.log("Successfully opened {}/{}", datasetName, slideName);
            if (onDone != null) {
                onDone.run();
            }
        });

        task.setOnFailed(event ->
                ImmuNetLog.error("Could not open " + datasetName + "/" + slideName, task.getException()));

        task.setOnCancelled(event ->
                ImmuNetLog.log("Cancelled opening {}/{}", datasetName, slideName));

        Thread thread = new Thread(task, "select-slide-" + datasetName + "-" + slideName);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * @return the background Task backing this command, or null before build() has been called.
     * Callers that may supersede this command (e.g. selecting a different slide) should hold onto
     * this and call cancel() on it before starting a new one.
     */
    public Task<SparseImageServer> getTask() {
        return task;
    }

    /**
     * @return the tile metadata fetched while loading the slide, or null if the load hasn't
     * succeeded yet. Lets other commands (see SlideLoadWorkflow) reuse it instead of re-fetching.
     */
    public List<TileMetadata> getTilesMetadata() {
        return tilesMetadata;
    }

    public void setOnDone(Runnable callback) {
        this.onDone = callback;
    }

    public boolean isFullyStopped() {
        // check if the executor is null or terminated, which means all tasks are done or cancelled 
        ExecutorService e = prefetchExecutor;
        return e == null || e.isTerminated();
    }

}
