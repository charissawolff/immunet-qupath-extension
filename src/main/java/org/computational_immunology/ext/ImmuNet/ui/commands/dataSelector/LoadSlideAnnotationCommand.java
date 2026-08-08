package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPointConverter;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.AttachPathObjectsToViewerCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.annotations.RegisterNewClassificationsCommand;

import qupath.lib.objects.PathObject;

public class LoadSlideAnnotationCommand extends AbstractAsyncCommand<List<AnnotationPoint>>  {
    private final SelectedDataStore selectedDataStore;
    private final ServerGateway serverGateway;
    private volatile ExecutorService fetchExecutor;
    private String datasetName;
    private String slideName;
    private List<TileMetadata> tilesMetadata;
    private double downsampleComposite;


    public LoadSlideAnnotationCommand(SelectedDataStore selectedDataStore, ServerGateway serverGateway) {
        this.selectedDataStore = selectedDataStore;
        this.serverGateway = serverGateway;

    }

    @Override
    protected void onSuccess(List<AnnotationPoint> annotationPoints) {
        selectedDataStore.setAnnotationPoints(annotationPoints);
        List<PathObject> pathObjects = AnnotationPointConverter.toPathObjects(annotationPoints, tilesMetadata, downsampleComposite);
        RegisterNewClassificationsCommand registerClassificationsCommand = new RegisterNewClassificationsCommand(pathObjects);
        registerClassificationsCommand.execute();
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
            List<String> tileCodes = serverGateway.fetchSlideAnnotations(datasetName, slideName);
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
                    annotations.addAll(future.get());
                } catch (ExecutionException e) {
                    ImmuNetLog.error("Unexpected error fetching a tile's annotations", e);
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
            List<AnnotationPoint> annotations = serverGateway.fetchAnnotations(datasetName, slideName, tileCode);
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName + ", tile: " + tileCode, e);
            return new ArrayList<>();
        }
    }
}
