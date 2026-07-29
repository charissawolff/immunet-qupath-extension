package org.computational_immunology.ext.ImmuNet.core;

import java.util.Objects;

public class Annotation {

    private final String id;
    private final String slide;
    private final String dataset;
    private final String tile;
    private final int x;
    private final int y;
    private final String t;
    private final String annotator;
    private final String purpose;
    private final String created;

    public Annotation(String id, String slide, String dataset, String tile,
                       int x, int y, String t, String annotator,
                       String purpose, String created) {
        this.id = id;
        this.slide = slide;
        this.dataset = dataset;
        this.tile = tile;
        this.x = x;
        this.y = y;
        this.t = t;
        this.annotator = annotator;
        this.purpose = purpose;
        this.created = created;
    }


    public String getId() { return id; }
    public String getSlide() { return slide; }
    public String getDataset() { return dataset; }
    public String getTile() { return tile; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getT() { return t; }
    public String getAnnotator() { return annotator; }
    public String getPurpose() { return purpose; }
    public String getCreated() { return created; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Annotation that)) return false;
        return x == that.x
                && y == that.y
                && Objects.equals(id, that.id)
                && Objects.equals(slide, that.slide)
                && Objects.equals(dataset, that.dataset)
                && Objects.equals(tile, that.tile)
                && Objects.equals(t, that.t)
                && Objects.equals(annotator, that.annotator)
                && Objects.equals(purpose, that.purpose)
                && Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, slide, dataset, tile, x, y, t, annotator, purpose, created);
    }

    @Override
    public String toString() {
        return "Annotation{id='" + id + "', slide='" + slide + "', dataset='" + dataset +
                "', tile='" + tile + "', x=" + x + ", y=" + y + ", t='" + t +
                "', annotator='" + annotator + "', purpose='" + purpose +
                "', created='" + created + "'}";
    }
}