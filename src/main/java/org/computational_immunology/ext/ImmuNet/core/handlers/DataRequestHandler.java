package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.json.JSONArray;
import org.json.JSONObject;

public class DataRequestHandler {
    private final PageFetcher pageFetcher;

    public DataRequestHandler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public JSONArray getWebpageAsJsonArray(String localPath) {
        JSONArray jsonArray = new JSONArray();
        try {
            String json = pageFetcher.fetchStringPage(localPath).body();
            jsonArray = new JSONArray(json);

        } catch (Exception e) {
            ImmuNetLog.error("Error in fetching webpage list of items. Localpath: " + localPath, e);
        }
        return jsonArray;
    }

    public JSONObject getWebpageAsJsonObject(String localPath) {
        JSONObject jsonObject = new JSONObject();
        try {
            String json = pageFetcher.fetchStringPage(localPath).body();
            jsonObject = new JSONObject(json);
        } catch (Exception e) {
            ImmuNetLog.error("Error in fetching webpage object. Localpath: " + localPath, e);
        }
        return jsonObject;
    }

    public byte[] fetchBytes(String path) throws IOException, InterruptedException {
        var response = pageFetcher.fetchPage(path);
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
