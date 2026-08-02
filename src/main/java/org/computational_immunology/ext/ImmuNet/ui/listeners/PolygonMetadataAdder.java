package org.computational_immunology.ext.ImmuNet.ui.listeners;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.SelectedSlide;

import javafx.collections.ListChangeListener;
import qupath.lib.objects.PathObject;

/*
Keeps track of new polyogns added by user, automatically adds the slidename and dataset name to the data,
so that user can upload it to the server.
*/
public class PolygonMetadataAdder {

    public PolygonMetadataAdder(PolygonTracker tracker, SelectedDataStore selectedDataStore) {
        tracker.getNewAnnotations().addListener((ListChangeListener<PathObject>) change -> {
            while (change.next()) {
                if (!change.wasAdded()) continue;
                for (PathObject added : change.getAddedSubList()) {
                    // keep track of it per polygon so that we don't have it out of sync with the selected slide
                    SelectedSlide selectedSlide = selectedDataStore.getSelectedSlide();
                    if (selectedSlide == null) {
                        ImmuNetLog.error("Selected slide is null, cannot add metadata to new polygon: " + added);
                        continue;
                    }
                    added.getMetadata().put("dataset", selectedSlide.getDatasetName());
                    added.getMetadata().put("slide", selectedSlide.getSlideName());
                    added.getMetadata().put("created", String.valueOf(System.currentTimeMillis()));
                }
            }
        });
    }
}
    
