package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;

/**
 * Combines SelectSlideCommand (open the slide) and PresentAnnotationsCommand (fetch its
 * annotations) behind one Task-shaped surface, so callers bind to a single message/state pair
 * instead of tracking two Tasks by hand.
 *
 * Cancellation only interferes when loading slides. Once annotation-fetching has started,
 * cancel() does nothing. INstead we rely on the timeout per tile... Why? easier to implement
 */
public class SlideLoadWorkflow {

    private final SelectSlideCommand selectSlideCommand;
    private final PresentAnnotationsCommand presentAnnotationsCommand;

    private final StringProperty message = new SimpleStringProperty("");
    private final ObjectProperty<Worker.State> state = new SimpleObjectProperty<>(Worker.State.READY);

    public SlideLoadWorkflow(String datasetName, String slideName, double compositeSwitchDownsample,
                              ImageRequestHandler imageRequestHandler, AnnotationRequestHandler annotationRequestHandler) {
        this.selectSlideCommand = new SelectSlideCommand(datasetName, slideName, compositeSwitchDownsample, imageRequestHandler);
        this.presentAnnotationsCommand = new PresentAnnotationsCommand(datasetName, slideName, annotationRequestHandler, imageRequestHandler);
    }

    public void build() {
        selectSlideCommand.build();
        presentAnnotationsCommand.build();

        // Both phases run one after another, never at the same time, so whichever task last
        // fired a message/state change is always the one that should currently be shown.
        selectSlideCommand.getTask().messageProperty().addListener((obs, oldMsg, newMsg) -> message.set(newMsg));
        presentAnnotationsCommand.getTask().messageProperty().addListener((obs, oldMsg, newMsg) -> message.set(newMsg));

        selectSlideCommand.getTask().stateProperty().addListener((obs, oldState, newState) -> state.set(newState));
        presentAnnotationsCommand.getTask().stateProperty().addListener((obs, oldState, newState) -> state.set(newState));

        // Only start fetching annotations once the slide itself has actually finished loading.
        selectSlideCommand.setOnDone(presentAnnotationsCommand::start);
    }

    public void start() {
        selectSlideCommand.start();
    }

    public void cancel() {
        selectSlideCommand.getTask().cancel();
    }

    public boolean isDone() {
        Worker.State loadState = selectSlideCommand.getTask().getState();
        if (loadState != Worker.State.SUCCEEDED) {
            // Still loading the slide, or the slide load itself ended (failed/cancelled) -
            // either way annotation-fetching never started, so this state alone decides it.
            return isTerminal(loadState);
        }
        return isTerminal(presentAnnotationsCommand.getTask().getState());
    }

    private static boolean isTerminal(Worker.State state) {
        return state == Worker.State.SUCCEEDED || state == Worker.State.FAILED || state == Worker.State.CANCELLED;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public ObjectProperty<Worker.State> stateProperty() {
        return state;
    }
}
