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
            // it is in the shape [[[outer ring], [hole1], [hole2]]]
            // OR 
            
            //first check shape of json vertices
            String shape = outerShape(jsonVertices);
            switch (shape) {
                case "flat":
                   List<Polygon.Vertex> vertices = parseVertices(jsonVertices);
                    Polygon polygon = new Polygon(
                            jsonPolygon.optString("_id", null),
                            vertices,
                            null,
                            jsonPolygon.optString("name", null),
                            jsonPolygon.optString("dataset", null),
                            jsonPolygon.optString("slide", null),
                            jsonPolygon.optString("created", null)
                    );
                    polygons.add(polygon);
                    continue;
                case "nested": 
                    List<Polygon.Vertex> outerRing = parseVertices(jsonVertices.getJSONArray(0));
                    List<List<Polygon.Vertex>> holes = parseHoles(jsonVertices, 1);
                    Polygon polygon2 = new Polygon(
                        jsonPolygon.optString("_id", null),
                        outerRing,
                        holes,
                        jsonPolygon.optString("name", null),
                        jsonPolygon.optString("dataset", null),
                        jsonPolygon.optString("slide", null),
                        jsonPolygon.optString("created", null)
                    );
                    polygons.add(polygon2);
                    break;
            }
        }
        return polygons;
    }

    private static List<Polygon.Vertex> parseVertices(JSONArray jsonArray){
        List<Polygon.Vertex> vertices = new ArrayList<>(jsonArray.length());
            for (int j = 0; j < jsonArray.length(); j++) {
                JSONArray point = jsonArray.getJSONArray(j); // e.g. [11255.70, 3696.45]
                double x = point.getDouble(0);
                double y = point.getDouble(1);
                vertices.add(new Polygon.Vertex(x, y));
            }
        return vertices;
    }

    private static List<List<Polygon.Vertex>> parseHoles(JSONArray jsonArray, int startIndex) {
        List<List<Polygon.Vertex>> holes = new ArrayList<>(jsonArray.length() - startIndex);
        for (int j = startIndex; j < jsonArray.length(); j++) {
            holes.add(parseVertices(jsonArray.getJSONArray(j)));
        }
        return holes;
    }

    static String outerShape(JSONArray vertices) {
        boolean allFlat = true;
        boolean allNested = true;

        for (int i = 0; i < vertices.length(); i++) {
            JSONArray el = vertices.getJSONArray(i);
            boolean elIsNested = el.length() > 0 && el.get(0) instanceof JSONArray;
            if (elIsNested) allFlat = false;
            else allNested = false;
        }

        if (allFlat) return "flat";     // [[x], [y], [z]]  -> one level: array of arrays
        if (allNested) return "nested"; 
        return "mixed";                 // shouldn;t happen
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
