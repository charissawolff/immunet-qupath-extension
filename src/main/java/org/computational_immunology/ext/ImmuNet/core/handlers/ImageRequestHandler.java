package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Tile;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ImageRequestHandler {
    private static final String TILE_IMAGE_PATH_FORMAT = "v/datasets/%s/%s/%s/%s.jpg"; // datasetName, slideName, tileCode, imageType
    private static final String TILEMETADATAPATH_FORMAT = "v/datasets/%s/%s/"; // datasetName, slideName
    private final PageFetcher pageFetcher;
    private static final int MAX_CONCURRENT_IMAGE_DECODES = 4;
    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_IMAGE_DECODES);

    public ImageRequestHandler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    // Get list of tiles belonging to a slide
    public List<TileMetadata> getAllTileMetadatas(String datasetName, String slideName) throws IOException, InterruptedException {
        String path = String.format(TILEMETADATAPATH_FORMAT, datasetName, slideName);
        List<TileMetadata> tileMetadatas;
        String allTilesJson = pageFetcher.fetchStringPage(path).body();

        ImmuNetLog.log("Path at getAllTileMetadatas is " + path);
        ImmuNetLog.log(allTilesJson);

        JSONArray parsedOutput = new JSONArray(allTilesJson);
        ImmuNetLog.log("Fetched all tiles json");
        tileMetadatas = jsonToTileMetadatas(parsedOutput, ImageType.THUMB);
        return tileMetadatas;
    }

    public Tile fetchTileImage(TileMetadata tileMetadata, String datasetName, String slideName) throws IOException, InterruptedException {
        // Fetch the image for a specific tile using its metadata and the dataset/slide names. 
        // Check for null image and throw IOException if the image cannot be decoded.
        // The application crashes when I try to load in too fast of the composite images, so I added a semaphore to limit how many images are read
        // at the same time
        
        String path = String.format(TILE_IMAGE_PATH_FORMAT, datasetName, slideName, tileMetadata.getCode(), tileMetadata.getType().toString());
        var response = pageFetcher.fetchPage(path);
        if (response == null) {
            throw new IOException("Could not fetch tile image for tile code: " + tileMetadata.getCode() + " at path: " + path);
        }
        byte[] imageBytes;
        try (InputStream imageInputStream = response.body()) {
            imageBytes = imageInputStream.readAllBytes();
        }
        semaphore.acquire();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IOException("Could not decode image data for tile code: " + tileMetadata.getCode() + " at path: " + path);
            }
            return new Tile(tileMetadata, image);
        } catch (IOException e) {
            ImmuNetLog.error("Error fetching tile image for tile code: " + tileMetadata.getCode() + " at path: " + path, e);
            throw e;
        } finally {
            semaphore.release();
        }
    }



    // Convert json to list of tiles
    private List<TileMetadata> jsonToTileMetadatas(JSONArray array, ImageType type) throws IOException, InterruptedException {
        List<TileMetadata> tileMetadatas = new ArrayList<>();
        ImmuNetLog.log("converting json to tiles");
        for (int i = 0; i < array.length(); i++) {
            JSONObject tile = array.getJSONObject(i);
            tileMetadatas.add(jsonToTileMetadata(tile, type));
        }
        ImmuNetLog.log("tiles converted");
        return tileMetadatas;
    }

    public List<TileMetadata> testJsonToTileMetadatas(JSONArray array, ImageType type) throws IOException, InterruptedException {
        return jsonToTileMetadatas(array, type);
    }

    private TileMetadata jsonToTileMetadata(JSONObject json, ImageType type) throws IOException, JSONException {
        return new TileMetadata(
                json.getInt("id"),
                json.getString("code"),
                type,
                json.getDouble("x"),
                json.getDouble("y"),
                json.getDouble("width"),
                json.getDouble("height")
        );
    }

    public TileMetadata testJsonToTileMetadata(JSONObject json, ImageType type) throws JSONException, IOException {
        return jsonToTileMetadata(json, type);
    }

    /**
     * Fetch a list of format JSON from v/{webpageString} and converts to List of type String.
     *
     * @param localPath path to webpage
     * @return list of type String obtained from JSON webpage OR empty list on exception
     */
    public List<String> getWebpageAsList(String localPath) {
        List<String> datasetList = new ArrayList<>();
        try {
            String json = pageFetcher.fetchStringPage("v/" + localPath).body();
            JSONArray parsedDatasetList = new JSONArray(json);

            // Add each element of the JSONArray to the list to convert types
            for (int i = 0; i < parsedDatasetList.length(); i++) {
                datasetList.add(parsedDatasetList.getString(i));
            }
        } catch (Exception e) {
            ImmuNetLog.error("Error in fetching webpage list of items. Localpath: " + localPath, e);
        }
        return datasetList;
    }
}
