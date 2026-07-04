package org.computational_immunology;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.io.IOException;

import javafx.application.Platform;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.regions.ImageRegion;

public class MenuActions {
    public static void connectToServer(String username, String hostname, String password, String dbuser, String dbpass) throws Exception {
        ServerConnectionHandler.getInstance().startSSHThread(username, hostname, password);
        ServerConnectionHandler.getInstance().performDatabaseLogin(dbuser,dbpass);
    }

    public static void updateListViewerBox(ListViewerBox box, List<String> list){
        box.setItems(list);
    }

    public static void setStreamedServer(String datasetName, String slideName) {
        setStreamedServer(datasetName, slideName, null);
    }

    /**
     * Streams the given slide into the QuPath viewer.
     *
     * @param datasetName      the dataset containing the slide
     * @param slideName        the slide to open
     * @param progressCallback optional callback invoked as each tile is processed
     *                         with (tilesProcessed, totalTiles); may be {@code null}.
     *                         Note: this runs on the calling thread, so call this
     *                         method off the JavaFX Application Thread if you want
     *                         progress to be visible.
     */
    public static void setStreamedServer(String datasetName, String slideName,
            BiConsumer<Integer, Integer> progressCallback) {
        try {
            List<Tile> tiles = ServerRequestHandler.getAllTiles(
                    datasetName, slideName
            );
            SparseImageServer server = createSparseImageServer(tiles, progressCallback);
            // The viewer must only be touched on the JavaFX Application Thread.
            Platform.runLater(() -> {
                try {
                    QuPathGUI.getInstance().getViewer().setImageData(new ImageData<>(server));
                    ImmuNetLog.log("Successfully set Image Data.");
                } catch (IOException e) {
                    ImmuNetLog.error("Could not set image data", e);
                }
            });
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Could not fetch tiles. Are you connected to the server?",e);
        }
    }

    /** Maximum number of tiles to download concurrently while prefetching. */
    private static final int MAX_PREFETCH_THREADS = 8;

    /**
     * Downsample levels registered for every tile, forming a small image pyramid so
     * zoomed-out views can be drawn from pre-shrunk copies instead of rescaling the
     * full-resolution image on every repaint.
     */
    private static final int[] DOWNSAMPLE_LEVELS = {1, 2, 4, 8};

    private static SparseImageServer createSparseImageServer(List<Tile> tiles,
            BiConsumer<Integer, Integer> progressCallback) throws IOException, InterruptedException {
        // Download all tile pixels up front (in parallel) so navigation is smooth
        // once the image is opened, rather than fetching lazily on the render thread.
        prefetchTiles(tiles, progressCallback);

        SparseImageServer.Builder builder = new SparseImageServer.Builder();
        for (var tile : tiles) {
            // One region instance per tile, reused across levels so the sparse server
            // groups the resolutions together as a pyramid for that region.
            ImageRegion region = ImageRegion.createInstance(
                    (int)tile.getTileX(),
                    (int)tile.getTileY(),
                    (int)tile.tileW, (int)tile.tileH, 1,0
            );
            for (int ds : DOWNSAMPLE_LEVELS) {
                // Skip levels that would shrink the tile below a single pixel.
                if ((int)tile.tileW / ds < 1 || (int)tile.tileH / ds < 1) {
                    continue;
                }
                builder.serverRegion(region, ds, new StreamedImageServer(tile, ds));
            }
            ImmuNetLog.log("serverRegion add: ({}), ({}, {}), levels {}",
                    region, tile.tileW, tile.tileH, DOWNSAMPLE_LEVELS.length);
        }

        return builder.build();
    }

    /**
     * Downloads and caches every tile's pixels concurrently, reporting progress as
     * each tile completes. Blocks until all tiles are fetched.
     *
     * @param tiles            the tiles to prefetch
     * @param progressCallback optional callback invoked with (tilesFetched, totalTiles);
     *                         may be called from multiple threads.
     */
    private static void prefetchTiles(List<Tile> tiles,
            BiConsumer<Integer, Integer> progressCallback) throws IOException, InterruptedException {
        int total = tiles.size();
        if (total == 0) {
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(total, MAX_PREFETCH_THREADS));
        AtomicInteger fetched = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(total);
        try {
            for (Tile tile : tiles) {
                futures.add(pool.submit(() -> {
                    // Download the full-resolution pixels and pre-build every pyramid
                    // level now (in parallel), so the first zoom-out is instant too.
                    tile.getImage();
                    for (int ds : DOWNSAMPLE_LEVELS) {
                        if (ds > 1 && (int) tile.tileW / ds >= 1 && (int) tile.tileH / ds >= 1) {
                            tile.getImage(ds);
                        }
                    }
                    if (progressCallback != null) {
                        progressCallback.accept(fetched.incrementAndGet(), total);
                    }
                    return null;
                }));
            }
            // Propagate the first failure (if any) and wait for all downloads to finish.
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException io) {
                        throw io;
                    }
                    throw new IOException("Failed to prefetch tile", cause);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private MenuActions() {
        /* This utility class should not be instantiated */
    }

}