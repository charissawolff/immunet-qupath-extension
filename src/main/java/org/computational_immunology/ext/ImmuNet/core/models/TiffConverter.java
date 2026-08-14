package org.computational_immunology.ext.ImmuNet.core.models;

import java.awt.Point;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/*
Used to convert bytes from an image from the server into the necessary layered images
*/
public class TiffConverter {

    private static final int MAX_CONCURRENT_TIFF_COMPONENT_FETCHES = 10;
    private static final Semaphore componentsTiffSemaphore = new Semaphore(MAX_CONCURRENT_TIFF_COMPONENT_FETCHES);

    private TiffConverter() {
        /* should not be initialized */
    }

    public static BufferedImage imageFromBytes(byte[] bytes) throws IOException, InterruptedException {
        componentsTiffSemaphore.acquire();
        try {
            return decode(bytes);
        } finally {
            componentsTiffSemaphore.release();
        }
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        ImagePlus imp = new Opener().openTiff(new ByteArrayInputStream(bytes), "components");
        if (imp == null) {
            throw new IOException("Could not decode components.tiff with ImageJ");
        }
        ImageStack stack = imp.getStack();

        // Instead of assuming how many channels there are, group slices by their actual (width, height) and
        // take the largest group as the channels because components.tiff also contains a smaller THUMB image,
        // which won't match that size and is excluded this way rather than by a hardcoded slice count.
        Map<List<Integer>, List<ImageProcessor>> slicesBySize = new LinkedHashMap<>();
        for (int i = 1; i <= stack.getSize(); i++) {
            ImageProcessor ip = stack.getProcessor(i);
            List<Integer> size = List.of(ip.getWidth(), ip.getHeight());
            slicesBySize.computeIfAbsent(size, k -> new ArrayList<>()).add(ip);
        }
        List<ImageProcessor> channelProcessors = slicesBySize.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElseThrow(() -> new IOException("components.tiff has no slices"));

        int numChannels = channelProcessors.size();
        ImageProcessor firstProcessor = channelProcessors.get(0);
        int width = firstProcessor.getWidth();
        int height = firstProcessor.getHeight();

        int dataType = dataBufferTypeOf(firstProcessor);
        WritableRaster raster = createBandedRaster(dataType, width, height, numChannels);
        for (int band = 0; band < numChannels; band++) {
            copyBand(channelProcessors.get(band), raster, band);
        }

        var colorModel = qupath.lib.color.ColorModelFactory.createColorModel(pixelTypeOf(dataType), numChannels, false);
        return new BufferedImage(colorModel, raster, false, null);
    }

    private static int dataBufferTypeOf(ImageProcessor ip) {
        if (ip instanceof FloatProcessor) {
            return DataBuffer.TYPE_FLOAT;
        }
        if (ip instanceof ShortProcessor) {
            return DataBuffer.TYPE_USHORT;
        }
        return DataBuffer.TYPE_BYTE;
    }

    public static WritableRaster createBandedRaster(int dataType, int width, int height, int numBands) {
        if (dataType == DataBuffer.TYPE_FLOAT) {
            SampleModel sampleModel = new BandedSampleModel(DataBuffer.TYPE_FLOAT, width, height, numBands);
            DataBuffer dataBuffer = new DataBufferFloat(width * height, numBands);
            return Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        }
        return Raster.createBandedRaster(dataType, width, height, numBands, null);
    }

    private static void copyBand(ImageProcessor ip, WritableRaster dst, int band) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dst.setSample(x, y, band, ip.getf(x, y));
            }
        }
    }

    private static qupath.lib.images.servers.PixelType pixelTypeOf(int dataBufferType) {
        return switch (dataBufferType) {
            case DataBuffer.TYPE_USHORT -> qupath.lib.images.servers.PixelType.UINT16;
            case DataBuffer.TYPE_FLOAT -> qupath.lib.images.servers.PixelType.FLOAT32;
            default -> qupath.lib.images.servers.PixelType.UINT8;
        };
    }
}
