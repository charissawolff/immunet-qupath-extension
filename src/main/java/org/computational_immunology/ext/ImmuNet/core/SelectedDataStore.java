package org.computational_immunology.ext.ImmuNet.core;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class SelectedDataStore {
    private final ReadOnlyObjectWrapper<SelectedSlide> selectedSlide = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<TileMetadata> selectedTile = new ReadOnlyObjectWrapper<>(); //only the tile meatdata as the tile image is not needed here and can be retrieved from the metadata

    public SelectedSlide getSelectedSlide() {
        return selectedSlide.get();
    }

    public void setSelectedSlide(SelectedSlide slide) {
        // When a new slide is selected, reset the selected tile to null
        selectedSlide.set(slide);
        selectedTile.set(null);
    }

    public TileMetadata getSelectedTile() {
        return selectedTile.get();
    }

    public void setSelectedTile(TileMetadata tile) {
        selectedTile.set(tile);
    }

    public ReadOnlyObjectProperty<SelectedSlide> selectedSlideProperty() {
        return selectedSlide.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<TileMetadata> selectedTileProperty() {
        return selectedTile.getReadOnlyProperty();
    }
}
