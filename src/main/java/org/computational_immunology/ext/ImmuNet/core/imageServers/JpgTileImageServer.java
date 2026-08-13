package org.computational_immunology.ext.ImmuNet.core.imageServers;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class JpgTileImageServer extends TileImageServer {
    private final double downsampleValue;
    private final ServerGateway serverGateway;
    private final ImageServerMetadata metadata;

    public JpgTileImageServer(TileMetadata tileMetadata, String datasetName, String slideName, double downsampleValue, ServerGateway serverGateway) {
        super(tileMetadata, datasetName, slideName);
        this.downsampleValue = downsampleValue;
        this.serverGateway = serverGateway;

        int fullWidth  = tileMetadata.getPixelWidth();
        int fullHeight = tileMetadata.getPixelHeight();
        int levelWidth  = Math.max(1, (int) Math.round(fullWidth  / downsampleValue));
        int levelHeight = Math.max(1, (int) Math.round(fullHeight / downsampleValue));
        double declaredDownsample = fullWidth / (double) levelWidth;

        double dx =tileMetadata.getDx(); //µm per pixel while tilemetadata has pixels per µm, so we need to invert it to get the correct value for qupath
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

    @Override
    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        int requestedWidth = tileRequest.getTileWidth();
        int requestedHeight = tileRequest.getTileHeight();
        try {
            Tile fetchedTile = serverGateway.fetchTileImage(datasetName, slideName, tileMetadata);
            ImmuNetLog.log("Fetching tile of type {}", tileMetadata.getType());
            return fetchedTile.resizeJpgImage(requestedWidth, requestedHeight, false, 1);
        } catch (InterruptedException e) {
            ImmuNetLog.log("Error in reading Tile in Tile image server");
            throw new IOException("Interrupted fetching tile " + tileMetadata.getCode(), e);
        } catch (IOException e) {
            ImmuNetLog.log("Could not fetch tile image for tile code: " + tileMetadata.getCode() + " at dataset: " + datasetName + ", slide: " + slideName, e);
            return blankTile(requestedWidth, requestedHeight);
        }
    }

    @Override
    protected BufferedImage blankTile(int width, int height) {
        BufferedImage blankImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return blankImage;
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
