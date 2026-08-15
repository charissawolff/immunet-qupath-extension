package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.geom.AffineTransform;

import org.json.JSONArray;
import org.json.JSONObject;

import org.locationtech.jts.geom.Geometry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.PathObjectTools;

/**
* Helper class to convert JSON representation of polygons to PathObject and vice versa, as well as to/from AnnotationPolygon.
* We specifically convert to GeoJSON-like format for saving to the database. This is because QuPath offers a helper class GsonTools to convert PathObject to/from GeoJSON.
* However, it also support loading polygons in a legacy none GeoJSON format in order to load old polygons that were saved in the database before we switched to GeoJSON-like format.
**/

public class PolygonConverter {


    /**
     * Converts an AnnotationPolygon to a QuPath PathObject.
     * @param p the AnnotationPolygon to convert to a PathObject
     * @param dx The scaling factor in the x direction (width)
     * @param dy The scaling factor in the y direction (height)
     * @return A QuPath PathObject representing our saved polygon in micrometers coordinate in the correct pixel coordinate system.
     */
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
        PathObject polygonPathObject = pathObjects.get(0); // Assuming only one polygon is present (should be true for our use case)

        PathObject scaledPathObject = PathObjectTools.transformObject(polygonPathObject, AffineTransform.getScaleInstance(1/dx, 1/dy), true, false);    
        PathClass polygonClass = PathClass.getInstance(p.getId());
        scaledPathObject.setPathClass(polygonClass); //to have different colors too
        scaledPathObject.setName(p.getName());
        scaledPathObject.getMetadata().put("id", p.getId());
        scaledPathObject.getMetadata().put("name", p.getName());
        scaledPathObject.getMetadata().put("dataset", p.getDataset());
        scaledPathObject.getMetadata().put("slide", p.getSlide());
        scaledPathObject.getMetadata().put("created", p.getCreated());
        scaledPathObject.getMetadata().put("type", p.getType());

        scaledPathObject.setLocked(true); // Lock the polygon to prevent accidental modifications
        return scaledPathObject;
    }

    /**
     * Converts a QuPath PathObject to an AnnotationPolygon.
     * The coordinates of the polygon are scaled by dx and dy to convert from pixel coordinates to micrometer coordinates.
     * @param pathObject the QuPath PathObject to convert to an AnnotationPolygon
     * @param dx The scaling factor in the x direction (width)
     * @param dy The scaling factor in the y direction (height)
     * @return An AnnotationPolygon representing the PathObject in micrometer coordinates.
     */

    public static AnnotationPolygon fromPathObject(PathObject pathObject, double dx, double dy) {
        Geometry geometry = pathObject.getROI().getGeometry();
        if (!(geometry instanceof org.locationtech.jts.geom.Polygon) && !(geometry instanceof org.locationtech.jts.geom.MultiPolygon)) {
            throw new IllegalArgumentException("ROI geometry is not a single Polygon or MultiPolygon: " + geometry.getGeometryType());
        }

        PathObject transformedPathObject = PathObjectTools.transformObject(pathObject, AffineTransform.getScaleInstance(dx, dy), true, false);

        String json = GsonTools.getInstance().toJson(transformedPathObject);
        JsonObject geometryJson = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("geometry");
        JSONArray coordinates = new JSONArray(geometryJson.getAsJsonArray("coordinates").toString());
        String type = geometryJson.get("type").getAsString();

        String id = (String) transformedPathObject.getMetadata().get("id");
        //read directly from transformedPathObject metadata/name, since name can be changed by user when user adds a new polygon
        String name = transformedPathObject.getName();
        String dataset = (String) transformedPathObject.getMetadata().get("dataset");
        String slide = (String) transformedPathObject.getMetadata().get("slide");
        String created = (String) transformedPathObject.getMetadata().get("created");

        return new AnnotationPolygon(id, coordinates, type, name, dataset, slide, created);
    }

    /**
    * Convert a Polygon object to a JSONObject for serialization. 
    * This representation can be read as a GeoJSON-like structure, with the coordinates and type of the polygon included.
    * Used for saving polygons to a database.
    * @param polygon the AnnotationPolygon to convert to a JSONObject
    * @return A JSONObject representing the AnnotationPolygon in a GeoJSON-like format.
    */
    public static JSONObject toJSONObject(AnnotationPolygon polygon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", polygon.getId());
        jsonObject.put("name", polygon.getName());
        jsonObject.put("dataset", polygon.getDataset());
        jsonObject.put("slide", polygon.getSlide());
        jsonObject.put("created", polygon.getCreated());
        jsonObject.put("type", polygon.getType()); //Polygon, multiPolygon, etc.
        jsonObject.put("coordinates", polygon.getCoordinates()); //can be any shape, flat or nested, but prefer the actual gson nested one
        return jsonObject;
    }

    /**
     * Convert a JSONArray of JSON representation of polygons to a list of AnnotationPolygon objects.
     * This method iterates through the JSONArray, extracting each JSONObject and converting it to an AnnotationPolygon using the fromJson method.
     * This is useful for loading polygons from a database. Supports GeoJSON-like format, with the coordinates and type of the polygon included.
     * But also none GeoJSON-like legacy format, with "coordinates" being "vertices", and no type included (defaults to "Polygon").
     * The method also ensures that the coordinates are in the correct format, closing rings if necessary
     * @param jsonArray A JSONArray containing the JSON representation of polygons. Each element in the array should be a JSONObject representing a single polygon.
     * @return A list of AnnotationPolygon objects constructed from the JSON representation.
     */
    public static List<AnnotationPolygon> fromJsonArray(JSONArray jsonArray) {
        List<AnnotationPolygon> polygons = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonPolygon = jsonArray.getJSONObject(i);

            String type = jsonPolygon.optString("type", "Polygon");
            if (!Arrays.asList("Polygon", "MultiPolygon").contains(type)) {
                type = "Polygon";
            }

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
            closeRingsIfNeeded(coordinates, type);

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

    // Polygon coordinates are [[[ring], [hole], ...]]]. MultiPolygon coordinates are one level deeper: [[[[ring],[hole]...]], [[[ring]...]], ...]]].
    private static void closeRingsIfNeeded(JSONArray coordinates, String type) {
        if ("MultiPolygon".equals(type)) {
            for (int i = 0; i < coordinates.length(); i++) {
                closeRings(coordinates.getJSONArray(i));
            }
        } else {
            closeRings(coordinates);
        }
    }

    // Make sure the ring's first and last coordinates are exactly the same; we get an error if they are not.
    private static void closeRings(JSONArray rings) {
        for (int i = 0; i < rings.length(); i++) {
            JSONArray ring = rings.getJSONArray(i);
            if (ring.length() == 0) continue;
            JSONArray first = ring.getJSONArray(0);
            JSONArray last = ring.getJSONArray(ring.length() - 1);
            if (first.getDouble(0) != last.getDouble(0) || first.getDouble(1) != last.getDouble(1)) {
                ring.put(first);
            }
        }
    }

    // Get the shape of the coordinates JSONArray. 
    // Returns "flat" if the coordinates are a single ring (one level of arrays), 
    // "nested" if they are multiple rings (two levels of arrays), and "mixed" if they are a mix of both (shouldn't happen).
    private static String outerShape(JSONArray vertices) {
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

    private PolygonConverter() {
        /* This utility class should not be instantiated */
    }
}
