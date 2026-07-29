package org.computational_immunology.ext.ImmuNet.core;

public record TileMetadata(int id, String code, ImageType type, double x, double y, double w, double h) {

    public enum ImageType {
        THUMB, COMPOSITE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public TileMetadata {
        if (id < 0) {
            throw new IllegalArgumentException("id cannot be negative");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("width and height cannot be negative or zero");
        }
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
    }

    public String getCode() {
        return code;
    }

    public ImageType getType() {
        return type;
    }

    public int getId() {
        return id;
    }
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public double getWidth() {
        return w;
    }
    public double getHeight() {
        return h;
    }

    public TileMetadata withType(ImageType newType) {
        return new TileMetadata(id, code, newType, x, y, w, h);
    }

}
