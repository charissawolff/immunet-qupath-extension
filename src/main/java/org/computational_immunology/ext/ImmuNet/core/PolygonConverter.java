package org.computational_immunology.ext.ImmuNet.core;

import java.util.Collection;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.Polygon.Vertex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

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

    public static Polygon fromPathObject(PathObject pathObject) {
        String id = (String) pathObject.getMetadata().get("id");
        //read directly from pathObject metadata, since name can be changed by user
        String name = pathObject.getName();
        String dataset = (String) pathObject.getMetadata().get("dataset");
        String slide = (String) pathObject.getMetadata().get("slide");
        String created = (String) pathObject.getMetadata().get("created");

        ROI roi = pathObject.getROI();
        List<Point2> vertices = roi.getAllPoints();
        List<Vertex> vertexList = vertices.stream()
            .map(point -> new Vertex(point.getX(), point.getY()))
            .toList();

        return new Polygon(id, vertexList, name, dataset, slide, created);
    }

    public static JSONObject toJSONObject(Polygon polygon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", polygon.getId());
        jsonObject.put("name", polygon.getName());
        jsonObject.put("dataset", polygon.getDataset());
        jsonObject.put("slide", polygon.getSlide());
        jsonObject.put("created", polygon.getCreated());

        JSONArray verticesArray = new JSONArray();
        for (Vertex vertex : polygon.getVertices()) {
            verticesArray.put(new JSONArray().put(vertex.getX()).put(vertex.getY()));
        }
        jsonObject.put("vertices", verticesArray);

        return jsonObject;
    }

    public static JSONArray toJSONArray(Collection<Polygon> polygons) {
        JSONArray array = new JSONArray();
        for (Polygon polygon : polygons) {
            array.put(toJSONObject(polygon));
        }
        return array;
    }
}
