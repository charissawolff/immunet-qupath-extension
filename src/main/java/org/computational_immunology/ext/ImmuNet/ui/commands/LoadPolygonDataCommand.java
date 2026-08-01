package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;

public class LoadPolygonDataCommand {
    private final SelectedDataStore selectedDataStore;

    public LoadPolygonDataCommand(AnnotationRequestHandler annotationRequestHandler, SelectedDataStore selectedDataStore) {
        this.selectedDataStore = selectedDataStore;
        this.annotationRequestHandler = annotationRequestHandler;
    }

    public void loadData() {
    }
    
}
