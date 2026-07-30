package org.computational_immunology.ext.ImmuNet.core;

import java.util.List;

import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class AnnotationPointConverter {

    private AnnotationPointConverter(){
        /* This utility class should not be instantiated */
    }

    public static PathObject toPathObject(AnnotationPoint point) {
        ROI roi = ROIs.createPointsROI(point.getX(), point.getY(), ImagePlane.getDefaultPlane());

        PathClass pointClassification = PathClass.getInstance(point.getT());
        PathObject annotation = PathObjects.createAnnotationObject(roi, pointClassification);

        annotation.setName(point.getId());
        annotation.getMetadata().put("annotator", point.getAnnotator());
        annotation.getMetadata().put("created", point.getCreated());
        annotation.getMetadata().put("tile", point.getTile());
        annotation.getMetadata().put("type", point.getT());
        annotation.getMetadata().put("purpose", point.getPurpose());

        annotation.setLocked(true);   
        return annotation;
    }

    public static List<PathObject> toPathObjects(List<AnnotationPoint> points) {
        return points.stream().map(AnnotationPointConverter::toPathObject).toList();
    }
}
