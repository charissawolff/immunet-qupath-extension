package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;

import org.checkerframework.checker.units.qual.h;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPolygon.Vertex;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
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

    public static PathObject toPathObject(AnnotationPolygon p) {
        //instead of being the polygon class, we need it to use geometryToROI do it can hold holes

        
        List<AnnotationPolygon.Vertex> outerRing = p.getOuterRing();
        List<List<AnnotationPolygon.Vertex>> holes = p.getHoles();

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

        for (List<AnnotationPolygon.Vertex> hole : holes) {
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

    public static AnnotationPolygon fromPathObject(PathObject pathObject) {
        String id = (String) pathObject.getMetadata().get("id");
        //read directly from pathObject metadata, since name can be changed by user
        String name = pathObject.getName();
        String dataset = (String) pathObject.getMetadata().get("dataset");
        String slide = (String) pathObject.getMetadata().get("slide");
        String created = (String) pathObject.getMetadata().get("created");

        ROI roi = pathObject.getROI();
        Geometry geometry = roi.getGeometry();

        if (!(geometry instanceof org.locationtech.jts.geom.Polygon jtsPolygon)) {
            throw new IllegalArgumentException("ROI geometry is not a single Polygon: " + geometry.getGeometryType());
        }

        List<Vertex> outerRing = toVertices(jtsPolygon.getExteriorRing());

        List<List<Vertex>> holes = new ArrayList<>();
        for (int i = 0; i < jtsPolygon.getNumInteriorRing(); i++) {
            holes.add(toVertices(jtsPolygon.getInteriorRingN(i)));
        }

        return new AnnotationPolygon(id, outerRing, holes, name, dataset, slide, created);
    }

    private static List<Vertex> toVertices(LineString ring) {
        return Arrays.stream(ring.getCoordinates())
            .map(c -> new Vertex(c.x, c.y))
            .toList();
    }

    public static JSONObject toJSONObject(AnnotationPolygon polygon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", polygon.getId());
        jsonObject.put("name", polygon.getName());
        jsonObject.put("dataset", polygon.getDataset());
        jsonObject.put("slide", polygon.getSlide());
        jsonObject.put("created", polygon.getCreated());

        JSONArray verticesArray = new JSONArray();
        JSONArray outerRingArray = new JSONArray();
        for (Vertex vertex : polygon.getOuterRing()) {
            outerRingArray.put(new JSONArray().put(vertex.getX()).put(vertex.getY()));
        }
        if (polygon.getHoles().isEmpty()) {
            // No holes: keep the flat shape [[x,y], [x,y], ...] for backward compatibility
            // with the Vue frontend, which can not paint nested vertices.
            verticesArray = outerRingArray;
        } else {
            // Holes present: shape is [[outerRing], [hole1], [hole2], ...]. Will not show on the vue frontend.
            verticesArray.put(outerRingArray);
            for (List<Vertex> hole : polygon.getHoles()) {
                JSONArray holeArray = new JSONArray();
                for (Vertex vertex : hole) {
                    holeArray.put(new JSONArray().put(vertex.getX()).put(vertex.getY()));
                }
                verticesArray.put(holeArray);
            }
        }
        jsonObject.put("vertices", verticesArray);

        return jsonObject;
    }

    public static JSONArray toJSONArray(Collection<AnnotationPolygon> polygons) {
        JSONArray array = new JSONArray();
        for (AnnotationPolygon polygon : polygons) {
            array.put(toJSONObject(polygon));
        }
        return array;
    }

    public static List<AnnotationPolygon> fromJsonArray(JSONArray jsonArray) {
        List<AnnotationPolygon> polygons = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonPolygon = jsonArray.getJSONObject(i);
            JSONArray jsonVertices = jsonPolygon.getJSONArray("vertices");
            // it is in the shape [[[outer ring], [hole1], [hole2]]]
            // OR [[vertex1], [vertex2], [vertex3]] if there are no holes
            //first check shape of json vertices
            String shape = outerShape(jsonVertices);
            switch (shape) {
                case "flat":
                   List<AnnotationPolygon.Vertex> vertices = parseVertices(jsonVertices);
                    AnnotationPolygon polygon = new AnnotationPolygon(
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
                    List<AnnotationPolygon.Vertex> outerRing = parseVertices(jsonVertices.getJSONArray(0));
                    List<List<AnnotationPolygon.Vertex>> holes = parseHoles(jsonVertices, 1);
                    AnnotationPolygon polygon2 = new AnnotationPolygon(
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

    private static List<AnnotationPolygon.Vertex> parseVertices(JSONArray jsonArray){
        List<AnnotationPolygon.Vertex> vertices = new ArrayList<>(jsonArray.length());
            for (int j = 0; j < jsonArray.length(); j++) {
                JSONArray point = jsonArray.getJSONArray(j); // e.g. [11255.70, 3696.45]
                double x = point.getDouble(0);
                double y = point.getDouble(1);
                vertices.add(new AnnotationPolygon.Vertex(x, y));
            }
        return vertices;
    }

    private static List<List<AnnotationPolygon.Vertex>> parseHoles(JSONArray jsonArray, int startIndex) {
        List<List<AnnotationPolygon.Vertex>> holes = new ArrayList<>(jsonArray.length() - startIndex);
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
}
