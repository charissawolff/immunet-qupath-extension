package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import org.computational_immunology.ext.ImmuNet.core.SelectedSlide;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.imageServers.SlideImageServer;
import org.computational_immunology.ext.ImmuNet.core.models.AnnotationPoint;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;

import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;


/**
 * Combines SelectSlideCommand (open the slide) and PresentAnnotationsCommand (fetch its
 * annotations) behind one Task-shaped surface, so callers bind to a single message/state pair
 * instead of tracking two Tasks by hand.
 * cancelling the workflow cancels both phases, and the workflow is only considered done once both phases reach terminated.
 */
public class SlideLoadWorkflow {

    private final String datasetName;
    private final String slideName;
    private final SelectSlideCommand selectSlideCommand;
    private final LoadSlideAnnotationCommand presentAnnotationsCommand;
    private final SelectedDataStore selectedDataStore;

    private final StringProperty message = new SimpleStringProperty("");
    private final ObjectProperty<Worker.State> state = new SimpleObjectProperty<>(Worker.State.READY);

    private Consumer<SelectedSlide> onSlideReady;

    public SlideLoadWorkflow(String datasetName, String slideName, double compositeSwitchDownsample,
                              ServerGateway serverGateway, Boolean useTiffComposite,
                              SelectedDataStore selectedDataStore) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.selectSlideCommand = new SelectSlideCommand(datasetName, slideName, compositeSwitchDownsample, serverGateway, useTiffComposite);
        this.presentAnnotationsCommand = new LoadSlideAnnotationCommand(selectedDataStore, serverGateway);
        this.selectedDataStore = selectedDataStore;
    }

    /**
     * @param onSlideReady called once tile metadata first exists (slide loaded, before annotations finish
     * fetching) with an immutable snapshot of the dataset/slide/tiles just loaded.
     */
    public void setOnSlideReady(Consumer<SelectedSlide> onSlideReady) {
        this.onSlideReady = onSlideReady;
    }

    public void build() {
        selectSlideCommand.build();
        presentAnnotationsCommand.build();

        // Both phases run one after another, never at the same time, so whichever task last
        // fired a message/state change is always the one that should currently be shown.
        selectSlideCommand.getTask().messageProperty().addListener((obs, oldMsg, newMsg) -> message.set(newMsg));
        presentAnnotationsCommand.getTask().messageProperty().addListener((obs, oldMsg, newMsg) -> message.set(newMsg));

        selectSlideCommand.setOnFailed(() -> state.set(Worker.State.FAILED));
        selectSlideCommand.setOnCancelled(() -> state.set(Worker.State.CANCELLED));
        selectSlideCommand.setOnDone(() -> {
            // Slide loading is done, but annotation fetching is about to start. Set the data in the store for the app to use.
            selectedDataStore.setSelectedSlide(new SelectedSlide(datasetName, slideName, selectSlideCommand.getTilesMetadata()));
            selectedDataStore.setDownSampleComposite(SlideImageServer.getDownsampleComposite());

            presentAnnotationsCommand.start();
            if (onSlideReady != null) {
                onSlideReady.accept(selectedDataStore.getSelectedSlide());
            }
        });

        presentAnnotationsCommand.setOnDone(() -> {
            state.set(Worker.State.SUCCEEDED);
            List<AnnotationPoint> annotationPoints = presentAnnotationsCommand.getTask().getValue();
            message.set("Fetched " + annotationPoints.size() + " annotations. There are a total of " + selectedDataStore.getSelectedSlide().getTileMetadataList().size() + " tiles.");
        });
        //if failed, don't clear the datastore, as the slide is still loaded and can be used. 
        presentAnnotationsCommand.setOnFailed(() -> {
            state.set(Worker.State.FAILED);
            message.set("Failed to fetch annotations.");
        });
        //if cancelled, we have to clear the datastore AND the viewer, else the other 
        // commands will still think there is a slide loaded and can try to use it,
        presentAnnotationsCommand.setOnCancelled(() -> {
            state.set(Worker.State.CANCELLED);
            selectedDataStore.clear();
            ClearImageViewerCommand.execute();
        }); 
    }

    public void start() {
        selectedDataStore.clear();
        ClearImageViewerCommand.execute();
        selectSlideCommand.start();
    }

    public void cancel() {
        //user cancelled the workflow while the slide was loading or while annotations were being fetched, so cancel both tasks and clear the viewer
        // also clear the datastore
        selectSlideCommand.getTask().cancel();
        presentAnnotationsCommand.getTask().cancel();
        selectedDataStore.clear();
        ClearImageViewerCommand.execute();
    }

    public boolean isDone() {
        Worker.State slideState = selectSlideCommand.getTask().getState();
        Worker.State annotationState = presentAnnotationsCommand.getTask().getState();
        return isCancelledOrFailed(slideState) || isCancelledOrFailed(annotationState)
                || (slideState == Worker.State.SUCCEEDED && annotationState == Worker.State.SUCCEEDED);
    }

    private static boolean isCancelledOrFailed(Worker.State state) {
        return state == Worker.State.FAILED || state == Worker.State.CANCELLED;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public ObjectProperty<Worker.State> stateProperty() {
        return state;
    }
}
