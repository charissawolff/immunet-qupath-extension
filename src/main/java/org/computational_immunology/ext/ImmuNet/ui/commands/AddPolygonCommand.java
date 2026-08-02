package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Polygon;
import org.computational_immunology.ext.ImmuNet.core.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.JsonDataUploadHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonObject;

import javafx.concurrent.Task;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class AddPolygonCommand {
    private final PathObject polygon;
    private final SelectedDataStore selectedDataStore;
    private final JsonDataUploadHandler dataUploadHandler;
    private Runnable onDone;
    private Runnable onFailed;
    private Task<JSONObject> task;

    AddPolygonCommand(PathObject polygon, SelectedDataStore selectedDataStore, JsonDataUploadHandler dataUploadHandler) {
        this.polygon = polygon;
        this.selectedDataStore = selectedDataStore;
        this.dataUploadHandler = dataUploadHandler;
    }

    public void build() {
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
            ImmuNetLog.log("Successfully uploaded polygon with ID: " + polygon.getMetadata().get("id") + " for dataset: " + selectedDataStore.getSelectedSlide().getDatasetName() + ", slide: " + selectedDataStore.getSelectedSlide().getSlideName() + ". Server response: " + response.toString());
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
        Polygon polygonData = PolygonConverter.fromPathObject(polygon);
        JSONObject polygonJson = PolygonConverter.toJSONObject(polygonData);
        //now rest of information to get format we needed: 
        try {
            JSONObject response = dataUploadHandler.uploadPolygonAnnotations(new JSONArray(List.of(polygonJson)));
            return response;
        } catch (Exception e) {
            if (onFailed != null) onFailed.run();
            throw new RuntimeException("Could not upload polygon data", e);
        }
    }
    
}
