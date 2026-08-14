package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
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
import org.locationtech.jts.geom.util.AffineTransformation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.GeometryROI;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.PathObjectTools;

public class PolygonConverter {

    public static PathObject toPathObject(AnnotationPolygon p, double dx, double dy) {
        List<PathObject> pathObjects = new ArrayList<>();
        // make json string from coords + type of annotation polygon
        JsonObject element = new JsonObject();
        element.addProperty("type", p.getType());
        element.add("coordinates", JsonParser.parseString(p.getCoordinates().toString()));

        try{
            pathObjects = GsonTools.parseObjectsFromGeoJSON(element);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GeoJSON: " + e.getMessage(), e);
        }
        PathObject polygonPathObject = pathObjects.get(0); // Assuming only one polygon is present

        // Now we need to scale the coordinates by dx and dy
        PathObject scaledPathObject = PathObjectTools.transformObject(polygonPathObject, AffineTransform.getScaleInstance(1/dx, 1/dy), true, false);    
        scaledPathObject.getMetadata().put("id", p.getId());
        scaledPathObject.getMetadata().put("name", p.getName());
        scaledPathObject.getMetadata().put("dataset", p.getDataset());
        scaledPathObject.getMetadata().put("slide", p.getSlide());
        scaledPathObject.getMetadata().put("created", p.getCreated());
        return scaledPathObject;
    }

    private PolygonConverter() {
        /* This utility class should not be instantiated */
    }

    public static AnnotationPolygon fromPathObject(PathObject pathObject, double dx, double dy) {
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

        List<Vertex> outerRing = toVertices(jtsPolygon.getExteriorRing(), dx, dy);

        List<List<Vertex>> holes = new ArrayList<>();
        for (int i = 0; i < jtsPolygon.getNumInteriorRing(); i++) {
            holes.add(toVertices(jtsPolygon.getInteriorRingN(i), dx, dy));
        }

        return new AnnotationPolygon(id, outerRing, holes, name, dataset, slide, created);
    }

    /*
    For saving the AnnotationPolygon to the server, we need to convert it to a JSONObject. 
    The coordinates are stored as a JSONArray of vertices, 
    which can be either flat (no holes) or nested (with holes). 
    The outer ring is always the first element in the JSONArray, and any holes follow as subsequent elements.
    */
    public static JSONObject toJSONObject(AnnotationPolygon polygon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", polygon.getId());
        jsonObject.put("name", polygon.getName());
        jsonObject.put("dataset", polygon.getDataset());
        jsonObject.put("slide", polygon.getSlide());
        jsonObject.put("created", polygon.getCreated());
        jsonObject.put("type", polygon.getType()); //Polygon, multiPolygon, etc.
        jsonObject.put("coordinates", polygon.getCoordinates()); //can be any shape, flat or nested, but prefer the actual gson 3 nested one
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

            // get the raw array, trying the future key first, falling back to today's key
            JSONArray rawCoordinates = jsonPolygon.has("coordinates")
                    ? jsonPolygon.getJSONArray("coordinates")
                    : jsonPolygon.getJSONArray("vertices");

            String shape = outerShape(rawCoordinates);
            JSONArray coordinates;
            if (shape.equals("flat")) {
                // wrap in an extra array so a single ring is valid GeoJSON Polygon coordinates
                coordinates = new JSONArray();
                coordinates.put(rawCoordinates);
            } else {
                coordinates = rawCoordinates;
            }
            closeRingsIfNeeded(coordinates);

            String type = jsonPolygon.optString("type", "Polygon");
            if (!Arrays.asList("Polygon", "MultiPolygon").contains(type)) {
                type = "Polygon";
            }
            AnnotationPolygon polygon = new AnnotationPolygon(
                jsonPolygon.optString("id", jsonPolygon.optString("_id", null)),
                coordinates,
                type,
                jsonPolygon.optString("name", null),
                jsonPolygon.optString("dataset", null),
                jsonPolygon.optString("slide", null),
                jsonPolygon.optString("created", null)
            );
            polygons.add(polygon);
        }
        return polygons;
    }

    // JTS's LinearRing throws if a ring's first and last coordinate don't match. 
    private static void closeRingsIfNeeded(JSONArray coordinates) {
        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray ring = coordinates.getJSONArray(i);
            if (ring.length() == 0) continue;
            JSONArray first = ring.getJSONArray(0);
            JSONArray last = ring.getJSONArray(ring.length() - 1);
            if (first.getDouble(0) != last.getDouble(0) || first.getDouble(1) != last.getDouble(1)) {
                ring.put(first);
            }
        }
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
