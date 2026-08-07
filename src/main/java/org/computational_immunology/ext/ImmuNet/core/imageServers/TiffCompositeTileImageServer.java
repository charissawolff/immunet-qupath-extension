package org.computational_immunology.ext.ImmuNet.core.imageServers;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.TiffImageRequestHandler;
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

public class TiffCompositeTileImageServer extends TileImageServer {
    private final double downsampleValue;
    private final TiffImageRequestHandler imageRequestHandler;
    private final ImageServerMetadata metadata;

    public TiffCompositeTileImageServer(DatasetMetadata datasetMetadata, 
        TileMetadata tileMetadata, String datasetName,
            String slideName, double downsampleValue, 
            TiffImageRequestHandler imageRequestHandler) {
        super(tileMetadata, datasetName, slideName);
        this.downsampleValue = downsampleValue;
        this.imageRequestHandler = imageRequestHandler;

        int fullWidth  = tileMetadata.getPixelWidth();
        int fullHeight = tileMetadata.getPixelHeight();
        int levelWidth  = Math.max(1, (int) Math.round(fullWidth  / downsampleValue));
        int levelHeight = Math.max(1, (int) Math.round(fullHeight / downsampleValue));
        double declaredDownsample = fullWidth / (double) levelWidth;

        this.metadata = new ImageServerMetadata.Builder()
                .width(fullWidth).height(fullHeight)
                .name(datasetName + "/" + slideName + "/" + tileMetadata.getCode() + " (" + tileMetadata.getType() + ")")
                .rgb(false).pixelType(PixelType.FLOAT32)
                .channels(toImageChannels(datasetMetadata.getAntibodyPanel()))
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
        try {
            Tile fetchedTile = imageRequestHandler.fetchComponentsTiffImage(tileMetadata, datasetName, slideName);
            ImmuNetLog.log("Fetching TIFF tile of type {}", tileMetadata.getType());
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
    protected BufferedImage blankTile(int width, int height) {
        int numChannels = metadata.getChannels().size();
        WritableRaster raster = TiffImageRequestHandler.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, numChannels);
        var dummyColorModel = qupath.lib.color.ColorModelFactory.getDummyColorModel(32 * numChannels);
        return new BufferedImage(dummyColorModel, raster, false, null);
    }

    @Override
    protected String createID() {
        return datasetName + "/" + slideName + "/" + tileMetadata.getCode() + "-COMPOSITE-TIFF";
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
