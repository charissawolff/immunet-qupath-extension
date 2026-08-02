package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Polygon;
import org.computational_immunology.ext.ImmuNet.core.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.handlers.JsonDataUploadHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.concurrent.Task;
import qupath.lib.objects.PathObject;

public class AddPolygonCommand {
    private final PathObject polygon;
    private final JsonDataUploadHandler dataUploadHandler;
    private JSONObject polygonJson;
    private Runnable onDone;
    private Runnable onFailed;
    private Task<JSONObject> task;

    public AddPolygonCommand(PathObject polygon, JsonDataUploadHandler dataUploadHandler) {
        this.polygon = polygon;
        this.dataUploadHandler = dataUploadHandler;
    }

    public void build() {
        Polygon polygonData = PolygonConverter.fromPathObject(polygon);
        polygonJson = PolygonConverter.toJSONObject(polygonData);
        task = new Task<>() {
            @Override
            protected JSONObject call() {
                return postNewPolygon();
            }
        };
    }

    public void start() {
        task.setOnSucceeded(event -> {
            JSONObject response = task.getValue();
            ImmuNetLog.log("Successfully uploaded polygon with ID: " + polygon.getMetadata().get("id") + ". Server response: " + response.toString());
            if (onDone != null) {
                onDone.run();
            }
        });
        task.setOnFailed(event -> {
            ImmuNetLog.error("Could not upload polygon data", task.getException());
            if (onFailed != null) onFailed.run(); 
        });

        Thread thread = new Thread(task, "upload-polygon-" + polygon.getMetadata().get("id"));
        thread.setDaemon(true);
        thread.start();
    }

    private JSONObject postNewPolygon() {
        //now rest of information to get format we needed: 
        try {
            JSONObject response = dataUploadHandler.uploadPolygonAnnotations(new JSONArray(List.of(polygonJson)));
            return response;
        } catch (Exception e) {
            ImmuNetLog.error("Could not upload polygon data", e);
            throw new RuntimeException("Could not upload polygon data", e);
        }
    }

    public Task<JSONObject> getTask() {
        return task;
    }

    public void setOnDone(Runnable callback) {
        this.onDone = callback;
    }

    public void setOnFailed(Runnable callback) {
        this.onFailed = callback;
    }
    
}
