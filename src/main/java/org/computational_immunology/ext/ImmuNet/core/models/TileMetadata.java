package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.List;

public record TileMetadata(int id, String code, ImageType type, 
                            double x, double y, double w, double h,
                            double dx, double dy) {

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

    public int getPixelX() {
        return (int) Math.round(x / dx);
    }
    public int getPixelY() {
        return (int) Math.round(y / dy);
    }
    public int getPixelWidth() {
        return (int) Math.round((x + w) / dx) - getPixelX();
    }
    public int getPixelHeight() {
        return (int) Math.round((y + h) / dy) - getPixelY();
    }

    public double getDx() {
        return dx;
    }
    public double getDy() {
        return dy;
    }

    public TileMetadata withType(ImageType newType) {
        return new TileMetadata(id, code, newType, x, y, w, h, dx, dy);
    }

    public static TileMetadata findByCode(String code, List<TileMetadata> tiles) {
        for (TileMetadata tile : tiles) {
            if (tile.getCode().equals(code)) {
                return tile;
            }
        }
        return null;
    }

    public boolean overlaps(TileMetadata other) {
        return this.getX() < other.getX() + other.getWidth() &&
            other.getX() < this.getX() + this.getWidth() &&
            this.getY() < other.getY() + other.getHeight() &&
            other.getY() < this.getY() + this.getHeight();
    }

    public TileMetadata cropToNonOverlapping(double newX, double newY, double newW, double newH) {
        return new TileMetadata(this.id, this.code, this.type, newX, newY, newW, newH, this.dx, this.dy);
    }
}
