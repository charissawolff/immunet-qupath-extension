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
    ImageRequestHandler imageRequestHandler;

    @Override
    public UriImageSupport<BufferedImage> checkImageSupport(URI uri, String... args) throws IOException {
        return null;
    }

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) throws Exception {
        return new TileImageServer(tileMetadata, datasetName, slideName, imageRequestHandler);
    }

    public TileImageServerBuilder forTile(TileMetadata tileMetadata, String datasetName, String slideName, ImageRequestHandler imageRequestHandler) {
        this.tileMetadata = tileMetadata;
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.imageRequestHandler = imageRequestHandler;
        return this;
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
