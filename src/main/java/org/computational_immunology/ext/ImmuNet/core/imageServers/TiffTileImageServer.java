package org.computational_immunology.ext.ImmuNet.core.imageServers;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.DatasetMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TiffTileImageServer extends TileImageServer {
    private final double downsampleValue;
    private final ServerGateway serverGateway;
    private final ImageServerMetadata metadata;

    public TiffTileImageServer(//DatasetMetadata datasetMetadata,
        TileMetadata tileMetadata, String datasetName,
            String slideName, double downsampleValue,
            ServerGateway serverGateway) {
        super(tileMetadata, datasetName, slideName);
        this.downsampleValue = downsampleValue;
        this.serverGateway = serverGateway;

        int fullWidth  = tileMetadata.getPixelWidth();
        int fullHeight = tileMetadata.getPixelHeight();
        int levelWidth  = Math.max(1, (int) Math.round(fullWidth  / downsampleValue));
        int levelHeight = Math.max(1, (int) Math.round(fullHeight / downsampleValue));
        double declaredDownsample = downsampleValue;

        double dx = tileMetadata.getDx(); // pixel per µm
        double dy = tileMetadata.getDy();

        this.metadata = new ImageServerMetadata.Builder()
                .width(fullWidth).height(fullHeight)
                .name(datasetName + "/" + slideName + "/" + tileMetadata.getCode() + " (" + tileMetadata.getType() + downsampleValue +")")
                .rgb(false).pixelType(PixelType.FLOAT32)
                .channels(ImageChannel.getDefaultChannelList(8)) //
                //.channels(toImageChannels(datasetMetadata.getAntibodyPanel()))
                .pixelSizeMicrons(dx, dy) //
                .sizeZ(1).sizeT(1)
                .levels(new ImageServerMetadata.ImageResolutionLevel.Builder(fullWidth, fullHeight)
                        .addLevel(declaredDownsample, levelWidth, levelHeight)
                        .build())
                .preferredTileSize(levelWidth, levelHeight)
                .build();
    }

    @Override
    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        int requestedWidth = tileRequest.getTileWidth();
        int requestedHeight = tileRequest.getTileHeight();
        double downsample = tileRequest.getDownsample();

        try {
            ImmuNetLog.log("Fetching TIFF tile with downsample {} ", downsample);
            Tile fetchedTile = serverGateway.fetchComponentsTiffImage(tileMetadata, datasetName, slideName, downsample);
            return fetchedTile.resizeTiffImage(requestedWidth, requestedHeight);
        } catch (InterruptedException e) {
            ImmuNetLog.log("Error in reading TIFF Tile in Tile image server");
            throw new IOException("Interrupted fetching tile " + tileMetadata.getCode(), e);
        } catch (IOException e) {
            ImmuNetLog.log("Could not fetch TIFF tile image for tile code: " + tileMetadata.getCode() + " at dataset: " + datasetName + ", slide: " + slideName, e);
            return blankTile(requestedWidth, requestedHeight);
        }
    }

    @Override
    protected String createID() {
        return datasetName + "/" + slideName + "/" + tileMetadata.getCode() + "-TIFF" + downsampleValue;
    }

    @Override
    public String getServerType() {
        return "TiffCompositeTileImageServer";
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return metadata;
    }

    private static List<ImageChannel> toImageChannels(DatasetMetadata.AntibodyPanel antibodyPanel) {
        List<ImageChannel> channels = new ArrayList<>();
        Map<String, int[]> defaultColors = antibodyPanel.getDefaultColors();
        for (String name : antibodyPanel.getChannels()) {
            int[] rgb = defaultColors.get(name);
            if (rgb != null) {
                channels.add(ImageChannel.getInstance(name, ColorTools.packRGB(rgb[0], rgb[1], rgb[2])));
            } else {
                channels.add(ImageChannel.getInstance(name, ImageChannel.getDefaultChannelColor(channels.size())));
            }
        }
        return channels;
    }
}
