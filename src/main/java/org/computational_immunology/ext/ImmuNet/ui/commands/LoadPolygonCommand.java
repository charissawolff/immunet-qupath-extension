package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Polygon;
import org.computational_immunology.ext.ImmuNet.core.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class LoadPolygonCommand extends AbstractAsyncCommand<List<Polygon>> {
    private final SelectedDataStore selectedDataStore;
    private final AnnotationRequestHandler annotationRequestHandler;

    public LoadPolygonCommand(AnnotationRequestHandler annotationRequestHandler, SelectedDataStore selectedDataStore) {
        this.selectedDataStore = selectedDataStore;
        this.annotationRequestHandler = annotationRequestHandler;
    }

    @Override
    //on success, add the polygons to the selectedDataStore and also add them to the QuPath hierarchy, so they are visible in the viewer
    protected void onSuccess(List<Polygon> polygons) {
        selectedDataStore.setPolygons(polygons);
        List<PathObject> pathObjects = new ArrayList<>();
        for (Polygon p: polygons) {
            pathObjects.add(PolygonConverter.toPathObject(p));
            ImmuNetLog.log("Fetched polygon with ID: " + p.getId() + " for dataset: " + selectedDataStore.getSelectedSlide().getDatasetName() + ", slide: " + selectedDataStore.getSelectedSlide().getSlideName());
        }
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer != null && viewer.getImageData() != null) {
            PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
            hierarchy.addObjects(pathObjects);
            ImmuNetLog.log("Added " + pathObjects.size() + " server polygon(s) for " + selectedDataStore.getSelectedSlide().getDatasetName() + ", slide: " + selectedDataStore.getSelectedSlide().getSlideName());
        }
    }

    @Override
    protected List<Polygon> execute(Consumer<String> progressReporter) throws Exception {
        List<Polygon> polygons = new ArrayList<>();
        String datasetName = selectedDataStore.getSelectedSlide().getDatasetName();
        String slideName = selectedDataStore.getSelectedSlide().getSlideName();
        if (datasetName.length() < 1 || slideName.length() < 1) {
            ImmuNetLog.error("fetchSlidePolygons called without tile metadata set. You need to call setTilesMetadata first for dataset: "
                    + datasetName + ", slide: " + slideName);
            return polygons; // No dataset or slide selected, exit the method
        }
        try{
            List<Polygon> ps = annotationRequestHandler.fetchPolygons(datasetName, slideName);
            for (Polygon p : ps) {
                ImmuNetLog.log("Fetched polygon with ID: " + p.getId() + " for dataset: " + datasetName + ", slide: " + slideName);
                polygons.add(p);
            }
            return polygons;
        }catch (Exception e) {
            throw new RuntimeException("Failed to load polygon data for dataset: " + datasetName + ", slide: " + slideName, e);
        }

    }
    
}
