package org.computational_immunology.ext.ImmuNet.core;

import java.util.List;

import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class PolygonConverter {

    public static PathObject toPathObject(Polygon p) {
        List<Point2> points = p.getVertices().stream()
            .map(v -> new Point2(v.getX(), v.getY()))
            .toList();
        ROI roi = ROIs.createPolygonROI(points, ImagePlane.getDefaultPlane());

        
        PathClass polygonClass = PathClass.getInstance(p.getId());
        PathObject polygon = PathObjects.createAnnotationObject(roi, polygonClass);
        polygon.setName(p.getName());
        polygon.getMetadata().put("id", p.getId());
        polygon.getMetadata().put("name", p.getName());
        polygon.getMetadata().put("dataset", p.getDataset());
        polygon.getMetadata().put("slide", p.getSlide());
        polygon.getMetadata().put("created", p.getCreated());

        polygon.setLocked(true); // Lock the polygon to prevent accidental modifications
        return polygon;
    }

    private PolygonConverter() {
        /* This utility class should not be instantiated */
    }
}