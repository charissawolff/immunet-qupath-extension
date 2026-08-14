package org.computational_immunology.ext.ImmuNet.core.store;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.SelectedSlide;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPolygon;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class SelectedDataStore {
    private final ReadOnlyObjectWrapper<SelectedSlide> selectedSlide = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<TileMetadata> selectedTile = new ReadOnlyObjectWrapper<>(); //only the tile meatdata as the tile image is not needed here and can be retrieved from the metadata
    private final ReadOnlyObjectWrapper<Double> downSampleComposite = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<Double> dx = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<Double> dy = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<List<AnnotationPolygon>> Polygons = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<List<AnnotationPoint>> annotationPoints = new ReadOnlyObjectWrapper<>();
    public SelectedSlide getSelectedSlide() {
        return selectedSlide.get();
    }

    public void setSelectedSlide(SelectedSlide slide) {
        // When a new slide is selected, reset the selected tile to null
        selectedSlide.set(slide);
        selectedTile.set(null);
    }

    public void clear() {
        selectedSlide.set(null);
        selectedTile.set(null);
        downSampleComposite.set(null);
        dx.set(null);
        dy.set(null);
        Polygons.set(null);
        annotationPoints.set(null);
    }

    public TileMetadata getSelectedTile() {
        return selectedTile.get();
    }

    public void setSelectedTile(TileMetadata tile) {
        selectedTile.set(tile);
    }

    public void setDownSampleComposite(double downsampleComposite) {
        this.downSampleComposite.set(downsampleComposite);
    }
    public double getDownSampleComposite() {
        return downSampleComposite.get();
    }

    public void setDx(double value) {
        this.dx.set(value);
    }
    public double getDx() {
        return dx.get();
    }

    public void setDy(double value) {
        this.dy.set(value);
    }
    public double getDy() {
        return dy.get();
    }


    public List<AnnotationPolygon> getPolygons() {
        return Polygons.get();
    }

    public void setPolygons(List<AnnotationPolygon> polygons) {
        Polygons.set(polygons);
    }

    public void setAnnotationPoints(List<AnnotationPoint> annotationPoints) {
        this.annotationPoints.set(annotationPoints);
    }

    public List<AnnotationPoint> getAnnotationPoints() {
        return annotationPoints.get();
    }

    public ReadOnlyObjectProperty<SelectedSlide> selectedSlideProperty() {
        return selectedSlide.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<TileMetadata> selectedTileProperty() {
        return selectedTile.getReadOnlyProperty();
    }
}
