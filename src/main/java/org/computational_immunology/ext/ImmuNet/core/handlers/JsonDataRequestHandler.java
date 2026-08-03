package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.json.JSONArray;

public class JsonDataRequestHandler {
    private final PageFetcher pageFetcher;

    public JsonDataRequestHandler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public JSONArray getWebpageAsJson(String localPath) {
        JSONArray jsonArray = new JSONArray();
        try {
            String json = pageFetcher.fetchStringPage(localPath).body();
            jsonArray = new JSONArray(json);

        } catch (Exception e) {
            ImmuNetLog.error("Error in fetching webpage list of items. Localpath: " + localPath, e);
        }
        return jsonArray;
    }
}
