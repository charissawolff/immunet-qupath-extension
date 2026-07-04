package org.computational_immunology;

import qupath.lib.images.servers.*;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;


public class StreamedImageServer extends AbstractImageServer<BufferedImage> {

    private final Tile OwnedTile;
    private final int downsample;

    private ImageServerMetadata metadata;

    public StreamedImageServer(Tile Tile) {
        this(Tile, 1);
    }

    /**
     * @param Tile       the tile whose pixels this server provides
     * @param downsample the resolution level this server represents (1 = full resolution).
     *                   Higher values serve a pre-shrunk copy so zoomed-out views don't
     *                   rescale the full-resolution image on every repaint.
     */
    public StreamedImageServer(Tile Tile, int downsample) {
        super(BufferedImage.class);
        this.OwnedTile = Tile;
        this.downsample = downsample;
    }

    private int levelWidth() {
        return Math.max(1, (int) OwnedTile.tileW / downsample);
    }

    private int levelHeight() {
        return Math.max(1, (int) OwnedTile.tileH / downsample);
    }

    @Override
    public synchronized ImageServerMetadata getOriginalMetadata() {
        if (metadata == null) {
            // Dimensions are known from the tile, so no network fetch is needed here.
            final int width = levelWidth();
            final int height = levelHeight();

            metadata = new ImageServerMetadata.Builder()
                    .width(width)
                    .height(height)
                    .name(createID())
                    .channels(ImageChannel.getDefaultRGBChannels())
                    .sizeZ(0)
                    .sizeT(0)
                    .rgb(true)
                    .pixelType(PixelType.UINT8)
                    .preferredTileSize(width, height).build();
        }
        return metadata;
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return ImageServerBuilder.DefaultImageServerBuilder.createInstance(StreamedImageServerBuilder.class,
                URI.create(createID()), "");
    }

    @Override
    protected String createID() {
        // Include the downsample so each pyramid level is a distinct server/builder.
        return OwnedTile.getPath() + "?downsample=" + downsample;
    }

    @Override
    public Collection<URI> getURIs() {
        return List.of(URI.create(createID()));
    }

    @Override
    public BufferedImage readRegion(RegionRequest request) throws IOException {
        // The tile serves (and caches) the pixels pre-shrunk to this resolution level.
        BufferedImage img = OwnedTile.getImage(downsample);

        // Requests arrive in full-resolution region coordinates; map them into this level.
        int x = (int) Math.round(request.getX() / (double) downsample);
        int y = (int) Math.round(request.getY() / (double) downsample);
        int w = (int) Math.round(request.getWidth() / (double) downsample);
        int h = (int) Math.round(request.getHeight() / (double) downsample);

        // Clamp to the level bounds to avoid RasterFormatException at the edges.
        x = Math.max(0, Math.min(x, img.getWidth() - 1));
        y = Math.max(0, Math.min(y, img.getHeight() - 1));
        w = Math.max(1, Math.min(w, img.getWidth() - x));
        h = Math.max(1, Math.min(h, img.getHeight() - y));

        BufferedImage sub = img.getSubimage(x, y, w, h);

        // Normally the requested downsample matches this level exactly and no work is
        // needed. If a coarser level was substituted (e.g. a small tile lacking this
        // level), scale the crop down to the size the sparse server expects.
        int outWidth = Math.max(1, (int) Math.round(request.getWidth() / request.getDownsample()));
        int outHeight = Math.max(1, (int) Math.round(request.getHeight() / request.getDownsample()));
        if (outWidth == sub.getWidth() && outHeight == sub.getHeight()) {
            return sub;
        }
        return Tile.resizeImage(sub, outWidth, outHeight, false);
    }

    @Override
    public String getServerType() {
        return "StreamedImageServer";
    }
}
