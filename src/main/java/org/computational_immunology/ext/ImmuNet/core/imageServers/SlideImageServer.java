package org.computational_immunology.ext.ImmuNet.core.imageServers;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.regions.ImageRegion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The image server for showing a vectra SLIDE, which consists of many TILES. Here we use a
 * {@link SparseImageServer}, which is an {@code AbstractTileableImageServer}. This server
 * determines the downsample values of the tiles, since we have 2 or 3 different layers: the
 * THUMB and the composite, each for the thumb.jpg or composite.jpg in the backend server.
 * Furthermore a third layer is made (overview) for some slides, so that QuPath doesn't crash
 * when opening slides with hundreds of tiles due to memory issues.
 * <p>
 * For each tile region, we build a {@link TileImageServer} per resolution and register it into
 * the {@link SparseImageServer.Builder}. {@code build()} returns the finished
 * {@link SparseImageServer}. This {@link SparseImageServer} instance (together with QuPath's own
 * viewer) decides which registered tile/resolution to actually fetch as the user zooms and pans
 * around.
 */
public class SlideImageServer {
    private static final double OVERVIEW_TARGET_MAX_DIMENSION = 2048.0; // target maximum dimension for overview level

    /**
     * Builds a {@link SparseImageServer} for an RGB slide (JPEG tiles) from the given tile
     * metadata, registering a {@link JpgTileImageServer} per resolution for each tile region.
     * The full-resolution downsample is always {@code 1.0}. This variable also determines where
     * the annotations are going to be: it must stay 1, now that the coordinate system of the
     * tiles has been changed into pixels by using the dx/dy values. This is because annotations
     * are gathered in the web application and stored in the backend in the coordinate system of
     * composite.jpg, not of the original tile.
     * @param tileMetadataList metadata for every tile which is part of the slide
     * @param datasetName the dataset the slide belongs to
     * @param slideName the name of the slide
     * @param serverGateway used by the registered tile servers to fetch tile images from the backend
     * @return the assembled sparse image server for the slide
     * @throws RuntimeException if building the {@link SparseImageServer.Builder} fails with an {@link IOException}
     */

