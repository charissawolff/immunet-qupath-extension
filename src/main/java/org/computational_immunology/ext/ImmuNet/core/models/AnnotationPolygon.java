package org.computational_immunology.ext.ImmuNet.core.models;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import com.google.gson.Gson;

public class AnnotationPolygon {

    private final String id;
    private final JSONArray coordinates; //can be any shape 
    private final String type;
    private final String name;
    private final String dataset;
    private final String slide;
    private final String created;

    public AnnotationPolygon(String id, JSONArray coordinates, String type, String name,
                    String dataset, String slide, String created) {
        this.id = id;
        this.coordinates = coordinates;
        this.type = type;
        this.name = name;
        this.dataset = dataset;
        this.slide = slide;
        this.created = created;
    }

        public AnnotationPolygon(String id, JSONArray coordinates, String name,
                    String dataset, String slide, String created) {
        this.id = id;
        this.coordinates = coordinates;
        this.type = "Polygon";
        this.name = name;
        this.dataset = dataset;
        this.slide = slide;
        this.created = created;
    }

    public String getId() { return id; }
    public JSONArray getCoordinates() { return coordinates; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getDataset() { return dataset; }
    public String getSlide() { return slide; }
    public String getCreated() { return created; }
    public String toString() {
        return "Polygon{" +
                "id='" + id + '\'' +
                ", coordinates=" + coordinates +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", dataset='" + dataset + '\'' +
                ", slide='" + slide + '\'' +
                ", created='" + created + '\'' +
                '}';
    }
}