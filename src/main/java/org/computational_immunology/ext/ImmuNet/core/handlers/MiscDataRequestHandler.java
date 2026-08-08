package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

public class MiscDataRequestHandler extends DataRequestHandler {
    private final String DATASET_PATH = "v/datasets/";
    private final String SLIDE_PATH = "v/datasets/%s/";
    private final String DATASET_METADATA_PATH = "v/datasets/%s";

    public MiscDataRequestHandler(PageFetcher pageFetcher) {
        super(pageFetcher);
    }

    public JSONArray getAllDatasets() throws IOException, InterruptedException {
        return getWebpageAsJsonArray(DATASET_PATH);
    }

    public JSONArray getAllSlides(String datasetName) throws IOException, InterruptedException {
        String path = String.format(SLIDE_PATH, datasetName);
        return getWebpageAsJsonArray(path);
    }

    public JSONObject getDatasetMetadata(String datasetName) throws IOException, InterruptedException {
        String path = String.format(DATASET_METADATA_PATH, datasetName);
        return getWebpageAsJsonObject(path);
    }
}
