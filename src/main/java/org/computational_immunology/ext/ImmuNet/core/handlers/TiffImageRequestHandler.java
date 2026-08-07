package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

public class TiffImageRequestHandler extends ImageRequestHandler {
    private static final String TIFF_COMPONENTS_TILE_PATH_FORMAT = "v/datasets/%s/%s/%s/components.tiff"; //dataset, slide and tile
    private static final int NUM_CHANNELS = 7;
    private static final int MAX_CONCURRENT_COMPONENT_DECODES = 4;
    private final Semaphore componentsSemaphore = new Semaphore(MAX_CONCURRENT_COMPONENT_DECODES);

    public TiffImageRequestHandler(PageFetcher pageFetcher) {
        super(pageFetcher);
    }

    public Tile fetchComponentsTiff(TileMetadata tileMetadata, String datasetName, String slideName)
            throws IOException, InterruptedException {
        String path = String.format(TIFF_COMPONENTS_TILE_PATH_FORMAT, datasetName, slideName, tileMetadata.getCode());
        byte[] bytes = fetchBytes(path); 

        componentsSemaphore.acquire();
        try {
            BufferedImage image = decodeChannels(bytes);
            return new Tile(tileMetadata, image);
        } catch (IOException e) {
            ImmuNetLog.error("Error decoding components.tiff for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e;
        } finally {
            componentsSemaphore.release();
        }
    }

    private BufferedImage decodeChannels(byte[] bytes) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) {
            throw new IOException("No TIFF reader available");
        }
        ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(iis);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            WritableRaster raster = Raster.createBandedRaster(DataBuffer.TYPE_BYTE, width, height, NUM_CHANNELS, null);

            for (int band = 0; band < NUM_CHANNELS; band++) {
                BufferedImage page = reader.read(band);
                int[] samples = page.getRaster().getSamples(0, 0, width, height, 0, (int[]) null);
                raster.setSamples(0, 0, width, height, band, samples);
            }

            // dummy color model here just to satisfy Tile's BufferedImage field.
            var dummyColorModel = qupath.lib.color.ColorModelFactory.getDummyColorModel(8 * NUM_CHANNELS);
            return new BufferedImage(dummyColorModel, raster, false, null);
        } finally {
            reader.dispose();
        }
    }
}
