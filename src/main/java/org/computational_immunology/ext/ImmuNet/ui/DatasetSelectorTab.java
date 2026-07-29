package org.computational_immunology.ext.ImmuNet.ui;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;
import org.computational_immunology.ext.ImmuNet.ui.commands.SelectSlideCommand;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Label;

public class DatasetSelectorTab extends CustomSidePanelTab {

    private final ImageRequestHandler imageRequestHandler;
    private Task<?> currentLoadTask;
    Label statusLabel = new Label();
    private final PauseTransition buttonPause = new PauseTransition((Duration.seconds(3)));

    public DatasetSelectorTab(ImageRequestHandler imageRequestHandler) {
        super("Image selector");
        this.imageRequestHandler = imageRequestHandler;
    }

    /**
     * Creates a panel with a button and two boxes underneath eachother.
     * @return The full panel with button and box components.
     */
    @Override
    public VBox getContent(){
        VBox sidePanelTab = new VBox();
        sidePanelTab.setPadding(new Insets(10, 10, 10, 10)); // Box margins
        sidePanelTab.setSpacing(5); // Space between buttons and boxes

        // Interactive selection boxes
        ListViewerBox dsBox = new ListViewerBox(300, sidePanelTab.getMaxWidth()); // Dataset
        ListViewerBox tsBox = new ListViewerBox(300, sidePanelTab.getMaxWidth()); // Tissue slide

        Button loadDataBtn = makeButton("Load Datasets", new Dimensions(40, 100));
        loadDataBtn.setOnAction(e -> MenuActions.updateListViewerBox(dsBox, getDatasets()));

        Button openImgBtn = makeButton("Open Slide", new Dimensions(40, 100));

        // set the the button will return to default when button pause; after button is "done" showing whatever it had to show
        buttonPause.setOnFinished(event -> {
            openImgBtn.setStyle("-fx-text-fill: black;");
            openImgBtn.setText("Open Slide");
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        });

        openImgBtn.setOnAction(e -> {
            try{
                String dsName = dsBox.getListView().getSelectionModel().selectedItemProperty().getValue();
                String tsName = tsBox.getListView().getSelectionModel().selectedItemProperty().getValue();
                if (currentLoadTask != null && !currentLoadTask.isDone()) {
                    currentLoadTask.cancel();
                    return;
                }
                SelectSlideCommand command = new SelectSlideCommand(dsName, tsName, imageRequestHandler);
                command.build();
                // hide label after done fetching slide image and presenting it, 
                //use setOnDone because else it's hidden before the image is visible if I use the worker.state.succeeded
                command.setOnDone(() -> {
                    statusLabel.setVisible(false);
                    statusLabel.setManaged(false);
                }); 
                command.start();
                currentLoadTask = command.getTask();
                currentLoadTask.messageProperty().addListener((obs, oldMsg, newMsg) -> statusLabel.setText(newMsg)); // bind the messages from the status of the command to the text
                currentLoadTask.stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        openImgBtn.setStyle("-fx-text-fill: green;");
                        openImgBtn.setText("Success");
                    } else if (newState == Worker.State.CANCELLED) {
                        openImgBtn.setStyle("-fx-text-fill: red;");
                        openImgBtn.setText("Cancelled");
                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                    } else if (newState == Worker.State.FAILED) {
                        openImgBtn.setStyle("-fx-text-fill: red;");
                        openImgBtn.setText("Failed");
                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                    } else {
                        return; 
                    }
                    buttonPause.stop();
                    buttonPause.playFromStart();
                });
                openImgBtn.setText("Cancel");
                openImgBtn.setStyle("-fx-text-fill: black;");
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
                //stop whatever pause is happening
                buttonPause.stop();
            } catch (NullPointerException exc){
                ImmuNetLog.error("No dataset of slide selected for opening.", exc);
            }
        });

        updateSlideByDataset(dsBox, tsBox);

        sidePanelTab.getChildren().addAll(loadDataBtn, dsBox.getBox(), tsBox.getBox(), openImgBtn, statusLabel);

        return sidePanelTab;
    }

    private List<String> getDatasets(){
        return imageRequestHandler.getWebpageAsList("datasets/");
    }

    private List<String> getSlides(String dataset){
        return imageRequestHandler.getWebpageAsList("datasets/" + dataset + "/");
    }

    private void updateSlideByDataset(ListViewerBox datasetBox, ListViewerBox slideBox){
        datasetBox.getListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null){
                    MenuActions.updateListViewerBox(slideBox, getSlides(newValue)); // Update tissue slide box
                    ImmuNetLog.log("Selected: " + newValue);
                }
            }
        );
    }
}
