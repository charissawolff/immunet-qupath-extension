package org.computational_immunology.ext.ImmuNet.core.imageServers;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * {@link TileImageServer} that serves a single tile as an RGB JPEG image at one declared
 * downsample level. Fetches the tile from the backend via {@link ServerGateway} and resizes it
 * locally to whatever tile size QuPath requests.
 */


public class JpgTileImageServer extends TileImageServer {
    private final ServerGateway serverGateway;
    private final ImageServerMetadata metadata;

    /**
     * Builds a single tile RGB image server for the given tile, declaring one resolution level at downsampleValue.
     * @param tileMetadata metadata describing the tile (pixel size, position, code, type) fetched from server
     * @param datasetName the dataset the tile belongs to
     * @param slideName the slide the tile belongs to
     * @param downsampleValue the downsample value this image server represents
     * @param serverGateway used to fetch the tile image from the backend when calling readTile
     */

    public JpgTileImageServer(TileMetadata tileMetadata, String datasetName, String slideName, double downsampleValue, ServerGateway serverGateway) {
        super(tileMetadata, datasetName, slideName);
        this.serverGateway = serverGateway;

        int fullWidth  = tileMetadata.getPixelWidth();
        int fullHeight = tileMetadata.getPixelHeight();
        int levelWidth  = Math.max(1, (int) Math.round(fullWidth  / downsampleValue));
        int levelHeight = Math.max(1, (int) Math.round(fullHeight / downsampleValue));
        double declaredDownsample = fullWidth / (double) levelWidth;

        double dx =tileMetadata.getDx(); // pixel per um
        double dy = tileMetadata.getDy();

        this.metadata = new ImageServerMetadata.Builder()
                .width(fullWidth).height(fullHeight)
                .name(datasetName + "/" + slideName + "/" + tileMetadata.getCode() + " (" + tileMetadata.getType() + ")")
                .rgb(true).pixelType(PixelType.UINT8)
                .channels(ImageChannel.getDefaultRGBChannels())
                .pixelSizeMicrons(dx, dy) 
                .sizeZ(1).sizeT(1)
                .levels(new ImageServerMetadata.ImageResolutionLevel.Builder(fullWidth, fullHeight)
                        .addLevel(declaredDownsample, levelWidth, levelHeight)
                        .build())
                .preferredTileSize(levelWidth, levelHeight)
                .build();
    }

    /**
     * {@inheritDoc}
     * Fetches the tile's JPEG image from the backend via {@link ServerGateway#fetchTileImage} and
     * resizes it to the requested tile dimensions. Returns a blank tile if the fetch fails.
     * @throws IOException if fetching the blanktile results in exception.
     */
    @Override
    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        int requestedWidth = tileRequest.getTileWidth();
        int requestedHeight = tileRequest.getTileHeight();
        try {
            Tile fetchedTile = serverGateway.fetchTileImage(datasetName, slideName, tileMetadata);
            ImmuNetLog.log("Fetching tile of type {}", tileMetadata.getType());
            return fetchedTile.resizeJpgImage(requestedWidth, requestedHeight, false, 1);
        } catch (InterruptedException e) {
            ImmuNetLog.log("InterruptedException while reading Tile in Tile image server");
            return null;
        } catch (IOException e) {
            ImmuNetLog.log("Could not fetch tile image for tile code: " + tileMetadata.getCode() + " at dataset: " + datasetName + ", slide: " + slideName, e);
            return blankTile(requestedWidth, requestedHeight);
        }
    }

    @Override
    protected String createID() {
        return datasetName + "/" + slideName + "/" + tileMetadata.getCode() + "-" + tileMetadata.getType();
    }

    @Override
    public String getServerType() {
        return "JpgTileImageServer";
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return metadata;
    }
}
