package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.SelectedSlide;
import org.computational_immunology.ext.ImmuNet.core.SlideImageServer;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import java.util.List;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;

import qupath.lib.objects.PathObject;

/**
 * Combines SelectSlideCommand (open the slide) and PresentAnnotationsCommand (fetch its
 * annotations) behind one Task-shaped surface, so callers bind to a single message/state pair
 * instead of tracking two Tasks by hand.
 * cancelling the workflow cancels both phases, and the workflow is only considered done once both phases are fully stopped.
 */
public class SlideLoadWorkflow {

    private final String datasetName;
    private final String slideName;
    private final SelectSlideCommand selectSlideCommand;
    private final PresentAnnotationsCommand presentAnnotationsCommand;

    private final StringProperty message = new SimpleStringProperty("");
    private final ObjectProperty<Worker.State> state = new SimpleObjectProperty<>(Worker.State.READY);

    private Consumer<SelectedSlide> onSlideReady;

    public SlideLoadWorkflow(String datasetName, String slideName, double compositeSwitchDownsample,
                              ImageRequestHandler imageRequestHandler, AnnotationRequestHandler annotationRequestHandler) {
        this.datasetName = datasetName;
        this.slideName = slideName;
        this.selectSlideCommand = new SelectSlideCommand(datasetName, slideName, compositeSwitchDownsample, imageRequestHandler);
        this.presentAnnotationsCommand = new PresentAnnotationsCommand(datasetName, slideName, annotationRequestHandler);
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

        // Reaching SUCCEEDED here only means slide loading is done and annotation fetching is about to start, not
        // that the whole workflow is done, so don't forward it as a terminal state.
        selectSlideCommand.getTask().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                state.set(newState);
            }
        });
        presentAnnotationsCommand.getTask().stateProperty().addListener((obs, oldState, newState) -> {
            state.set(newState);
            if (newState == Worker.State.SUCCEEDED) {
                // Only known once annotation-fetching has actually finished, so build the final
                List<PathObject> pathObjects = presentAnnotationsCommand.getTask().getValue();
                message.set("Fetched " + pathObjects.size() + " annotations in " + presentAnnotationsCommand.getAnnotatedTileCount()
                        + " tiles. There are a total of " + selectSlideCommand.getTilesMetadata().size() + " tiles.");
            }
        });

        // Only start fetching annotations once the slide itself has actually finished loading
        selectSlideCommand.setOnDone(() -> {
            presentAnnotationsCommand.setTilesMetadata(selectSlideCommand.getTilesMetadata());
            presentAnnotationsCommand.setDownsampleComposite(SlideImageServer.getDownsampleComposite());
            presentAnnotationsCommand.start();
            if (onSlideReady != null) {
                // set the selected slide here since it finished loading
                onSlideReady.accept(new SelectedSlide(datasetName, slideName, selectSlideCommand.getTilesMetadata()));
            }
        });
    }

    public void start() {
        selectSlideCommand.start();
    }

    public void cancel() {
        selectSlideCommand.getTask().cancel();
        presentAnnotationsCommand.getTask().cancel();
    }

    public boolean isDone() {
        Worker.State slideState = selectSlideCommand.getTask().getState();
        Worker.State annotationState = presentAnnotationsCommand.getTask().getState();
        if ((isCancelledOrFailed(slideState) || isCancelledOrFailed(annotationState)) || (slideState == Worker.State.SUCCEEDED && annotationState == Worker.State.SUCCEEDED)) {
            if (selectSlideCommand.isFullyStopped() && presentAnnotationsCommand.isFullyStopped()) {
                return true;
            }
            return false; // still waiting for executors to finish
        }
        return false;
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
