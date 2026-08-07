package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.models.Polygon;
import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class AnnotationRequestHandler {
    private final PageFetcher pageFetcher;
    private static final String SLIDE_ANNOTATIONS = "/v/annotations/%s/%s/"; // datasetName, slideName
    private static final String TILE_ANNOTATIONS = "v/datasets/%s/%s/%s/annotations.json"; // datasetName, slideName, tileCode
    private static final String SLIDE_POLYGONS = "v/datasets/%s/%s/polygons.json"; // datasetName, slideName

    public AnnotationRequestHandler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public List<String> fetchSlideAnnotations(String dataset, String slide) throws IOException, JSONException, InterruptedException {
        // Fetches the list of tile codes that have annotations for a given dataset and slide
        
        String path = String.format(SLIDE_ANNOTATIONS, dataset, slide);
        List<String> tileCodes = new ArrayList<>();

        HttpResponse<String> response = pageFetcher.fetchStringPage(path);

        int status = response.statusCode();
        if (status == 404) {
            return tileCodes; // no annotations for this dataset/slide 
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Could not fetch annotations for dataset: " + dataset
                    + " with slide: " + slide + " (status " + status + ")");
        }

        String body = response.body().trim();
        if (body.isEmpty()) {
            return tileCodes; // empty body means there are no annotations
        }

        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            tileCodes.add(array.getString(i));
        }
        return tileCodes;
    }

    public List<AnnotationPoint> fetchAnnotations(String dataset, String slide, String tile) throws IOException, JSONException, InterruptedException {
        String path = String.format(TILE_ANNOTATIONS, dataset, slide, tile);
        List<AnnotationPoint> annotations = new ArrayList<>();

        HttpResponse<String> response = pageFetcher.fetchStringPage(path);

        int status = response.statusCode();
        if (status == 404) {
            return annotations; // no annotations for this dataset/slide 
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Could not fetch annotations for dataset: " + dataset
                    + " with slide: " + slide + " (status " + status + ")");
        }

        String body = response.body().trim();
        if (body.isEmpty()) {
            return annotations; // empty body means there are no annotations
        }

        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            annotations.add(jsonToAnnotation(array.getJSONObject(i)));
        }
        return annotations;
    }

    public List<Polygon> fetchPolygons(String dataset, String slide) throws IOException, JSONException, InterruptedException {
        String path = String.format(SLIDE_POLYGONS, dataset, slide);
        List<Polygon> polygons = new ArrayList<>();

        HttpResponse<String> response = pageFetcher.fetchStringPage(path);

        int status = response.statusCode();
        if (status == 404) {
            return polygons; // no polygons for this dataset/slide
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Could not fetch polygons for dataset: " + dataset
                    + " with slide: " + slide + " (status " + status + ")");
        }

        String body = response.body().trim();
        if (body.isEmpty()) {
            return polygons; // empty body means there are no polygons
        }

        JSONArray array = new JSONArray(body);
        for (int i = 0; i < array.length(); i++) {
            JSONObject jsonPolygon = array.getJSONObject(i);

            JSONArray jsonVertices = jsonPolygon.getJSONArray("vertices");
            List<Polygon.Vertex> vertices = new ArrayList<>(jsonVertices.length());
            for (int j = 0; j < jsonVertices.length(); j++) {
                JSONArray point = jsonVertices.getJSONArray(j); // e.g. [11255.70, 3696.45]
                double x = point.getDouble(0);
                double y = point.getDouble(1);
                vertices.add(new Polygon.Vertex(x, y));
            }

            Polygon polygon = new Polygon(
                    jsonPolygon.optString("_id", null),
                    vertices,
                    jsonPolygon.optString("name", null),
                    jsonPolygon.optString("dataset", null),
                    jsonPolygon.optString("slide", null),
                    jsonPolygon.optString("created", null)
            );
            polygons.add(polygon);
        }
        return polygons;
    }

    private AnnotationPoint jsonToAnnotation(JSONObject json) throws JSONException {
    return new AnnotationPoint(
        json.getString("_id"),
        json.getString("slide"),
        json.getString("dataset"),
        json.getString("tile"),
        json.getInt("x"),
        json.getInt("y"),
        json.getString("t"),
        json.getString("annotator"),
        json.getString("purpose"),
        json.getString("created")
    );
}




}
