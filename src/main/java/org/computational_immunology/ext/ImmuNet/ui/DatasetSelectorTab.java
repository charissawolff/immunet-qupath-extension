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
                command.setOnDone(() -> statusLabel.setText("Done!")); // when the image is set in viewer, show Done in label
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
                    } else if (newState == Worker.State.FAILED) {
                        openImgBtn.setStyle("-fx-text-fill: red;");
                        openImgBtn.setText("Failed");
                    } else {
                        return; 
                    }
                    PauseTransition pause = new PauseTransition(Duration.seconds(5));
                    pause.setOnFinished(event -> {
                        openImgBtn.setStyle("-fx-text-fill: black;");
                        openImgBtn.setText("Open Slide");
                    });
                    pause.play();
                });
                openImgBtn.setText("Cancel");
                openImgBtn.setStyle("-fx-text-fill: red;");
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
