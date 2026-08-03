package org.computational_immunology.ext.ImmuNet.core.handlers;

import org.json.JSONArray;

public class MiscDataRequestHandler extends JsonDataRequestHandler {
    private final String DATASET_PATH = "v/datasets/";
    private final String SLIDE_PATH = "v/datasets/%s/";

    public MiscDataRequestHandler(PageFetcher pageFetcher) {
        super(pageFetcher);
    }

    public JSONArray getAllDatasets() {
        return getWebpageAsJson(DATASET_PATH);
    }

    public JSONArray getAllSlides(String datasetName) {
        String path = String.format(SLIDE_PATH, datasetName);
        return getWebpageAsJson(path);
    }
}
