package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

public class ServerRequestHandler extends JsonDataRequestHandler{
    private final PageFetcher pageFetcher;
    private final String DATASET_PATH = "v/datasets/";
    private final String SLIDE_PATH = "v/datasets/%s/";
    private final String DATASET_METADATA_PATH = "v/datasets/%s";
    private static final String TILE_IMAGE_PATH_FORMAT = "v/datasets/%s/%s/%s/%s.jpg"; // datasetName, slideName, tileCode, imageType
    private static final String TILEMETADATAPATH_FORMAT = "v/datasets/%s/%s/"; // datasetName, slideName

    public ServerRequestHandler(PageFetcher pageFetcher) {
        super(pageFetcher);
        this.pageFetcher = pageFetcher;
    }

    public JSONArray getAllDatasets() {
        return getWebpageAsJsonArray(DATASET_PATH);
    }

    public JSONArray getAllSlides(String datasetName) {
        String path = String.format(SLIDE_PATH, datasetName);
        return getWebpageAsJsonArray(path);
    }

    public JSONObject getDatasetMetadata(String datasetName) {
        String path = String.format(DATASET_METADATA_PATH, datasetName);
        return getWebpageAsJsonObject(path);
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

    public JSONArray getTileMetadatas(String datasetName, String slideName) throws IOException, InterruptedException {
        String path = String.format(TILEMETADATAPATH_FORMAT, datasetName, slideName);
        List<TileMetadata> tileMetadatas;
        String allTilesJson = pageFetcher.fetchStringPage(path).body();

        //ImmuNetLog.log("Path at getTileMetadatas is " + path);
        //ImmuNetLog.log(allTilesJson);

        JSONArray parsedOutput = new JSONArray(allTilesJson);
        ImmuNetLog.log("Fetched all tiles json");
        return parsedOutput;
    }



    
}
