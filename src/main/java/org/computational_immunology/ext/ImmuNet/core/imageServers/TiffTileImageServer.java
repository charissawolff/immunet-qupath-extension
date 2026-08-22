package org.computational_immunology.ext.ImmuNet.core.imageServers;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.api.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TileImageServer} that serves a single tile as a multi-channel float32 image at one
 * declared downsample level. Fetches the tile from the backend via {@link ServerGateway} and
 * resizes it locally to whatever tile size QuPath requests.
 */

public class TiffTileImageServer extends TileImageServer {
    private final double downsampleValue;
    private final ServerGateway serverGateway;
    private final ImageServerMetadata metadata;

    /**
     * Builds a single tile multichannel image server for the given tile, declaring one
     * resolution level at {@code downsampleValue}.
     * @param tileMetadata metadata describing the tile (pixel size, position, code, type)
     * @param datasetName the dataset the tile belongs to
     * @param slideName the slide the tile belongs to
     * @param channelList the names of the channels to expose, each assigned a color from a fixed palette
     * @param downsampleValue the downsample this server instance represents; determines the
     *                        declared resolution level and preferred tile size
     * @param serverGateway used to fetch the tile image from the backend
     */
    public TiffTileImageServer(
        TileMetadata tileMetadata, String datasetName,
        String slideName, List<String> channelList, double downsampleValue,
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
                .channels(toImageChannels(channelList))
                .pixelSizeMicrons(dx, dy) //
                .sizeZ(1).sizeT(1)
                .levels(new ImageServerMetadata.ImageResolutionLevel.Builder(fullWidth, fullHeight)
                        .addLevel(declaredDownsample, levelWidth, levelHeight)
                        .build())
                .preferredTileSize(levelWidth, levelHeight)
                .build();
    }

    /**
     * {@inheritDoc}
     * Fetches the tile's TIFF image from the backend via
     * {@link ServerGateway#fetchComponentsTiffImage}, passing along the requested tile's
     * downsample, and resizes it to the requested tile dimensions. Returns a blank tile if the
     * fetch fails with an {@link IOException}.
     * @throws IOException if fetching the tile is interrupted
     */
    @Override
    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        int requestedWidth = tileRequest.getTileWidth();
        int requestedHeight = tileRequest.getTileHeight();
        double downsample = tileRequest.getDownsample();

        try {
            ImmuNetLog.log("Fetching TIFF tile with downsample {} ", downsample);
            Tile fetchedTile = serverGateway.fetchComponentsTiffImage(tileMetadata, datasetName, slideName, (int) downsample);
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
        return "TiffTileImageServer";
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return metadata;
    }

    /**
     * Builds one {@link ImageChannel} per channel name, assigning each a color from a fixed
     * palette of 16 colors. This insures that the slides on the server, each channel will have a unique color.
     * @param channelList the names of the channels to build
     * @return the image channels, in the same order as {@code channelList}
     */
    private static List<ImageChannel> toImageChannels(List<String> channelList) {
        List<ImageChannel> channels = new ArrayList<>();
        List<int[]> defaultColors = new ArrayList<>();
        // Define 16 different colors for the channels to choose from (RGB values)
        defaultColors.add(new int[]{255, 0, 0}); // Red
        defaultColors.add(new int[]{0, 255, 0}); // Green
        defaultColors.add(new int[]{0, 0, 255}); // Blue
        defaultColors.add(new int[]{255, 255, 0}); // Yellow
        defaultColors.add(new int[]{255, 0, 255}); // Magenta
        defaultColors.add(new int[]{0, 255, 255}); // Cyan
        defaultColors.add(new int[]{255, 165, 0}); // Orange
        defaultColors.add(new int[]{128, 0, 128}); // Purple
        defaultColors.add(new int[]{0, 128, 0}); // Dark Green
        defaultColors.add(new int[]{128, 128, 128}); // Gray
        defaultColors.add(new int[]{255, 192, 203}); // Pink
        defaultColors.add(new int[]{165, 42, 42}); // Brown
        defaultColors.add(new int[]{0, 0, 128}); // Navy
        defaultColors.add(new int[]{255, 215, 0}); // Gold
        defaultColors.add(new int[]{0, 100, 0}); // Dark Green
        defaultColors.add(new int[]{75, 0, 130}); // Indigo

        for (String name : channelList) {
            int[] rgb = defaultColors.get(channels.size() % defaultColors.size());
            channels.add(ImageChannel.getInstance(name, ColorTools.packRGB(rgb[0], rgb[1], rgb[2])));
        }
        return channels;
    }
}
