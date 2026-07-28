package org.computational_immunology.ext.ImmuNet.core;

import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.regions.ImageRegion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class SlideImageServer {
    // SparseImageServerManager's per-region level selection only ever compares the requested
    // downsample against the registered THUMB value when there are just two levels. As soon as
    // the viewer's zoom passes below it, composite is selected, regardless of how far off
    // composite's own value. This constant is the actual switch-to-composite threshold.
    // Fixed at an absolute downsample.  StreamedImageServer.readRegion then 
    // resizes the real fetched thumb image to whatever size this implies, so registering thumb here
    // instead of at its true native resolution stays consistent.
    private static final double COMPOSITE_SWITCH_DOWNSAMPLE = 1.5;

    public static SparseImageServer build(
            List<TileMetadata> tileMetadataList,
            String datasetName,
            String slideName,
            ImageRequestHandler imageRequestHandler) {
        try {
            double[] downsamples = deriveDownsamples(tileMetadataList, datasetName, slideName, imageRequestHandler);
            double downsampleThumb = downsamples[0];
            double downsampleComposite = downsamples[1];

            double registeredDownsampleThumb = COMPOSITE_SWITCH_DOWNSAMPLE;
            if (registeredDownsampleThumb <= downsampleComposite) {
                // This slide's composite is coarser than COMPOSITE_SWITCH_DOWNSAMPLE, so we fall back to the midpoint, which is
                // guaranteed to stay strictly between the two true values so it can never invert the order (so composite will never be loaded before thumb zooming out).
                ImmuNetLog.error("COMPOSITE_SWITCH_DOWNSAMPLE (" + COMPOSITE_SWITCH_DOWNSAMPLE
                        + ") is at or below this slide's downsampleComposite (" + downsampleComposite
                        + "), using the midpoint instead.");
                registeredDownsampleThumb = (downsampleThumb + downsampleComposite) / 2;
            }

            SparseImageServer.Builder builder = new SparseImageServer.Builder();
            for (var tileMetadata : tileMetadataList) {
                ImageRegion tileRegion = ImageRegion.createInstance(
                        (int) tileMetadata.getX(),
                        (int) tileMetadata.getY(),
                        (int) tileMetadata.getWidth(),
                        (int) tileMetadata.getHeight(),
                        1, 0
                );

                TileMetadata thumbTile = tileMetadata.withType(TileMetadata.ImageType.THUMB);
                builder.serverRegion(tileRegion, registeredDownsampleThumb,
                        new TileImageServer(thumbTile, datasetName, slideName, imageRequestHandler));

                TileMetadata compositeTile = tileMetadata.withType(TileMetadata.ImageType.COMPOSITE);
                builder.serverRegion(tileRegion, downsampleComposite,
                        new TileImageServer(compositeTile, datasetName, slideName, imageRequestHandler));
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
