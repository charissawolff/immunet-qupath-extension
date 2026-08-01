package org.computational_immunology.ext.ImmuNet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public String getDatasetName() {
        return datasetName;
    }

    public String getSlideName() {
        return slideName;
    }

    public List<TileMetadata> getTileMetadataList() {
        return tileMetadataList;
    }
}
