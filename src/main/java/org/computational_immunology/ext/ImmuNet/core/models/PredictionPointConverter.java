package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.json.JSONObject;

import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * Utility class for converting PredictionAnnotationPoint objects to and from JSON and PathObject representations.
*/

public class PredictionPointConverter {

    public static PredictionAnnotationPoint fromJson(JSONObject jsonObject){
        PredictionAnnotationPoint point = new PredictionAnnotationPoint(
                    jsonObject.getString("dataset"),
                    jsonObject.getString("slide"),
                    jsonObject.getString("tile"),
                    jsonObject.getString("annotator"),
                    jsonObject.getString("t"),
                    jsonObject.getJSONArray("positivity").toList().stream().mapToInt(o -> (int) o).toArray(),
                    jsonObject.getInt("x"),
                    jsonObject.getInt("y"),
                    jsonObject.getJSONArray("prediction").toList().stream().mapToDouble(o -> (double) o).toArray()
            );
        return point;
    }

    public static PathObject toPathObject(PredictionAnnotationPoint point, TileMetadata tileMetadata) {
        double absoluteX = tileMetadata.getPixelX() + point.getX();
        double absoluteY = tileMetadata.getPixelY() + point.getY();
        ROI roi = ROIs.createPointsROI(absoluteX, absoluteY, ImagePlane.getDefaultPlane());    
        PathClass pointClassification = PathClass.getInstance(point.getT());
        PathObject annotation = PathObjects.createAnnotationObject(roi, pointClassification);

        annotation.getMetadata().put("slide", point.getSlide());
        annotation.getMetadata().put("dataset", point.getDataset());
        annotation.getMetadata().put("model", point.getModelName());
        annotation.getMetadata().put("tile", point.getTile());
        annotation.getMetadata().put("type", point.getT());
        annotation.setLocked(false);
        return annotation;
    }

    public static List<PathObject> toPathObjects(List<PredictionAnnotationPoint> points, List<TileMetadata> tileMetadatas) {
        List<PathObject> pathObjects = new ArrayList<>();
        for (PredictionAnnotationPoint point : points) {
            TileMetadata tileMetadata = TileMetadata.findByCode(point.getTile(), tileMetadatas);
            if (tileMetadata == null) {
                ImmuNetLog.error("No tile metadata found for tile code: {}, skipping this annotation", point.getTile());
                continue;
            }
            PathObject pathObject = toPathObject(point, tileMetadata);
            pathObjects.add(pathObject);
        }
        return pathObjects;
    }

    private PredictionPointConverter(){
        /* Should not be instantiated */
    }

}
