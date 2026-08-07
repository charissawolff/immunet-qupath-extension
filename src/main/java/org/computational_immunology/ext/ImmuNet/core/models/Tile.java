package org.computational_immunology.ext.ImmuNet.core.models;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;

public class Tile {
    private final TileMetadata metadata;
    private final BufferedImage image;

    public Tile(TileMetadata metadata, BufferedImage image) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata cannot be null");
        }
        if (image == null) {
            throw new IllegalArgumentException("image cannot be null");
        }
        this.metadata = metadata;
        this.image = image;
    }

    public TileMetadata getMetadata() {
        return metadata;
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getCode() {
        return metadata.code();
    }

    public TileMetadata.ImageType getType() {
        return metadata.type();
    }

    public int getId() {
        return metadata.id();
    }

    public double getTileX() {
        return metadata.x();
    }

    public double getTileY() {
        return metadata.y();
    }

    public double getTileW() {
        return metadata.w();
    }

    public double getTileH() {
        return metadata.h();
    }

    //todo: rename this to indicate that we can also change the buffered image type. Intentional as with qupath, the viewer
    // has to always have the correct type to show
    public BufferedImage resizeJpgImage(int targetWidth, int targetHeight, boolean qualityOverSpeed, int... bufferedImageType) {
        int imageType = bufferedImageType.length > 0 ? bufferedImageType[0] : image.getType();
        BufferedImage bufferedImg = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D g2d = bufferedImg.createGraphics();

        if (qualityOverSpeed) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return bufferedImg;
    }

    public BufferedImage resizeTiffImage(int targetWidth, int targetHeight) {
        WritableRaster originalRaster = image.getRaster();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int numBands = originalRaster.getNumBands();

        WritableRaster resizedRaster = originalRaster.createCompatibleWritableRaster(targetWidth, targetHeight);
        for (int y = 0; y < targetHeight; y++) {
            int originalY = Math.min(imageHeight - 1, y * imageHeight / targetHeight);
            for (int x = 0; x < targetWidth; x++) {
                int originalX = Math.min(imageWidth - 1, x * imageWidth / targetWidth);
                for (int band = 0; band < numBands; band++) {
                    resizedRaster.setSample(x, y, band, originalRaster.getSampleFloat(originalX, originalY, band));
                }
            }
        }
        return new BufferedImage(image.getColorModel(), resizedRaster, false, null);
    }

}
