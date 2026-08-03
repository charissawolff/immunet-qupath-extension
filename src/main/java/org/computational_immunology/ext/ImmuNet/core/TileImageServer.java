package org.computational_immunology.ext.ImmuNet.core;

import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

/*
Image server for a TILE, which is what a SLIDE is made out of. The slide image server determines the position of each tile, and a tile
is called. We use AbstractTileableImageServer instead of AbstractImageServer because it offers better caching and calling of the tiles, with less
code. Furthermore, it offers better positioning making that when zooming or moving around the viewer, the tiles are not out of place.
This was done after many errors using AbstractImageServer.
*/

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
        /*
        This function is called when the sparse image server determines that the tile corresponding to this server is at this position, and since it
        is not in cache, it needs to be fetched from the server, resized and made the correct bufferedImageType to avoid crashes.
        */
        int requestedWidth = tileRequest.getTileWidth();
        int requestedHeight = tileRequest.getTileHeight();
        try {
            Tile fetchedTile = imageRequestHandler.fetchTileImage(vectraTileMetadata, datasetName, slideName);
            ImmuNetLog.log("Fetching tile of type {}",vectraTileMetadata.getType() );
            return fetchedTile.resizeImage(requestedWidth, requestedHeight, false, 1);
        } catch(InterruptedException e) {
            ImmuNetLog.log("Error in reading Tile in Tile image server");
            throw new IOException("Interrupted fetching tile " + vectraTileMetadata.getCode(), e); 
        } catch (IOException e) {
            ImmuNetLog.log("Could not fetch tile image for tile code: " + vectraTileMetadata.getCode() + " at dataset: " + datasetName + ", slide: " + slideName, e);
            return blankTile(requestedWidth, requestedHeight);
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

    private BufferedImage blankTile(int width, int height) {
        //GENERATE A black blank tile to avoid crashes when a tile cannot be fetched. 
        // This is better than returning null, which will crash the viewer.
        BufferedImage blankImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return blankImage;
    }
}
