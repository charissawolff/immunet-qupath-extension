package org.computational_immunology.ext.ImmuNet.core.handlers;

import org.json.JSONArray;
import org.json.JSONObject;

public class MiscDataRequestHandler extends JsonDataRequestHandler {
    private final String DATASET_PATH = "v/datasets/";
    private final String SLIDE_PATH = "v/datasets/%s/";
    private final String DATASET_METADATA_PATH = "v/datasets/%s";

    public MiscDataRequestHandler(PageFetcher pageFetcher) {
        super(pageFetcher);
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
}
