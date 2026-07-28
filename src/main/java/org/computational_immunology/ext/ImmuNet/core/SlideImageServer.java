package org.computational_immunology.ext.ImmuNet.core;

import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.regions.ImageRegion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class SlideImageServer {
    private static final double COMPOSITE_SWITCH_DOWNSAMPLE = 1.5;
    private static final double OVERVIEW_TARGET_MAX_DIMENSION = 2048;

    public static SparseImageServer build(
            List<TileMetadata> tileMetadataList,
            String datasetName,
            String slideName,
            ImageRequestHandler imageRequestHandler) {
        try {
            //register information aout the slide: how many pixels large it is
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (var tileMetadata : tileMetadataList) {
                minX = Math.min(minX, tileMetadata.getX());
                minY = Math.min(minY, tileMetadata.getY());
                maxX = Math.max(maxX, tileMetadata.getX() + tileMetadata.getWidth());
                maxY = Math.max(maxY, tileMetadata.getY() + tileMetadata.getHeight());
            }
            double totalWidth = maxX - minX;
            double totalHeight = maxY - minY;

            double[] downsamples = deriveDownsamples(tileMetadataList, datasetName, slideName, imageRequestHandler);
            double downsampleThumb = downsamples[0];
            double downsampleComposite = downsamples[1];

            double registeredDownsampleThumb = COMPOSITE_SWITCH_DOWNSAMPLE;
            //this is for the overview upon image opening; else for very large tiles it doesn't open due to memory issues making this
            //extension useless
            double overviewDownsample = Math.max(totalWidth, totalHeight) / OVERVIEW_TARGET_MAX_DIMENSION;
            // use it is it's larger than the default thumb one
            boolean registerOverviewLevel = overviewDownsample > registeredDownsampleThumb;

            if (registeredDownsampleThumb <= downsampleComposite) {
                ImmuNetLog.error("COMPOSITE_SWITCH_DOWNSAMPLE (" + COMPOSITE_SWITCH_DOWNSAMPLE
                        + ") is at or below this slide's downsampleComposite (" + downsampleComposite
                        + "), using the midpoint instead.");
                registeredDownsampleThumb = (downsampleThumb + downsampleComposite) / 2;
            }

            // we are making image regions being the same size as the tiles we get from backend
            SparseImageServer.Builder builder = new SparseImageServer.Builder();
            for (var tileMetadata : tileMetadataList) {
                ImageRegion tileRegion = ImageRegion.createInstance(
                        (int) tileMetadata.getX(),
                        (int) tileMetadata.getY(),
                        (int) tileMetadata.getWidth(),
                        (int) tileMetadata.getHeight(),
                        0, 0
                );

                TileMetadata thumbTile = tileMetadata.withType(TileMetadata.ImageType.THUMB);
                TileImageServer thumbServer = new TileImageServer(thumbTile, datasetName, slideName, downsampleThumb, imageRequestHandler);
                builder.serverRegion(tileRegion, registeredDownsampleThumb, thumbServer);
                
                // register this overview level to not crash upon opening the image
                if (registerOverviewLevel) {
                    builder.serverRegion(tileRegion, overviewDownsample, thumbServer);
                }

                TileMetadata compositeTile = tileMetadata.withType(TileMetadata.ImageType.COMPOSITE);
                TileImageServer compositeServer = new TileImageServer(compositeTile, datasetName, slideName, downsampleComposite, imageRequestHandler);
                builder.serverRegion(tileRegion, downsampleComposite, compositeServer);
            }
            return builder.build();
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Error building SparseImageServer", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Samples one tile's thumb and composite images to derive the downsample factor for each
     * resolution level (averaged from width and height ratios, since they don't necessarily agree
     * exactly. Tries tiles in order in case
     * a particular tile fails to fetch (edge tile, transient error, not found on server althoug it should be there), rather than failing outright
     * on the first one.
     */
    private static double[] deriveDownsamples(
            List<TileMetadata> tileMetadataList,
            String datasetName,
            String slideName,
            ImageRequestHandler imageRequestHandler) throws IOException, InterruptedException {
        BufferedImage thumbSample = null;
        BufferedImage compositeSample = null;
        TileMetadata sampleMetadata = null;
        IOException lastError = null;

        int idx = 0;
        do {
            TileMetadata candidate = tileMetadataList.get(idx);
            try {
                thumbSample = imageRequestHandler.fetchTileImage(
                        candidate.withType(TileMetadata.ImageType.THUMB), datasetName, slideName).getImage();
                compositeSample = imageRequestHandler.fetchTileImage(
                        candidate.withType(TileMetadata.ImageType.COMPOSITE), datasetName, slideName).getImage();
                sampleMetadata = candidate;
            } catch (IOException e) {
                ImmuNetLog.log("Sample tile " + candidate.getCode() + " could not be fetched at both resolutions, trying next tile");
                lastError = e;
            }
            idx++;
        } while (sampleMetadata == null && idx < tileMetadataList.size());

        if (sampleMetadata == null) {
            throw new IOException("Could not find any tile that could be fetched at both resolutions to derive downsample", lastError);
        }

        double thumbWidthRatio = sampleMetadata.getWidth() / thumbSample.getWidth();
        double thumbHeightRatio = sampleMetadata.getHeight() / thumbSample.getHeight();
        double downsampleThumb = (thumbWidthRatio + thumbHeightRatio) / 2.0;

        double compositeWidthRatio = sampleMetadata.getWidth() / compositeSample.getWidth();
        double compositeHeightRatio = sampleMetadata.getHeight() / compositeSample.getHeight();
        double downsampleComposite = (compositeWidthRatio + compositeHeightRatio) / 2.0;

        ImmuNetLog.log("Derived downsamples from sample tile {}: downsampleThumb={}, downsampleComposite={}",
                sampleMetadata.getCode(), downsampleThumb, downsampleComposite);

        return new double[]{downsampleThumb, downsampleComposite};
    }
}
