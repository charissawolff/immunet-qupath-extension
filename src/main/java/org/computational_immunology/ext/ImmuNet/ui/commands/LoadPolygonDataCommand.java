package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Polygon;
import org.computational_immunology.ext.ImmuNet.core.PolygonConverter;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;

import javafx.concurrent.Task;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class LoadPolygonDataCommand {
    private final SelectedDataStore selectedDataStore;
    private final AnnotationRequestHandler annotationRequestHandler;
    private Runnable onDone;
    private Runnable onFailed;
    private Task<List<Polygon>> task;

    public LoadPolygonDataCommand(AnnotationRequestHandler annotationRequestHandler, SelectedDataStore selectedDataStore) {
        this.selectedDataStore = selectedDataStore;
        this.annotationRequestHandler = annotationRequestHandler;
    }

    public void build() {
        task = new Task<>() {
            @Override
            protected List<Polygon> call() {
                return fetchSlidePolygons();
            }
        };
    }
    public void start() {
        task.setOnSucceeded(event -> {
            List<Polygon> polygons = task.getValue();
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
            if (onDone != null) {
                onDone.run();
            }
        });
        task.setOnFailed(event -> {
            ImmuNetLog.error("Could not fetch polygon data", task.getException());
            if (onFailed != null) onFailed.run(); 
        });

        Thread thread = new Thread(task, "fetch-polygons-" + selectedDataStore.getSelectedSlide().getDatasetName() + "-" + selectedDataStore.getSelectedSlide().getSlideName());
        thread.setDaemon(true);
        thread.start();
    }

    public Task<List<Polygon>> getTask() {
        return task;
    }

    public void setOnDone(Runnable callback) {
        this.onDone = callback;
    }

    public void setOnFailed(Runnable callback) {
        this.onFailed = callback;
    }



    public List<Polygon> fetchSlidePolygons() {
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
            ImmuNetLog.log("Failed to load polygon data for dataset: " + datasetName + ", slide: " + slideName, e);
            throw new RuntimeException("Failed to load polygon data for dataset: " + datasetName + ", slide: " + slideName, e);
        }

    }

    
}
