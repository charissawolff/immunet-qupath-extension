package org.computational_immunology.ext.ImmuNet.core;

import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.AbstractImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;


public class TileImageServer extends AbstractImageServer<BufferedImage> {
    private TileMetadata tileMetadata;
    private String datasetName;
    private String slideName;
    private ImageRequestHandler imageRequestHandler;
    

    public TileImageServer(TileMetadata tileMetadata, String datasetName, String slideName, ImageRequestHandler imageRequestHandler) {
        super(BufferedImage.class);
        this.tileMetadata = tileMetadata;
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.imageRequestHandler = imageRequestHandler;
    }

    @Override
    public synchronized ImageServerMetadata getMetadata() {
        try {
            final int width = (int)tileMetadata.getWidth();
            final int height = (int)tileMetadata.getHeight();

            return new ImageServerMetadata.Builder()
                    .width(width)
                    .height(height)
                    // Temporary: code stands in for a real identifier until dataset/slide
                    // context (SlideRef) is threaded through to StreamedImageServer.
                    .name(tileMetadata.getCode())
                    .channels(ImageChannel.getDefaultRGBChannels())
                    .sizeZ(0)
                    .sizeT(0)
                    .rgb(true)
                    .pixelType(PixelType.UINT8)
                    .preferredTileSize(width,height).build();
        } catch (Exception e) {
            ImmuNetLog.error("Couldn't get metadata of ImageServer", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return ImageServerBuilder.DefaultImageServerBuilder.createInstance(TileImageServerBuilder.class,
                URI.create(createID()), "");
    }

    @Override
    protected String createID() {
        // Must include type, not just code: SparseImageServerManager caches/deduplicates child
        // servers by the builder this ID produces. Each physical tile is registered twice (once
        // per resolution), so code alone would make the thumb and composite variants collide onto
        // the same cache entry, whichever got registered first silently wins for both, regardless
        // of which downsample was actually requested.
        return tileMetadata.getCode() + "-" + tileMetadata.getType();
    }

    @Override
    public Collection<URI> getURIs() {
        // Same reason as createID()
        return List.of(URI.create(tileMetadata.getCode() + "-" + tileMetadata.getType()));
    }

    @Override
    public BufferedImage readRegion(RegionRequest request) throws IOException {
        ImmuNetLog.log("readRegion: type={} code={} downsample={} request=({},{} {}x{})",
                tileMetadata.getType(), tileMetadata.getCode(), request.getDownsample(),
                request.getX(), request.getY(), request.getWidth(), request.getHeight());
        try {
            Tile fetchedTile = imageRequestHandler.fetchTileImage(tileMetadata, datasetName, slideName);
            BufferedImage image = fetchedTile.getImage();
            double downsample = request.getDownsample();

            // request.getDownsample() is the *registered* level's own value, not necessarily the
            // real fetched image's true native resolution (e.g. THUMB_ZOOM_BUFFER_FACTOR deliberately
            // registers thumb smaller than it really is, as a hack to let users zoom in before changing to composite). 
            // Resize to whatever size this request's
            // downsample implies for the whole tile first, so the geometry is self-consistent
            // regardless of any mismatch between registered and true resolution.
            int expectedFullWidth = (int) Math.round(tileMetadata.getWidth() / downsample);
            int expectedFullHeight = (int) Math.round(tileMetadata.getHeight() / downsample);
            if (image.getWidth() != expectedFullWidth || image.getHeight() != expectedFullHeight) {
                image = Tile.resizeImage(image, expectedFullWidth, expectedFullHeight, true);
            }

            int x = (int) Math.round(request.getX() / downsample);
            int y = (int) Math.round(request.getY() / downsample);
            int requestedWidth = (int) Math.round(request.getWidth() / downsample);
            int requestedHeight = (int) Math.round(request.getHeight() / downsample);

            x = Math.min(x, image.getWidth());
            y = Math.min(y, image.getHeight());
            int width = Math.min(requestedWidth, image.getWidth() - x);
            int height = Math.min(requestedHeight, image.getHeight() - y);
            if (width <= 0 || height <= 0) {
                return new BufferedImage(requestedWidth, requestedHeight, image.getType());
            }
            return image.getSubimage(x, y, width, height);
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Error fetching tile image", e);
            throw new IOException("Error fetching tile image that exists according to the database. Tile code: " + tileMetadata.getCode(), e);
        }
    }

    @Override
    public String getServerType() {
        return "StreamedImageServer";
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return null;
    }
}
