package org.computational_immunology.ext.ImmuNet.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import qupath.lib.common.ColorTools;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class AnnotationPointConverter {

    // a categorical palette so different "t" values are easy to tell apart at a glance,
    // rather than relying on PathClass's default hash-derived color (which isn't guaranteed to be distinguishable).
    private static final int[] TYPE_COLOR_PALETTE = {
            ColorTools.packRGB(230, 25, 75),   // red
            ColorTools.packRGB(60, 180, 75),   // green
            ColorTools.packRGB(0, 130, 200),   // blue
            ColorTools.packRGB(245, 130, 48),  // orange
            ColorTools.packRGB(145, 30, 180),  // purple
            ColorTools.packRGB(70, 240, 240),  // cyan
            ColorTools.packRGB(240, 50, 230),  // magenta
            ColorTools.packRGB(170, 110, 40),  // brown
    };

    private AnnotationPointConverter(){
        /* This utility class should not be instantiated */
    }

    private static int colorForType(String type) {
        int index = Math.floorMod(type == null ? 0 : type.hashCode(), TYPE_COLOR_PALETTE.length);
        return TYPE_COLOR_PALETTE[index];
    }

    public static PathObject toPathObject(AnnotationPoint point, TileMetadata tileMetadata, double downsampleComposite) {
        double absoluteX = tileMetadata.getX() + point.getX() * downsampleComposite;
        double absoluteY = tileMetadata.getY() + point.getY() * downsampleComposite;
        ROI roi = ROIs.createPointsROI(absoluteX, absoluteY, ImagePlane.getDefaultPlane());

        PathClass pointClassification = PathClass.getInstance(point.getT(), colorForType(point.getT()));
        PathObject annotation = PathObjects.createAnnotationObject(roi, pointClassification);

        annotation.getMetadata().put("id", point.getId());
        annotation.getMetadata().put("slide", point.getSlide());
        annotation.getMetadata().put("dataset", point.getDataset());
        annotation.getMetadata().put("annotator", point.getAnnotator());
        annotation.getMetadata().put("created", point.getCreated());
        annotation.getMetadata().put("tile", point.getTile());
        annotation.getMetadata().put("type", point.getT());
        annotation.getMetadata().put("purpose", point.getPurpose());

        annotation.setLocked(true);
        return annotation;
    }

    public static List<AnnotationPoint> fromPathObjects(List<PathObject> pathObjects,
                                                        List<TileMetadata> tileMetadatas,
                                                        double downsampleComposite) {
        List<AnnotationPoint> points = new ArrayList<>();

        for (PathObject pathObject : pathObjects) {
            ROI roi = pathObject.getROI();
            if (roi == null || !roi.isPoint()) {
                continue;
            }

            Map<String, String> metadata = pathObject.getMetadata();
            String tileId = metadata.get("tile");
            TileMetadata tileMetadata = findTileMetadata(tileId, tileMetadatas);
            if (tileMetadata == null) {
                ImmuNetLog.error("No tile metadata found for tile code: {}, skipping this annotation", tileId);
                continue;
            }

            double absoluteX = roi.getCentroidX();
            double absoluteY = roi.getCentroidY();

            double localX = Math.round((absoluteX - tileMetadata.getX()) / downsampleComposite);
            double localY = Math.round((absoluteY - tileMetadata.getY()) / downsampleComposite);

            points.add(new AnnotationPoint(
                    metadata.get("id"),
                    metadata.get("slide"),
                    metadata.get("dataset"),
                    tileId,
                    localX,
                    localY,
                    metadata.get("type"),
                    metadata.get("annotator"),
                    metadata.get("purpose"),
                    metadata.get("created")
            ));
        }

        return points;
    }

    public static List<PathObject> toPathObjects(List<AnnotationPoint> points, List<TileMetadata> tileMetadatas, double downsampleComposite) {
        List<PathObject> pathObjects = new ArrayList<>();
        for (AnnotationPoint point : points) {
            TileMetadata tileMetadata = findTileMetadata(point.getTile(), tileMetadatas);
            if (tileMetadata == null) {
                ImmuNetLog.error("No tile metadata found for tile code: {}, skipping this annotation", point.getTile());
                continue;
            }
            pathObjects.add(toPathObject(point, tileMetadata, downsampleComposite));
        }
        return pathObjects;
    }

    private static TileMetadata findTileMetadata(String tileCode, List<TileMetadata> tileMetadatas) {
        for (TileMetadata tileMetadata : tileMetadatas) {
            if (tileMetadata.getCode().equals(tileCode)) {
                return tileMetadata;
            }
        }
        return null;
    }

    
}
