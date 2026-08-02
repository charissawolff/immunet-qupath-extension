package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.List;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.JsonDataUploadHandler;


import qupath.lib.objects.PathObject;

public class AddPolygonCommand extends AbstractAsyncCommand<JSONObject> {
    private final PathObject polygon;
    private final JsonDataUploadHandler dataUploadHandler;
    private JSONObject polygonJson;

    public AddPolygonCommand(PathObject polygon, JsonDataUploadHandler dataUploadHandler) {
        this.polygon = polygon;
        this.dataUploadHandler = dataUploadHandler;
    }

    @Override
    protected String getThreadName() {
       return "add-polygon-" + polygon.getMetadata().get("id");
    }

    @Override
    protected JSONObject execute(Consumer<String> progressReporter) throws Exception {
    //now rest of information to get format we needed: 
        try {
            JSONObject response = dataUploadHandler.uploadPolygonAnnotations(new JSONArray(List.of(polygonJson)));
            return response;
        } catch (Exception e) {
            ImmuNetLog.error("Could not upload polygon data", e);
            throw new RuntimeException("Could not upload polygon data", e);
        }
    }

    
}
