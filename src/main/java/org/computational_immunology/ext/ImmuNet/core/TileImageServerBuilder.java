package org.computational_immunology.ext.ImmuNet.core;

import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

public class TileImageServerBuilder implements ImageServerBuilder<BufferedImage> {
    TileMetadata tileMetadata;
    String datasetName;
    String slideName;
    double downsampleValue;
    ImageRequestHandler imageRequestHandler;

    @Override
    public UriImageSupport<BufferedImage> checkImageSupport(URI uri, String... args) throws IOException {
        return null;
    }

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) throws Exception {
        throw new UnsupportedOperationException(
            "TileImageServer cannot be reconstructed from a URI alone, it needs a live ImageRequestHandler...");
    }

    public TileMetadata.ImageType getType() {
    return tileMetadata.getType();
}

    @Override
    public String getName() {
        return "Streamed Image Server Builder";
    }

    @Override
    public String getDescription() {
        return "Builder for streamed png.";
    }

    @Override
    public Class<BufferedImage> getImageType() {
        return BufferedImage.class;
    }
}
