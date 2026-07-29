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

import org.computational_immunology.ext.ImmuNet.core.Annotation;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Tile;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class AnnotationRequestHandler {
    private final PageFetcher pageFetcher;
    private static final String ANNOTATIONS = "v/datasets/%s/%s/"; // datasetName, slideName. Used for slide AND tile level...

    public AnnotationRequestHandler(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public List<Annotation> fetchAnnotations(String dataset, String slide) throws IOException, JSONException {
        String path = String.format(ANNOTATIONS, dataset, slide);
        List<Annotation> annotations = new ArrayList<>();

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

    private Annotation jsonToAnnotation(JSONObject json) throws JSONException {
    return new Annotation(
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
