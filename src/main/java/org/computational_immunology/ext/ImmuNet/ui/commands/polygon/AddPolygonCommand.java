package org.computational_immunology.ext.ImmuNet.ui.commands.polygon;

import java.util.List;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.JsonDataUploadHandler;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;

public class AddPolygonCommand extends AbstractAsyncCommand<JSONObject> {
    private final JSONObject polygonJson;
    private final JsonDataUploadHandler dataUploadHandler;

    public AddPolygonCommand(JSONObject polygonJson, JsonDataUploadHandler dataUploadHandler) {
        this.polygonJson = polygonJson;
        this.dataUploadHandler = dataUploadHandler;
    }

    @Override
    protected void onSuccess(JSONObject result) {
        ImmuNetLog.log("Successfully uploaded polygon! result: " + result.toString());
    }

    @Override
    protected JSONObject execute(Consumer<String> progressReporter) throws Exception {
    //now rest of information to get format we needed: 
        try {
            progressReporter.accept("Uploading polygon data...");
            JSONObject response = dataUploadHandler.uploadPolygonAnnotations(new JSONArray(List.of(polygonJson)));
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Could not upload polygon data", e);
        }
    }
}
