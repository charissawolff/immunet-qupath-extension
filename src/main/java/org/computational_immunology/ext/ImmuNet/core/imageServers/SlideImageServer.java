package org.computational_immunology.ext.ImmuNet.core.imageServers;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.DatasetMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.regions.ImageRegion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
The image server for showing a SLIDE, which consists of many TILES.
Here we use a SparseImageServer which is a AbstractTileableImageServer.
This server determines the downsample values of the tiles; since we have 2 or 3 different layers. The THUMB and the composite,
each for the thumb.jpg or composite.jpg in backend server. Furthermore a third layer is made (overview) for some slides, so that Qupath doesn't crash
when opening slides with hundreds of tiles due to memory issues.

For each tile region, we build a TileImageServer per resolution and register it into the SparseImageServer.Builder.
Build() returns the finished SparseImageServer. This SparseImageServer instance (together with QuPath's own viewer)
decides which registered tile/resolution to actually fetch as the user zooms and pans around.

*/
public class SlideImageServer {
    private static final double OVERVIEW_TARGET_MAX_DIMENSION = 2048;
    //IMPORTANT: this variable also determines where the annotations are going to be. It must ALWAYS hold the  jpg-composite derived ration
    // (declared tile width /actually fetched composite.jpg width) for the slide,
    // BECAUSE the annotations are stored in the backend in the coordinate system of the composite.jpg, not the original tile. since
    // annotations are gathered in the webapplication. IN the future there should be 2 variables if people were ever going
    // going to use the qupath viewer to annotate cells in slides, but for now this is the only one we need.
    private static double downsampleComposite;

    public static double getDownsampleComposite() {
        return downsampleComposite;
    }

    public static SparseImageServer build(
            List<TileMetadata> tileMetadataList,
            String datasetName,
            String slideName,
            double compositeSwitchDownsample,
            ServerGateway serverGateway) {
        try {
            //double[] downsamples = deriveJpgDownsamples(tileMetadataList, datasetName, slideName, serverGateway);
            //downsampleComposite = downsamples[1];

            double overviewDownsample = getOverviewDownsample(tileMetadataList);
            ImmuNetLog.log("overviewDownsample value is" + overviewDownsample);
            double compositeDownsample= 1.0;
            ImmuNetLog.log("compositeDownsample" + compositeDownsample);
            downsampleComposite = compositeDownsample;
            double thumbDownsample = (overviewDownsample+compositeDownsample) /2;
            ImmuNetLog.log("thumbDownsample" + thumbDownsample);


            //DownsampleLevels downsampleLevels = getDownsampleLevels(tileMetadataList, compositeSwitchDownsample, downsamples[0], downsampleComposite);

            //double downsampleThumb = downsampleLevels.downsampleThumb();
            //double registeredDownsampleThumb = downsampleLevels.registeredDownsampleThumb();
            //double overviewDownsample = downsampleLevels.overviewDownsample();
            boolean registerOverviewLevel = overviewDownsample > 1;

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

                TileMetadata thumbTile = tileMetadata.withType(TileMetadata.ImageType.THUMB);
                JpgTileImageServer thumbServer = new JpgTileImageServer(thumbTile, datasetName, slideName, thumbDownsample, serverGateway);
                builder.serverRegion(tileRegion, thumbDownsample, thumbServer);

                // register this overview level to not crash upon opening the image
                if (registerOverviewLevel) {
                    builder.serverRegion(tileRegion, overviewDownsample, thumbServer);
                }

                TileMetadata compositeTile = tileMetadata.withType(TileMetadata.ImageType.COMPOSITE);
                JpgTileImageServer compositeServer = new JpgTileImageServer(compositeTile, datasetName, slideName, compositeDownsample, serverGateway);
                builder.serverRegion(tileRegion, compositeDownsample, compositeServer);
            }
            return builder.build();
        } catch (IOException e) {
            ImmuNetLog.error("Error building SparseImageServer for slide " + slideName + " in dataset " + datasetName, e);
            throw new RuntimeException(e);
        }
    }
    

    public static double getOverviewDownsample(List<TileMetadata> tileMetadataList){
        //make sure it's under OVERVIEW_TARGET_MAX_DIMENSION pixels 
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
        double overviewDownsample = Math.max(totalWidth, totalHeight) / OVERVIEW_TARGET_MAX_DIMENSION;
        return overviewDownsample;
    }

    // how pre-downsampled is the raw fetched source file?
    private static DownsampleLevels getDownsampleLevels(
        List<TileMetadata> tileMetadataList, double compositeSwitchDownsample,
        double downsampleThumb, double downsampleComposite) {
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

        double registeredDownsampleThumb = compositeSwitchDownsample;
        //this is for the overview upon image opening; else for very large tiles it doesn't open due to memory issues making this
        //extension useless
        double overviewDownsample = Math.max(totalWidth, totalHeight) / OVERVIEW_TARGET_MAX_DIMENSION;

        if (registeredDownsampleThumb <= downsampleComposite) {
            ImmuNetLog.error("compositeSwitchDownsample (" + compositeSwitchDownsample
                    + ") is at or below this slide's downsampleComposite (" + downsampleComposite
                    + "), using the midpoint instead.");
            registeredDownsampleThumb = (downsampleThumb + downsampleComposite) / 2;
        }

        // cap so thumb stays selectable at the most-zoomed-out view, otherwise the initial slide-open
        // (QuPath's histogram scan reads the whole slide in one call) would need composite tiles across
        // the entire slide, which is the exact OOM/crash risk the overview level exists to prevent.
        if (registeredDownsampleThumb >= overviewDownsample) {
            ImmuNetLog.error("compositeSwitchDownsample (" + compositeSwitchDownsample
                    + ") is at or above this slide's overviewDownsample (" + overviewDownsample
                    + "), capping it just below instead.");
            registeredDownsampleThumb = overviewDownsample * 0.99;
        }
        boolean registerOverviewLevel = overviewDownsample > registeredDownsampleThumb;
        return new DownsampleLevels(downsampleThumb, registeredDownsampleThumb, overviewDownsample, registerOverviewLevel);
    }


    public static List<TileImageServer> getThumbServers(SparseImageServer sparseServer) throws IOException {
        double thumbDownsample = sparseServer.getPreferredDownsamples()[1];
        List<TileImageServer> thumbServers = new ArrayList<>();
        for (ImageRegion region : sparseServer.getManager().getRegions()) {
            try {
                ImageServer<BufferedImage> server = sparseServer.getManager().getServer(region, thumbDownsample);
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
    /**
     * Samples one tile's thumb and composite images to derive the downsample factor for each
     * resolution level (averaged from width and height ratios, since they don't necessarily agree
     * exactly. Tries tiles in order in case
     * a particular tile fails to fetch (edge tile, transient error, not found on server althoug it should be there), rather than failing
     * on the first one.
     */
    private static double[] deriveJpgDownsamples(
            List<TileMetadata> tileMetadataList,
            String datasetName,
            String slideName,
            ServerGateway serverGateway) throws IOException, InterruptedException {
        BufferedImage thumbSample = null;
        BufferedImage compositeSample = null;
        TileMetadata sampleMetadata = null;
        IOException lastError = null;

        int idx = 0;
        do {
            TileMetadata candidate = tileMetadataList.get(idx);
            try {
                thumbSample = serverGateway.fetchTileImage(
                        datasetName, slideName, candidate.withType(TileMetadata.ImageType.THUMB)).getImage();
                compositeSample = serverGateway.fetchTileImage(
                        datasetName, slideName, candidate.withType(TileMetadata.ImageType.COMPOSITE)).getImage();
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

        //double compositeWidthRatio = sampleMetadata.getWidth() / compositeSample.getWidth();
        //double compositeHeightRatio = sampleMetadata.getHeight() / compositeSample.getHeight();
        double downsampleComposite = 1.0; //(compositeWidthRatio + compositeHeightRatio) / 2.0;

        ImmuNetLog.log("the downsample composite of jpeg is ", downsampleComposite);

        ImmuNetLog.log("Derived downsamples from sample tile {}: downsampleThumb={}, downsampleComposite={}",
                sampleMetadata.getCode(), downsampleThumb, downsampleComposite);
        return new double[]{downsampleThumb, downsampleComposite};
    }

    private record DownsampleLevels(
        double downsampleThumb,
        double registeredDownsampleThumb,
        double overviewDownsample,
        boolean registerOverviewLevel
    ) {}
}