    public static SparseImageServer build(
            List<TileMetadata> tileMetadataList,
            String datasetName,
            String slideName,
            ServerGateway serverGateway) {
        try {
            int overviewDownsample = getOverviewDownsample(tileMetadataList);
            ImmuNetLog.log("overviewDownsample value is" + overviewDownsample);
            double fullResolutionDownsample= 1.0;
            ImmuNetLog.log("fullResolutionDownsample" + fullResolutionDownsample);

            boolean registerOverviewLevel = overviewDownsample > fullResolutionDownsample;
            List<Integer> downsampleQuantiles = null;
            double middleDownsample = 0.0;
            if (registerOverviewLevel) {
                downsampleQuantiles = getDownsampleQuantiles(fullResolutionDownsample, overviewDownsample, 4);
                middleDownsample = downsampleQuantiles.get(downsampleQuantiles.size() / 2);
                ImmuNetLog.log("downsample quantiles are: " + downsampleQuantiles);
            } else {
                ImmuNetLog.log("No overview levels besides full resolution ");
            }
            

            // we are making image regions being the same size as the tiles we get from backend
            SparseImageServer.Builder builder = new SparseImageServer.Builder();
            for (var tileMetadata : tileMetadataList) {
                ImageRegion tileRegion = ImageRegion.createInstance(
                        tileMetadata.getPixelX(),
                        tileMetadata.getPixelY(),
                        tileMetadata.getPixelWidth(),
                        tileMetadata.getPixelHeight(),
                        0, 0
                );

                // register a thumb pyramid only when the slide is actually bigger than the overview target;
                // otherwise every interpolated level would fall below fullResolutionDownsample
                if (registerOverviewLevel) {
                    TileMetadata thumbTile = tileMetadata.withType(TileMetadata.ImageType.THUMB);
                    JpgTileImageServer thumbServer = new JpgTileImageServer(thumbTile, datasetName, slideName, middleDownsample, serverGateway);
                    for (double downsample : downsampleQuantiles) {
                        builder.serverRegion(tileRegion, downsample, thumbServer);
                    }
                    builder.serverRegion(tileRegion, overviewDownsample, thumbServer);
                }

                TileMetadata fullResolutionTile = tileMetadata.withType(TileMetadata.ImageType.COMPOSITE);
                JpgTileImageServer fullResolutionServer = new JpgTileImageServer(fullResolutionTile, datasetName, slideName, fullResolutionDownsample, serverGateway);
                builder.serverRegion(tileRegion, fullResolutionDownsample, fullResolutionServer);
            }
            return builder.build();
        } catch (IOException e) {
            ImmuNetLog.error("Error building SparseImageServer for slide " + slideName + " in dataset " + datasetName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a {@link SparseImageServer} for a a slide consisting of multi channel TIFF tiles, 
     * by registering a {@link TiffTileImageServer} per resolution for each
     * tile region. The same as {@link #build}, except that it serves per-channel float32 tiles built
     * from {@code channelList} instead of RGB JPEGs. There are also more quantiles resolutions (12 instead of 4).
     * @param tileMetadataList metadata for every tile which is part of the slide
     * @param datasetName the dataset the slide belongs to
     * @param slideName the name of the slide
     * @param channelList the names of the channels for each {@link TiffTileImageServer}
     * @param serverGateway used by the registered tile servers to fetch tile images from the backend
     * @return the sparse image server for the slide
     * @throws RuntimeException if building the {@link SparseImageServer.Builder} fails with an {@link IOException}
     */

    public static SparseImageServer buildTiff(
        List<TileMetadata> tileMetadataList,
        String datasetName,
        String slideName,
        List<String> channelList,
        ServerGateway serverGateway) {
        try {
            double overviewDownsample = getOverviewDownsample(tileMetadataList);
            ImmuNetLog.log("overviewDownsample value is" + overviewDownsample);
            double fullResolutionDownsample= 1.0;
            ImmuNetLog.log("fullResolutionDownsample" + fullResolutionDownsample);

            boolean registerOverviewLevel = overviewDownsample > fullResolutionDownsample;
            List<Integer> downsampleQuantiles = null;
            double middleDownsample = 0.0;
            if (registerOverviewLevel) {
                downsampleQuantiles = getDownsampleQuantiles(fullResolutionDownsample, overviewDownsample, 12);
                middleDownsample = downsampleQuantiles.get(downsampleQuantiles.size() / 2);
                ImmuNetLog.log("downsample quantiles are: " + downsampleQuantiles);
            } else {
                ImmuNetLog.log("No overview levels besides full resolution ");
            }
            

            SparseImageServer.Builder builder = new SparseImageServer.Builder();
            for (var tileMetadata : tileMetadataList) {
                ImageRegion tileRegion = ImageRegion.createInstance(
                        tileMetadata.getPixelX(),
                        tileMetadata.getPixelY(),
                        tileMetadata.getPixelWidth(),
                        tileMetadata.getPixelHeight(),
                        0, 0
                );

                // register a thumb pyramid only when the slide is actually bigger than the overview target;
                // otherwise every interpolated level would fall below fullResolutionDownsample
                if (registerOverviewLevel) {
                    TileMetadata thumbTile = tileMetadata.withType(TileMetadata.ImageType.THUMB);
                    TiffTileImageServer thumbServer = new TiffTileImageServer(thumbTile, datasetName, slideName, channelList, middleDownsample, serverGateway);
                    for (double downsample : downsampleQuantiles) {
                        builder.serverRegion(tileRegion, downsample, thumbServer);
                    }
                    builder.serverRegion(tileRegion, overviewDownsample, thumbServer);
                }

                TileMetadata fullResolutionTile = tileMetadata.withType(TileMetadata.ImageType.COMPOSITE);
                TiffTileImageServer fullResolutionServer = new TiffTileImageServer(fullResolutionTile, datasetName, slideName, channelList, fullResolutionDownsample, serverGateway);
                builder.serverRegion(tileRegion, fullResolutionDownsample, fullResolutionServer);
            }
            return builder.build();
        } catch (IOException e) {
            ImmuNetLog.error("Error building SparseImageServer for slide " + slideName + " in dataset " + datasetName, e);
            throw new RuntimeException(e);
        }
    }

    private static List<Integer> getDownsampleQuantiles(double fullResolutionDownsample, double overviewDownsample, int count) {
        List<Integer> quantiles = new ArrayList<>();
        for (int i = 1; i < count; i++) {
            double fraction = (double) i / count;
            double quantile = fullResolutionDownsample + fraction * (overviewDownsample - fullResolutionDownsample);
            //round up to the nearest integer so that the server can actually process it, because it treats non int numbers as 1 which is NOT what we want.
            quantile = Math.ceil(quantile);
            quantiles.add((int) quantile);
        }
        return quantiles;
    }
            
    /**
     * Computes the downsample value needed for a given slide made up on tiles to fit {@link #OVERVIEW_TARGET_MAX_DIMENSION} pixels on its longest side, based on the combined
     * pixel extent of every tile in {@code tileMetadataList}.
     * @param tileMetadataList lists of tiles containing their the metadata with information on tile height and width
     * @return the overview downsample, as an integer (rounded up)
     */
    private static int getOverviewDownsample(List<TileMetadata> tileMetadataList){
        //make sure it's under OVERVIEW_TARGET_MAX_DIMENSION pixels 
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (var tileMetadata : tileMetadataList) {
            minX = Math.min(minX, tileMetadata.getPixelX());
            minY = Math.min(minY, tileMetadata.getPixelY());
            maxX = Math.max(maxX, tileMetadata.getPixelX() + tileMetadata.getPixelWidth());
            maxY = Math.max(maxY, tileMetadata.getPixelY() + tileMetadata.getPixelHeight());
        }
        double totalWidth = maxX - minX;
        double totalHeight = maxY - minY;
        double overviewDownsample = Math.max(totalWidth, totalHeight) / OVERVIEW_TARGET_MAX_DIMENSION;
        //floor it up to the nearest integer so that the server can actually process it, because it treats non int numbers as 1 which is NOT what we want.
        overviewDownsample = Math.ceil(overviewDownsample);
        ImmuNetLog.log("Calculated overview downsample: " + overviewDownsample + " for total width: " + totalWidth + ", total height: " + totalHeight);
        return (int) overviewDownsample;
    }
    
    /**
     * Returns an overview image server so that they can be prefetched. By doing prefetching, it is possible to send many requests to backend at once so that
     * we can hold the relevant overview imageServers in cache, to hopefully load the slide faster, upon opening. 
     * @param sparseServer  the sparse server built by {@link #build} or {@link #buildTiff}
     * @return the relevant image servers
     * @throws IOException if the sparse server's region manager cannot be read
     */
    public static List<TileImageServer> getOverviewServers(SparseImageServer sparseServer) throws IOException {
        double middleDownsample = sparseServer.getPreferredDownsamples()[0];
        List<TileImageServer> thumbServers = new ArrayList<>();
        for (ImageRegion region : sparseServer.getManager().getRegions()) {
            try {
                ImageServer<BufferedImage> server = sparseServer.getManager().getServer(region, middleDownsample);
                if (server instanceof TileImageServer tileImageServer) {
                    thumbServers.add(tileImageServer);
                } else {
                    ImmuNetLog.error("No thumb server registered for region {}, skipping its prefetch", region);
                }
            } catch (IOException e) {
                ImmuNetLog.error("Could not build thumb server for region " + region + " skipping its prefetch", e);
            }
        }
        return thumbServers;
    }
}
