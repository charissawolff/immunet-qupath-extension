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
import java.util.Arrays;
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
public class TiffSlideImageServer {
    private static final double OVERVIEW_TARGET_MAX_DIMENSION = 1024;
    //IMPORTANT: this variable also determines where the annotations are going to be. It must ALWAYS hold the  jpg-composite derived ration
    // (declared tile width /actually fetched composite.jpg width) for the slide,
    // BECAUSE the annotations are stored in the backend in the coordinate system of the composite.jpg, not the original tile. since
    // annotations are gathered in the webapplication. IN the future there should be 2 variables if people were ever going
    // going to use the qupath viewer to annotate cells in slides, but for now this is the only one we need.
    private static double downsampleComposite;
    private List<TileMetadata> tileMetadataList;

    public static double getDownsampleComposite() {
        return downsampleComposite;
    }
    public SparseImageServer build(
        //DatasetMetadata datasetMetadata,
        List<TileMetadata> tileMetadataList,
        String datasetName,
        String slideName,
        ServerGateway serverGateway) {

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

        double overviewDownsample = Math.max(totalWidth, totalHeight) / OVERVIEW_TARGET_MAX_DIMENSION;
        ImmuNetLog.log("overviewDownsample: {}", overviewDownsample);
        double[] downsamples = new double[]{overviewDownsample,overviewDownsample*0.5, 1.0};
        downsamples = Arrays.stream(downsamples).filter(d -> d >= 0.5).toArray(); // Filter out downsample values less than 0.5 to avoid fetching tiles at higher resolution than available
        try {
            SparseImageServer.Builder builder = new SparseImageServer.Builder() ;
            for (TileMetadata tileMetadata : tileMetadataList) {
                ImageRegion tileRegion = ImageRegion.createInstance(
                        tileMetadata.getPixelX(),
                        tileMetadata.getPixelY(),
                        tileMetadata.getPixelWidth(),
                        tileMetadata.getPixelHeight(),
                        0, 0
                );
                for (double downsample : downsamples) {
                    if (downsample < 1.0) {
                        continue; // Skip downsample values less than 1.0 to avoid fetching tiles at higher resolution than available
                    }
                    TiffTileImageServer tileServer = new TiffTileImageServer(
                                tileMetadata, datasetName, slideName, downsample, serverGateway);
                            //datasetMetadata, tileMetadata, datasetName, slideName, downsample, serverGateway);
                    builder.serverRegion(tileRegion, downsample, tileServer);
                }
            }
            return builder.build();
        } catch (IOException e) {
            ImmuNetLog.error("Error building SparseImageServer for slide: " + slideName + " in dataset: " + datasetName, e);
            throw new RuntimeException(e);
        }
    }

    public static List<TileImageServer> getThumbServers(SparseImageServer sparseServer) throws IOException {
        double[] downsamples = sparseServer.getPreferredDownsamples();
        double thumbDownsample = downsamples[downsamples.length - 1]; // Get the last (highest) downsample value
        ImmuNetLog.log("Fetching thumb servers for downsample: {}", thumbDownsample);
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
     */

}

