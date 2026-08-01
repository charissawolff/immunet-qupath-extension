package org.computational_immunology.ext.ImmuNet.core;

import java.util.List;
import java.util.Objects;

public class Polygon {

    private final String id;
    private final List<Vertex> vertices;
    private final String name;
    private final String dataset;
    private final String slide;
    private final String created;

    public Polygon(String id, List<Vertex> vertices, String name,
                    String dataset, String slide, String created) {
        this.id = id;
        this.vertices = vertices;
        this.name = name;
        this.dataset = dataset;
        this.slide = slide;
        this.created = created;
    }

    public String getId() { return id; }
    public List<Vertex> getVertices() { return vertices; }
    public String getName() { return name; }
    public String getDataset() { return dataset; }
    public String getSlide() { return slide; }
    public String getCreated() { return created; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Polygon that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(vertices, that.vertices)
                && Objects.equals(name, that.name)
                && Objects.equals(dataset, that.dataset)
                && Objects.equals(slide, that.slide)
                && Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vertices, name, dataset, slide, created);
    }

    @Override
    public String toString() {
        return "Polygon{id='" + id + "', name='" + name + "', dataset='" + dataset +
                "', slide='" + slide + "', created='" + created +
                "', vertexCount=" + (vertices != null ? vertices.size() : 0) + "}";
    }

    /**
     * A single (x, y) vertex of the polygon.
     */
    public record Vertex(double x, double y) {
    }
}