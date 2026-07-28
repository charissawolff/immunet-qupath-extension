package org.computational_immunology.ext.ImmuNet.core;

import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.AbstractImageServer;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class TileImageServer extends AbstractTileableImageServer {
    private final TileMetadata vectraTileMetadata;
    private final String datasetName;
    private final String slideName;
    private final double downsampleValue;
    private final ImageRequestHandler imageRequestHandler;
    private final ImageServerMetadata metadata;
    
    public TileImageServer(TileMetadata vectraTileMetadata, String datasetName, String slideName, double downsampleValue, ImageRequestHandler imageRequestHandler) {
        super();
        this.vectraTileMetadata = vectraTileMetadata;
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.downsampleValue = downsampleValue;
        this.imageRequestHandler = imageRequestHandler;

        int fullWidth  = (int) Math.round(vectraTileMetadata.getWidth());
        int fullHeight = (int) Math.round(vectraTileMetadata.getHeight());
        int levelWidth  = Math.max(1, (int) Math.round(fullWidth  / downsampleValue));
        int levelHeight = Math.max(1, (int) Math.round(fullHeight / downsampleValue));
        double declaredDownsample = fullWidth / (double) levelWidth;

        this.metadata = new ImageServerMetadata.Builder()
                .width(fullWidth).height(fullHeight)
                .name(datasetName + "/" + slideName + "/" + vectraTileMetadata.getCode() + " (" + vectraTileMetadata.getType() + ")")
                .rgb(true).pixelType(PixelType.UINT8)
                .channels(ImageChannel.getDefaultRGBChannels())
                .sizeZ(1).sizeT(1)
                .levels(new ImageServerMetadata.ImageResolutionLevel.Builder(fullWidth, fullHeight)
                        .addLevel(declaredDownsample, levelWidth, levelHeight)
                        .build())
                .preferredTileSize(levelWidth, levelHeight)
                .build();
        
    }

    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        int requestedWidth = tileRequest.getTileWidth();
        int requestedHeight = tileRequest.getTileHeight();
        try {
            Tile fetchedTile = imageRequestHandler.fetchTileImage(vectraTileMetadata, datasetName, slideName);
            return fetchedTile.resizeImage(requestedWidth, requestedHeight, false, 1);
        } catch(InterruptedException e) {
            ImmuNetLog.log("Error in reading Tile in Tile image server");
            throw new IOException("Interrupted fetching tile " + vectraTileMetadata.getCode(), e); 
        }
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return ImageServerBuilder.DefaultImageServerBuilder.createInstance(TileImageServerBuilder.class,
                getURIs().iterator().next());
    }

    @Override
    protected String createID() {
        return datasetName + "/" + slideName + "/" + vectraTileMetadata.getCode() + "-" + vectraTileMetadata.getType();
    }

    @Override
    public Collection<URI> getURIs() {
        try {
            return List.of(new URI("ImmuNet", createID(), null));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("bad URI for tile " + vectraTileMetadata.getCode(), e);
        }
    }

    @Override
    public String getServerType() {
        return "TileImageServer";
    }
    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return metadata;
    }
}
