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
import java.util.Map;

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
    private static final double OVERVIEW_TARGET_MAX_DIMENSION = 2048.0; // target maximum dimension for overview level

    //IMPORTANT: this variable also determines where the annotations are going to be. It is always 1, now that we changed
    // the coordinate system of the tiles into pixels by using the dy/dx values. So it is always 1.

    // BECAUSE the annotations are stored in the backend in the coordinate system of the composite.jpg, not the original tile. since
    // annotations are gathered in the webapplication. 
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
            double overviewDownsample = getOverviewDownsample(tileMetadataList);
            ImmuNetLog.log("overviewDownsample value is" + overviewDownsample);
            double fullResolutionDownsample= 1.0;
            downsampleComposite = fullResolutionDownsample;
            ImmuNetLog.log("fullResolutionDownsample" + fullResolutionDownsample);
            downsampleComposite = fullResolutionDownsample;

            boolean registerOverviewLevel = overviewDownsample > fullResolutionDownsample;
            List<Double> downsampleQuantiles = null;
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
            List<Double> downsampleQuantiles = null;
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

    private static List<Double> getDownsampleQuantiles(double fullResolutionDownsample, double overviewDownsample, int count) {
        List<Double> quantiles = new ArrayList<>();
        for (int i = 1; i < count; i++) {
            double fraction = (double) i / count;
            quantiles.add(fullResolutionDownsample + fraction * (overviewDownsample - fullResolutionDownsample));
        }
        return quantiles;
    }
            
    public static double getOverviewDownsample(List<TileMetadata> tileMetadataList){
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
        return overviewDownsample;
    }

    public static List<TileImageServer> getOverviewServers(SparseImageServer sparseServer) throws IOException {
        double middleDownsample = sparseServer.getPreferredDownsamples()[1];
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
