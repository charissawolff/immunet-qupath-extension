package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;

import org.checkerframework.checker.units.qual.h;
import org.computational_immunology.ext.ImmuNet.core.models.Polygon.Vertex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.GeometryROI;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class PolygonConverter {

    public static PathObject toPathObject(Polygon p) {
        //instead of being the polygon class, we need it to use geometryToROI do it can hold holes

        
        List<Polygon.Vertex> outerRing = p.getOuterRing();
        List<List<Polygon.Vertex>> holes = p.getHoles();

        GeometryFactory gf = new GeometryFactory();
        List<Coordinate> outerRingCoords = outerRing.stream()
                .map(v -> new Coordinate(v.getX(), v.getY()))
                .collect(Collectors.toList());

        if (!outerRingCoords.get(0).equals2D(outerRingCoords.get(outerRingCoords.size() - 1))) {
            outerRingCoords.add(outerRingCoords.get(0)); // close the ring
        }

        Coordinate[] outerRingCoordsArray = outerRingCoords.toArray(new Coordinate[0]);
        LinearRing outerLinearRing = gf.createLinearRing(outerRingCoordsArray);

        List<LinearRing> holeLinearRings = new ArrayList<>();

        for (List<Polygon.Vertex> hole : holes) {
            List<Coordinate> holeCoords = hole.stream()
                .map(v -> new Coordinate(v.getX(), v.getY()))
                .collect(Collectors.toList());

        if (!holeCoords.get(0).equals2D(holeCoords.get(holeCoords.size() - 1))) {
            holeCoords.add(holeCoords.get(0)); // close the ring
            }
        Coordinate[] holeCoordsArray = holeCoords.toArray(new Coordinate[0]);
        holeLinearRings.add(gf.createLinearRing(holeCoordsArray));
        }

        LinearRing[] holesArray = holeLinearRings.toArray(new LinearRing[0]);
        org.locationtech.jts.geom.Polygon polygon = gf.createPolygon(outerLinearRing, holesArray);
        polygon.normalize();

        ROI roi = GeometryTools.geometryToROI(polygon, ImagePlane.getDefaultPlane());

        PathClass polygonClass = PathClass.getInstance(p.getId());
        PathObject polygonPathObject = PathObjects.createAnnotationObject(roi, polygonClass);
        polygonPathObject.setName(p.getName());
        polygonPathObject.getMetadata().put("id", p.getId());
        polygonPathObject.getMetadata().put("name", p.getName());
        polygonPathObject.getMetadata().put("dataset", p.getDataset());
        polygonPathObject.getMetadata().put("slide", p.getSlide());
        polygonPathObject.getMetadata().put("created", p.getCreated());

        polygonPathObject.setLocked(true); // Lock the polygon to prevent accidental modifications
        return polygonPathObject;
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
        
        List<Vertex> vertices = roi.getAllPoints();
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
