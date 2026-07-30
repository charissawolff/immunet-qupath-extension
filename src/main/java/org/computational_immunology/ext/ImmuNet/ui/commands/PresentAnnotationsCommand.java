package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.computational_immunology.ext.ImmuNet.core.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import javafx.concurrent.Task;

import org.computational_immunology.ext.ImmuNet.core.AnnotationPointConverter;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class PresentAnnotationsCommand {
    // Annotation-fetching isn't cancelable once started (see SlideLoadWorkflow)  this bounds
    // here we set how long a single slow tile can hold up the rest of the batch instead.
    private static final long TILE_FETCH_TIMEOUT_SECONDS = 10;

    private final String datasetName;
    private final String slideName;
    private final AnnotationRequestHandler annotationRequestHandler;
    private final ImageRequestHandler imageRequestHandler;
    private Task<List<PathObject>> task;
    private Runnable onDone;

    // The total tile count for the slide alongside the fetched annotations, since that
    // can't be derived from the annotation list itself (it only reflects tiles that had annotations).
    public record SlideAnnotationsResult(List<PathObject> pathObjects, int totalTileCount) {}

    public PresentAnnotationsCommand(String datasetName, String slideName, AnnotationRequestHandler annotationRequestHandler, ImageRequestHandler imageRequestHandler) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.annotationRequestHandler = annotationRequestHandler;
        this.imageRequestHandler = imageRequestHandler;
    }

    public void build() {
        // only build on slide level, since then we make use of the parallelization of fetching tile annotations as a Task
        // else just do a single fetch for the tile annotations without parallelization by calling fetchTileAnnotations directly
        task = new Task<>() {
            @Override
            protected List<PathObject> call() {
                updateMessage("Fetching annotations...");
                SlideAnnotationsResult result = fetchSlideAnnotations();
                long annotatedTileCount = result.pathObjects().stream()
                        .map(pathObject -> pathObject.getMetadata().get("tile"))
                        .distinct()
                        .count();
                updateMessage("Fetched " + result.pathObjects().size() + " annotations in " + annotatedTileCount
                        + " tiles. There are a total of " + result.totalTileCount() + " tiles.");
                return result.pathObjects();
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

    public SlideAnnotationsResult fetchSlideAnnotations() {
        try {
            List<String> tileCodes = annotationRequestHandler.fetchSlideAnnotations(datasetName, slideName);
            List<TileMetadata> allTiles = imageRequestHandler.getAllTileMetadatas(datasetName, slideName);
            Map<String, TileMetadata> tileMetadataByCode = allTiles.stream()
                    .collect(Collectors.toMap(TileMetadata::getCode, tile -> tile));

            List<PathObject> annotationPathObjects = new ArrayList<>();
            ExecutorService fetchExecutor = Executors.newFixedThreadPool(64);
            List<Future<List<PathObject>>> futureList = new ArrayList<>();
            for (String tileCode : tileCodes) {
                TileMetadata tileMetadata = tileMetadataByCode.get(tileCode);
                if (tileMetadata == null) {
                    ImmuNetLog.error("No tile metadata found for tile code: {} skipping its annotations", tileCode);
                    continue;
                }
                Future<List<PathObject>> future = fetchExecutor.submit(() -> fetchTileAnnotations(tileCode, tileMetadata));
                futureList.add(future);
            }
            for (Future<List<PathObject>> future : futureList) {
                try {
                    annotationPathObjects.addAll(future.get(TILE_FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (TimeoutException te) {
                    ImmuNetLog.error("Timed out after {} seconds waiting for a tile's annotations so we are skipping it", TILE_FETCH_TIMEOUT_SECONDS);
                    future.cancel(true);
                }
            }
            fetchExecutor.shutdown();
            return new SlideAnnotationsResult(annotationPathObjects, allTiles.size());
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName, e);
            return new SlideAnnotationsResult(new ArrayList<>(), 0);
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
