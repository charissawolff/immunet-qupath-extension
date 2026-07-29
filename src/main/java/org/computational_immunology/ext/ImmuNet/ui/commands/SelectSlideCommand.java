package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SlideImageServer;
import org.computational_immunology.ext.ImmuNet.core.TileImageServer;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import javafx.concurrent.Task;

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
    private Task<SparseImageServer> task;
    private Runnable onDone;

    public SelectSlideCommand(String datasetName, String slideName, ImageRequestHandler imageRequestHandler) {
        this.datasetName = datasetName;
        this.slideName = slideName;
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
                List<TileMetadata> tiles = imageRequestHandler.getAllTileMetadatas(datasetName, slideName);
                SparseImageServer sparseServer = SlideImageServer.build(tiles, datasetName, slideName, imageRequestHandler);
                List<TileImageServer> allThumbServers = SlideImageServer.getThumbServers(sparseServer);
                int amountTiles = allThumbServers.size();
                AtomicInteger completedCount = new AtomicInteger(0);

                // initialize countdownlatch to make it possible to cancel midway of the executor
                CountDownLatch latch = new CountDownLatch(amountTiles);

                ExecutorService prefetchExecutor = Executors.newFixedThreadPool(64);
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
                updateMessage("Drawing slide...");
                try {
                    latch.await(); 
                } catch (InterruptedException e) {
                    prefetchExecutor.shutdownNow();
                    throw e;
                }
                prefetchExecutor.close();
                return sparseServer;
            }
        };
    }

    public void start() {
         task.setOnSucceeded(event -> {
            QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
            if (viewer != null) {
                try {
                    viewer.setImageData(new ImageData<>(task.getValue()));
                    ImmuNetLog.log("Successfully opened {}/{}", datasetName, slideName);
                    if (onDone != null) {
                        onDone.run();
                    } 
                } catch (IOException e) {
                    ImmuNetLog.error("Could not set image data for " + datasetName + "/" + slideName, e);
                }
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

    public void setOnDone(Runnable callback) {
        this.onDone = callback;
    }

}
