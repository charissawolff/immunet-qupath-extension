package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.imageServers.SlideImageServer;
import org.computational_immunology.ext.ImmuNet.core.imageServers.TileImageServer;
import org.computational_immunology.ext.ImmuNet.core.models.DatasetMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;

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
    private final ServerGateway serverGateway;
    private final Boolean useTiffComposite;
    private final double compositeSwitchDownsample;
    private volatile ExecutorService prefetchExecutor;
    private List<TileMetadata> tileMetadatas;

    public SelectSlideCommand(String datasetName, String slideName, double compositeSwitchDownsample,
        ServerGateway serverGateway, Boolean useTiffComposite) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.compositeSwitchDownsample = compositeSwitchDownsample;
        this.serverGateway = serverGateway;
        this.useTiffComposite = useTiffComposite;
    }

    @Override
    protected SparseImageServer execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Fetching slide metadata...");
        tileMetadatas = serverGateway.getTileMetadatas(datasetName, slideName);
        progressReporter.accept("Getting ready to process tiles...");
        SparseImageServer sparseServer;
        if (!useTiffComposite) {
            sparseServer = SlideImageServer.build(tileMetadatas, datasetName, slideName, compositeSwitchDownsample, serverGateway);
        } else {
            //get tiff
            sparseServer = SlideImageServer.buildTiff(tileMetadatas, datasetName, slideName, serverGateway);
        }

        List<TileImageServer> allThumbServers = SlideImageServer.getThumbServers(sparseServer);
        progressReporter.accept("Fetching " + tileMetadatas.size() +" files...");
        if (task.isCancelled()) {
            return null;
        }
        prefetchThumbnails(allThumbServers, progressReporter);

        progressReporter.accept("Drawing slide...");

        attachToViewer(sparseServer);
        return sparseServer;
    }

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
        return tileMetadatas == null ? null : Collections.unmodifiableList(tileMetadatas);
    }
}
