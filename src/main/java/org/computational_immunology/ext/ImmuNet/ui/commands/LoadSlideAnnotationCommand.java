package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;

import org.computational_immunology.ext.ImmuNet.core.AnnotationPointConverter;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;

import qupath.lib.objects.PathObject;

public class LoadSlideAnnotationCommand extends AbstractAsyncCommand<List<AnnotationPoint>>  {
    // Annotation-fetching is cancellable, but we also keep a timeout per tile so a single
    // slow/stuck tile can't block the rest even when nothing has been cancelled.
    private static final long TILE_FETCH_TIMEOUT_SECONDS = 10;

    private final SelectedDataStore selectedDataStore;
    private final AnnotationRequestHandler annotationRequestHandler;
    private volatile ExecutorService fetchExecutor;
    private String datasetName;
    private String slideName;
    private List<TileMetadata> tilesMetadata;
    private double downsampleComposite;


    public LoadSlideAnnotationCommand(SelectedDataStore selectedDataStore, AnnotationRequestHandler annotationRequestHandler) {
        this.selectedDataStore = selectedDataStore;
        this.annotationRequestHandler = annotationRequestHandler;

    }

    @Override
    protected void onSuccess(List<AnnotationPoint> annotationPoints) {
        selectedDataStore.setAnnotationPoints(annotationPoints);
        List<PathObject> pathObjects = AnnotationPointConverter.toPathObjects(annotationPoints, tilesMetadata, downsampleComposite);
        AttachPathObjectsToViewerCommand attachCommand = new AttachPathObjectsToViewerCommand(pathObjects);
        attachCommand.execute();
        ImmuNetLog.log("Attempted: Added " + pathObjects.size() + " server annotation(s) for {}/{}", datasetName, slideName);
    }

    @Override
    protected List<AnnotationPoint> execute(Consumer<String> progressReporter) throws Exception {
        datasetName = selectedDataStore.getSelectedSlide().getDatasetName();
        slideName = selectedDataStore.getSelectedSlide().getSlideName();
        tilesMetadata = selectedDataStore.getSelectedSlide().getTileMetadataList();
        downsampleComposite = selectedDataStore.getDownSampleComposite();
        progressReporter.accept("Fetching annotations for dataset: " + datasetName + ", slide: " + slideName);
        if (tilesMetadata == null) {
            ImmuNetLog.error("fetchSlideAnnotations called without tile metadata set. You need to call setTilesMetadata first for dataset: "
                    + datasetName + ", slide: " + slideName);
            return new ArrayList<>();
        }
        try {
            List<String> tileCodes = annotationRequestHandler.fetchSlideAnnotations(datasetName, slideName);
            if (task.isCancelled()) {
                // avoid creating the executor at all if we were cancelled while fetching tile codes,
                // so a cancelled fetch can never come up with a fresh pool after the caller was told we're done
                return new ArrayList<>();
            }
            List<AnnotationPoint> annotations = fetchAnnotations(tileCodes, tilesMetadata, progressReporter);
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName, e);
            //return empty list
            return new ArrayList<AnnotationPoint>();
        }
    }

    private List<AnnotationPoint> fetchAnnotations(List<String> tileCodes, List<TileMetadata> tileMetadataList, Consumer<String> progressReporter) {
        try { 
        fetchExecutor = Executors.newFixedThreadPool(64);
            List<Future<List<AnnotationPoint>>> futureList = new ArrayList<>();
            for (String tileCode : tileCodes) {
                TileMetadata tileMetadata = TileMetadata.findByCode(tileCode, tileMetadataList);
                if (tileMetadata == null) {
                    ImmuNetLog.error("No tile metadata found for tile code: {} skipping its annotations", tileCode);
                    continue;
                }
                Future<List<AnnotationPoint>> future = fetchExecutor.submit(() -> fetchTileAnnotations(tileCode, tilesMetadata));
                futureList.add(future);
            }
            List<AnnotationPoint> annotations = new ArrayList<>();
            for (Future<List<AnnotationPoint>> future : futureList) {
                try {
                    annotations.addAll(future.get(TILE_FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    progressReporter.accept("Timed out after " + TILE_FETCH_TIMEOUT_SECONDS + " seconds waiting for a tile's annotations so we are skipping it");
                    ImmuNetLog.error("Timed out after {} seconds waiting for a tile's annotations so we are skipping it", TILE_FETCH_TIMEOUT_SECONDS);
                    future.cancel(true);
                } catch (InterruptedException e) {
                    ImmuNetLog.error("Cancelled while fetching annotations.", e);
                    return new ArrayList<AnnotationPoint>();
                }
            }
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName, e);
            return new ArrayList<AnnotationPoint>();
        } finally {
            // always runs so the pool can never be left running
            // in the background after this method returns.
            if (fetchExecutor != null) {
                fetchExecutor.shutdownNow();
            }
        }
    }

    public List<AnnotationPoint> fetchTileAnnotations(String tileCode, List<TileMetadata> tileMetadataList) {
        try{
            List<AnnotationPoint> annotations = annotationRequestHandler.fetchAnnotations(datasetName, slideName, tileCode);
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName + ", tile: " + tileCode, e);
            return new ArrayList<>();
        }
    }

    public boolean isFullyStopped() {
        ExecutorService e = fetchExecutor;
        return e == null || e.isTerminated();
    }

}
