package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonDataRequestHandler {
    private final PageFetcher pageFetcher;

    public JsonDataRequestHandler(PageFetcher pageFetcher) {
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
}
