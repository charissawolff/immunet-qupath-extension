package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

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
