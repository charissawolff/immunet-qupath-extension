package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPointConverter;
import org.computational_immunology.ext.ImmuNet.core.models.PredictionAnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.models.PredictionPointConverter;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.AttachPathObjectsToViewerCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.annotations.RegisterNewClassificationsCommand;

import qupath.lib.objects.PathObject;

public class LoadPredictionAnnotationCommand extends AbstractAsyncCommand<List<PredictionAnnotationPoint>>  {
    private final SelectedDataStore selectedDataStore;
    private final ServerGateway serverGateway;
    private volatile ExecutorService fetchExecutor;
    private String datasetName;
    private String slideName;
    private List<TileMetadata> tilesMetadata;


    public LoadPredictionAnnotationCommand(SelectedDataStore selectedDataStore, ServerGateway serverGateway) {
        this.selectedDataStore = selectedDataStore;
        this.serverGateway = serverGateway;

    }

    @Override
    protected void onSuccess(List<PredictionAnnotationPoint> points) {
        selectedDataStore.setPredictionAnnotationPoints(points);
        List<PathObject> pathObjects = PredictionPointConverter.toPathObjects(points, tilesMetadata);
        RegisterNewClassificationsCommand registerClassificationsCommand = new RegisterNewClassificationsCommand(pathObjects);
        registerClassificationsCommand.execute();
        AttachPathObjectsToViewerCommand attachCommand = new AttachPathObjectsToViewerCommand(pathObjects);
        attachCommand.execute();
        ImmuNetLog.log("Attempted: Added " + pathObjects.size() + " server annotation(s) for {}/{}", datasetName, slideName);
    }

    @Override
    protected List<PredictionAnnotationPoint> execute(Consumer<String> progressReporter) throws Exception {
        datasetName = selectedDataStore.getSelectedSlide().getDatasetName();
        slideName = selectedDataStore.getSelectedSlide().getSlideName();
        tilesMetadata = selectedDataStore.getSelectedSlide().getTileMetadataList();
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
            List<PredictionAnnotationPoint> annotations = fetchAnnotations(tileCodes, tilesMetadata, progressReporter);
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName, e);
            //return empty list
            return new ArrayList<PredictionAnnotationPoint>();
        }
    }

    private List<PredictionAnnotationPoint> fetchAnnotations(List<String> tileCodes, List<TileMetadata> tileMetadataList, Consumer<String> progressReporter) {
        try { 
        fetchExecutor = Executors.newFixedThreadPool(10);
            List<Future<List<PredictionAnnotationPoint>>> futureList = new ArrayList<>();
            for (String tileCode : tileCodes) {
                TileMetadata tileMetadata = TileMetadata.findByCode(tileCode, tileMetadataList);
                if (tileMetadata == null) {
                    ImmuNetLog.error("No tile metadata found for tile code: {} skipping its annotations", tileCode);
                    continue;
                }
                Future<List<PredictionAnnotationPoint>> future = fetchExecutor.submit(() -> fetchTileAnnotations(tileCode, tilesMetadata));
                futureList.add(future);
            }
            List<PredictionAnnotationPoint> annotations = new ArrayList<>();
            for (Future<List<PredictionAnnotationPoint>> future : futureList) {
                try {
                    annotations.addAll(future.get());
                } catch (ExecutionException e) {
                    ImmuNetLog.error("Unexpected error fetching a tile's annotations", e);
                } catch (InterruptedException e) {
                    ImmuNetLog.error("Cancelled while fetching annotations.", e);
                    return new ArrayList<PredictionAnnotationPoint>();
                }
            }
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName, e);
            return new ArrayList<PredictionAnnotationPoint>();
        } finally {
            // always runs so the pool can never be left running
            // in the background after this method returns.
            if (fetchExecutor != null) {
                fetchExecutor.shutdownNow();
            }
        }
    }

    public List<PredictionAnnotationPoint> fetchTileAnnotations(String tileCode, List<TileMetadata> tileMetadataList) {
        try{
            String modelName = "default";
            List<PredictionAnnotationPoint> annotations = serverGateway.fetchPredictionAnnotations(datasetName, slideName, tileCode, modelName);
            return annotations;
        } catch (Exception e) {
            ImmuNetLog.error("Error fetching annotations for dataset: " + datasetName + ", slide: " + slideName + ", tile: " + tileCode, e);
            return new ArrayList<>();
        }
    }

    
}

