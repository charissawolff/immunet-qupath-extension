package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.computational_immunology.ext.ImmuNet.core.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;

import javafx.concurrent.Task;

import org.computational_immunology.ext.ImmuNet.core.AnnotationPointConverter;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class PresentAnnotationsCommand {
    // Annotation-fetching isn't cancelable once started so we set up a timeout per tile 
    private static final long TILE_FETCH_TIMEOUT_SECONDS = 10;

    private final String datasetName;
    private final String slideName;
    private final AnnotationRequestHandler annotationRequestHandler;
    private Task<List<PathObject>> task;
    private Runnable onDone;
    private List<TileMetadata> tilesMetadata;
    private int annotatedTileCount;

    public PresentAnnotationsCommand(String datasetName, String slideName, AnnotationRequestHandler annotationRequestHandler) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.annotationRequestHandler = annotationRequestHandler;
    }

    public void setTilesMetadata(List<TileMetadata> tilesMetadata) {
        this.tilesMetadata = tilesMetadata;
    }

    public void build() {
        task = new Task<>() {
            @Override
            protected List<PathObject> call() {
                updateMessage("Fetching annotations...");
                return fetchSlideAnnotations();
            }
        };
    }

    public void start() {
        task.setOnSucceeded(event -> {
            List<PathObject> pathObjects = task.getValue();
            QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
            if (viewer != null && viewer.getImageData() != null) {
                PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
                hierarchy.addObjects(pathObjects);
                ImmuNetLog.log("Added " + pathObjects.size() + " server annotation(s) for {}/{}", datasetName, slideName);
            }
            if (onDone != null) {
                onDone.run();
            }
        });

        task.setOnFailed(event ->
                ImmuNetLog.error("Could not present annotations for " + datasetName + "/" + slideName, task.getException()));

        Thread thread = new Thread(task, "present-annotations-" + datasetName + "-" + slideName);
        thread.setDaemon(true);
        thread.start();
    }

    public Task<List<PathObject>> getTask() {
        return task;
    }

    public void setOnDone(Runnable callback) {
        this.onDone = callback;
    }

    public int getAnnotatedTileCount() {
        return annotatedTileCount;
    }

    public List<PathObject> fetchSlideAnnotations() {
        if (tilesMetadata == null) {
            ImmuNetLog.error("fetchSlideAnnotations called without tile metadata set. You need to call setTilesMetadata first for dataset: "
                    + datasetName + ", slide: " + slideName);
            return new ArrayList<>();
        }
        try {
            List<String> tileCodes = annotationRequestHandler.fetchSlideAnnotations(datasetName, slideName);
            Map<String, TileMetadata> tileMetadataByCode = new HashMap<>();
            for (TileMetadata tileMetadata : tilesMetadata) {
                tileMetadataByCode.put( tileMetadata.getCode(), tileMetadata);
            }

            List<PathObject> annotationPathObjects = new ArrayList<>();
            ExecutorService fetchExecutor = Executors.newFixedThreadPool(64);
            List<Future<List<PathObject>>> futureList = new ArrayList<>();
            int matchedTileCount = 0;
            for (String tileCode : tileCodes) {
                TileMetadata tileMetadata = tileMetadataByCode.get(tileCode);
                if (tileMetadata == null) {
                    ImmuNetLog.error("No tile metadata found for tile code: {} skipping its annotations", tileCode);
                    continue;
                }
                matchedTileCount++;
                Future<List<PathObject>> future = fetchExecutor.submit(() -> fetchTileAnnotations(tileCode, tileMetadata));
                futureList.add(future);
            }
            annotatedTileCount = matchedTileCount;
            for (Future<List<PathObject>> future : futureList) {
                try {
                    annotationPathObjects.addAll(future.get(TILE_FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (TimeoutException te) {
                    ImmuNetLog.error("Timed out after {} seconds waiting for a tile's annotations so we are skipping it", TILE_FETCH_TIMEOUT_SECONDS);
                    future.cancel(true);
                    annotatedTileCount--;
                }
            }
            fetchExecutor.shutdown();
            return annotationPathObjects;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName, e);
            return new ArrayList<>();
        }
    }

    public List<PathObject> fetchTileAnnotations(String tileCode, TileMetadata tileMetadata) {
        try{
            List<AnnotationPoint> annotations = annotationRequestHandler.fetchAnnotations(datasetName, slideName, tileCode);
            List<PathObject> annotationPathObjects = AnnotationPointConverter.toPathObjects(annotations, tileMetadata);
            return annotationPathObjects;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName + ", tile: " + tileCode, e);
            return new ArrayList<>();
        }
    }

}
