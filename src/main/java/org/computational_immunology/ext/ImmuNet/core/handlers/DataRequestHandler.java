package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles data (JSON and Image) requests to the backend api. Delegates the actual HTTP request to a {@link PageFetcher} instance.
 * Provides the JSON or {@link BufferedImage} representation of the response for further processing.
 */

public class DataRequestHandler {
    private final PageFetcher pageFetcher;

    public DataRequestHandler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public JSONArray getWebpageAsJsonArray(String localPath) throws IOException, InterruptedException {
        String json = pageFetcher.fetchStringPage(localPath).body();
        return new JSONArray(json);
    }

    public JSONObject getWebpageAsJsonObject(String localPath) throws IOException, InterruptedException {
        String json = pageFetcher.fetchStringPage(localPath).body();
        return new JSONObject(json);
    }

    public byte[] fetchBytes(String path) throws IOException, InterruptedException {
        var response = pageFetcher.fetchImagePage(path);
        if (response == null) {
            throw new IOException("Could not fetch image at path: " + path);
        }
        try (InputStream imageInputStream = response.body()) {
            return imageInputStream.readAllBytes();
        }
    }

    /**
     * Fetches and decodes images into a BufferedImage.
     * Semaphore handles how many images are decoded simultaneously. This was added after too many simultaneous decoding leads to 
     * performance issues and crashes.
     * @param path the absolute path to the image resource on the server
     * @param semaphore a semaphore to control concurrent decoding of images.
     * @return a BufferedImage object representing the fetched image.
     * @throws IOException when the image data cannot be found or properly decoded.
     * @throws InterruptedException when user cancels the javafx task for fetching images.
     */

    public BufferedImage fetchImage(String path, Semaphore semaphore) throws IOException, InterruptedException {
        byte[] imageBytes = fetchBytes(path);
        semaphore.acquire();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IOException("Could not decode image data request at path: " + path);
            }
            return image;
        } finally {
            semaphore.release();
        }
    }
}
