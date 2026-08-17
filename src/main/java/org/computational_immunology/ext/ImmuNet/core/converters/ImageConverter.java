package org.computational_immunology.ext.ImmuNet.core.converters;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;

public class ImageConverter {

    private static final int MAX_CONCURRENT_THUMB_DECODES = 16;
    private static final int MAX_CONCURRENT_COMPOSITE_DECODES = 4;
    private static final Semaphore thumbSemaphore = new Semaphore(MAX_CONCURRENT_THUMB_DECODES);
    private static final Semaphore compositeSemaphore = new Semaphore(MAX_CONCURRENT_COMPOSITE_DECODES);

    private ImageConverter() {
    }

    public static BufferedImage imageFromBytes(byte[] bytes, TileMetadata.ImageType type) throws IOException, InterruptedException {
        Semaphore semaphore = type == TileMetadata.ImageType.THUMB ? thumbSemaphore : compositeSemaphore;
        semaphore.acquire();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IOException("Could not decode image data");
            }
            return image;
        } finally {
            semaphore.release();
        }
    }
}
