package org.computational_immunology.ext.ImmuNet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;

/**
 * Record of what slide the user has open in the viewer. 
 * @param datasetName
 * @param slideName
 * @param tileMetadataList
 */
public record SelectedSlide(String datasetName, String slideName, List<TileMetadata> tileMetadataList) {

    public SelectedSlide {
        if (datasetName == null || datasetName.isEmpty()) {
            throw new IllegalArgumentException("datasetName cannot be null or empty");
        }
        if (slideName == null || slideName.isEmpty()) {
            throw new IllegalArgumentException("slideName cannot be null or empty");
        }
        if (tileMetadataList == null) {
            throw new IllegalArgumentException("tileMetadataList cannot be null");
        }
        tileMetadataList = Collections.unmodifiableList(new ArrayList<>(tileMetadataList));
    }

    public boolean hasTileMetadata() {
        return !tileMetadataList.isEmpty();
    }

    public Optional<TileMetadata> tileAt(double imageX, double imageY) {
        for (TileMetadata tile : tileMetadataList) {
            if (imageX >= tile.getPixelX() && imageX < tile.getPixelX() + tile.getPixelWidth()
                    && imageY >= tile.getPixelY() && imageY < tile.getPixelY() + tile.getPixelHeight()) {
                return Optional.of(tile);
            }
        }
        return Optional.empty();
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getSlideName() {
        return slideName;
    }

    public List<TileMetadata> getTileMetadataList() {
        return tileMetadataList;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SelectedSlide that)) return false;

        if (!datasetName.equals(that.datasetName)) return false;
        return slideName.equals(that.slideName);
    }
}
