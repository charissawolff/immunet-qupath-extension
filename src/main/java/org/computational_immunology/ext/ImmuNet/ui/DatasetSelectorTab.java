package org.computational_immunology.ext.ImmuNet.ui;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.ui.commands.SlideLoadWorkflow;

import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;

public class DatasetSelectorTab extends CustomSidePanelTab {

    private final ImageRequestHandler imageRequestHandler;
    private final AnnotationRequestHandler annotationRequestHandler;
    private SlideLoadWorkflow currentWorkflow;
    private final PauseTransition buttonPause = new PauseTransition((Duration.seconds(3)));

    public DatasetSelectorTab(ImageRequestHandler imageRequestHandler, AnnotationRequestHandler annotationRequestHandler) {
        super("Image selector");
        this.imageRequestHandler = imageRequestHandler;
        this.annotationRequestHandler = annotationRequestHandler;

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
        Label statusLabel = new Label();

        // add a slider to see what user wants to transition composite to be. The smaller the better
        Slider compositeTransitionSlider = new Slider(0.2, 12, 1.5);
        Label infoIcon = new Label("ⓘ");
        infoIcon.setTooltip(new Tooltip(
            "Controls how much you need to zoom in before the viewer loads full detail images.\n" +
            "Lower = safer for weaker machines (more zoom needed before detail loads).\n" +
            "Higher = sharper images sooner, but uses more memory. \n" +
            "WARNING: If you set this higher, make sure that you allocate enough memory from your system. \n"+
            "Change this at (Edit > Preferences > General > Maximum memory) and restart the application. \n" +
            "If your machine crashes upon loading in slides, lower this or avoid zooming at all before tile selection."
        ));
        HBox sliderRow = new HBox(5, new Label("Safer zoom"), compositeTransitionSlider, new Label("Sharper, sooner"), infoIcon);

        // set the the button will return to default when button pause; after button is "done" showing whatever it had to show
        buttonPause.setOnFinished(event -> {
            openImgBtn.setStyle("-fx-text-fill: black;");
            openImgBtn.setText("Open Slide");
            //statusLabel.setVisible(false);
            //statusLabel.setManaged(false);
        });

        openImgBtn.setOnAction(e -> {
            if (dsBox.getListView().getSelectionModel().isEmpty() || tsBox.getListView().getSelectionModel().isEmpty()) {
                ImmuNetLog.error("No dataset or slide selected for opening.");
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
                statusLabel.setText("No dataset or slide selected for opening.");
                return;
            }
            try{
                String dsName = dsBox.getListView().getSelectionModel().selectedItemProperty().getValue();
                String tsName = tsBox.getListView().getSelectionModel().selectedItemProperty().getValue();
                if (currentWorkflow != null && !currentWorkflow.isDone()) {
                    currentWorkflow.cancel();
                    return;
                }
                SlideLoadWorkflow workflow = new SlideLoadWorkflow(dsName, tsName, compositeTransitionSlider.getValue(), imageRequestHandler, annotationRequestHandler);
                workflow.build();

                workflow.messageProperty().addListener((obs, oldMsg, newMsg) -> statusLabel.setText(newMsg)); // bind the combined status message to the label
                workflow.stateProperty().addListener((obs, oldState, newState) -> {
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

                workflow.start();
                currentWorkflow = workflow;

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

        sidePanelTab.getChildren().addAll(loadDataBtn, dsBox.getBox(), tsBox.getBox(), openImgBtn, statusLabel, sliderRow);

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
