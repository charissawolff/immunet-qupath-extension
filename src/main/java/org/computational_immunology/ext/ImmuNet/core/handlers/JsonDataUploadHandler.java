package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;


public class JsonDataUploadHandler {
    private final PagePoster<JSONArray> pagePoster;
    private static final String CELL_ANNOTATION_UPLOAD = "/v/annotationstore/cells";
    private static final String POLYGON_ANNOTATION_UPLOAD = "/v/annotationstore/polygons";
    // for example, with cell annotation, its http://localhost:8082/v/annotationstore/cells
    // response is {"deleted": 0, "inserted": 1, "updated": 0}
    // request is [{"slide":"T02-16400-D_Scan1","dataset":"2020-01-31-phenotyping-paper-melanoma","tile":"55762,14303","x":664,"y":468,"t":"T cell","annotator":"chawolff","purpose":"training","created":"1785663209279"}]

    public JsonDataUploadHandler(PagePoster<JSONArray> pagePoster) {
        this.pagePoster = pagePoster;
    }
    

    public JSONObject uploadCellAnnotations(JSONArray cellAnnotations) throws IOException, InterruptedException {
        HttpResponse<String> response = pagePoster.postObject(CELL_ANNOTATION_UPLOAD, cellAnnotations);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Could not upload cell annotations (status " + status + ")");
        }
        return new JSONObject(response.body());
    }

    public JSONObject uploadPolygonAnnotations(JSONArray polygonAnnotations) throws IOException, InterruptedException {
        HttpResponse<String> response = pagePoster.postObject(POLYGON_ANNOTATION_UPLOAD, polygonAnnotations);
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Could not upload polygon annotations (status " + status + ")");
        }
        return new JSONObject(response.body());
    }
}

