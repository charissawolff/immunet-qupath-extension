package org.computational_immunology.ext.ImmuNet.core;

import java.util.List;
import java.util.Objects;

public class Polygon{

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

    /**
     * The text to show for this polygon in the UI
     */
    public String getDisplayedName() { return name; }

    /**
     * A single (x, y) vertex of the polygon.
     */
   public static class Vertex{
        private final double x;
        private final double y;

        public Vertex(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public double getY() { return y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return Double.compare(vertex.x, x) == 0 && Double.compare(vertex.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}