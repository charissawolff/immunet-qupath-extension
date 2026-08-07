package org.computational_immunology.ext.ImmuNet.core.imageServers;

import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

public abstract class TileImageServer extends AbstractTileableImageServer {

    protected final TileMetadata tileMetadata;
    protected final String datasetName;
    protected final String slideName;

    protected TileImageServer(TileMetadata tileMetadata, String datasetName, String slideName) {
        super();
        this.tileMetadata = tileMetadata;
        this.datasetName = datasetName;
        this.slideName = slideName;
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return ImageServerBuilder.DefaultImageServerBuilder.createInstance(TileImageServerBuilder.class,
                getURIs().iterator().next());
    }

    @Override
    protected abstract String createID();

    @Override
    public abstract String getServerType();

    protected abstract BufferedImage readTile(qupath.lib.images.servers.TileRequest tileRequest) throws java.io.IOException;

    @Override
    public abstract ImageServerMetadata getOriginalMetadata();

    @Override
    public Collection<URI> getURIs() {
        try {
            return List.of(new URI("ImmuNet", createID(), null));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("bad URI for tile " + tileMetadata.getCode(), e);
        }
    }

    protected static BufferedImage blankTile(int width, int height) {
        BufferedImage blankImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return blankImage;
    }
}
