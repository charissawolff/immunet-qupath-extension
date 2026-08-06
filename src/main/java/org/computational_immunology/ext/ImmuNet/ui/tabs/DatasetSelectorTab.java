package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.MiscDataRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverController;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.ClearImageViewerCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.LoadDatasetsCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.LoadSlideDataCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.SetPolygonVisibilityCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.SlideLoadWorkflow;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;

public class DatasetSelectorTab extends CustomSidePanelTab {

    private final ImageRequestHandler imageRequestHandler;
    private final AnnotationRequestHandler annotationRequestHandler;
    private final MiscDataRequestHandler miscDatarequestHandler;
    private final TileHoverController tileHoverController;
    private final SelectedDataStore selectedDataStore;
    private SlideLoadWorkflow currentWorkflow;
    private final PauseTransition buttonPause = new PauseTransition((Duration.seconds(2)));

    public DatasetSelectorTab(ImageRequestHandler imageRequestHandler, AnnotationRequestHandler annotationRequestHandler, 
                                MiscDataRequestHandler miscDatarequestHandler, SelectedDataStore selectedDataStore, TileHoverController tileHoverController) {
        super("Image selector");
        this.imageRequestHandler = imageRequestHandler;
        this.annotationRequestHandler = annotationRequestHandler;
        this.miscDatarequestHandler = miscDatarequestHandler;
        this.selectedDataStore = selectedDataStore;
        this.tileHoverController = tileHoverController;

    }

    /**
     * Creates a panel with a button and two boxes underneath eachother.
     * @return The full panel with button and box components.
     */
    @Override
    public VBox getContent(){
        VBox sidePanelTab = new VBox();
        sidePanelTab.setPadding(new Insets(10, 10, 10, 10)); // Box padding
        sidePanelTab.setSpacing(5); // Space between buttons and boxes

        // Interactive selection boxes
        ListViewerBox dsBox = new ListViewerBox(250, sidePanelTab.getMaxWidth()); // Dataset
        ListViewerBox tsBox = new ListViewerBox(250, sidePanelTab.getMaxWidth()); // slides
        VBox.setMargin(tsBox.getBox(), new Insets(15, 2, 2, 2)); // Box padding
        VBox.setMargin(dsBox.getBox(), new Insets(5, 2, 2, 2)); // Box padding


        //two buttons next to each other, one for loading datasets and one for clearing the current selection from viewer
        Button loadDataBtn = makeButton("Load Datasets", new Dimensions(40, 120));
        loadDataBtn.setOnAction(e -> {
            LoadDatasetsCommand loadDatasetCommand = new LoadDatasetsCommand(miscDatarequestHandler);
            loadDatasetCommand.build();
            loadDatasetCommand.setOnDone(() -> {
                List<String> datasets = loadDatasetCommand.getTask().getValue();
                updateListViewerBox(dsBox, datasets);
            });
            loadDatasetCommand.setOnFailed(() -> {
                ImmuNetLog.error("Failed to load dataset data", loadDatasetCommand.getTask().getException());
            });
            loadDatasetCommand.start();
        });
        
        // checkbox to show or not the tile overlay
        CheckBox tileOverlayCheckbox = new CheckBox("Show tile overlay when hovering");
        tileOverlayCheckbox.setSelected(true);
        tileOverlayCheckbox.selectedProperty().addListener((obs, wasSelected, isSelected) -> { 
            if (isSelected) {
                tileHoverController.setShow();
            } else {
                tileHoverController.setDontShow();
            }
        });
            

        Button clearSelectionBtn = makeButton("Clear Image", new Dimensions(40, 120));
        clearSelectionBtn.setOnAction(e -> {
            selectedDataStore.clear();
            ClearImageViewerCommand.execute();
        });
        // bind to the selected slide property of the datastore, so that the button is only enabled when a slide is selected
        // this continues working after the slide is cleaered, because when we click this button, we also
        // clear the data from the datastore, which in turn enables the button again
        clearSelectionBtn.disableProperty().bind(Bindings.isNull(selectedDataStore.selectedSlideProperty()));

        BorderPane buttonRow = new BorderPane();
        buttonRow.setLeft(loadDataBtn);
        buttonRow.setRight(clearSelectionBtn);
        

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

        //WE HAVE to make that if this button is clicked while the workflow is running, 
        // it will ONLY cancel the workflow and clear the viewer. 
        // Otherwise, if the user clicks this button again, it will start a new workflow while the old one is still running,
        //  which can cause problems.
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
                SlideLoadWorkflow workflow = new SlideLoadWorkflow(dsName, tsName, compositeTransitionSlider.getValue(), imageRequestHandler, annotationRequestHandler, selectedDataStore);
                workflow.build();

                workflow.setOnSlideReady(slide -> {
                    QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
                    if (viewer != null) {
                        tileHoverController.setSlide(slide, viewer);
                    }
                });

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

        sidePanelTab.getChildren().addAll(buttonRow, dsBox.getBox(), tsBox.getBox(), openImgBtn, statusLabel, sliderRow, tileOverlayCheckbox);

        return sidePanelTab;
    }

    private void updateSlideByDataset(ListViewerBox datasetBox, ListViewerBox slideBox){
        datasetBox.getListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null){
                    LoadSlideDataCommand loadSlideDataCommand = new LoadSlideDataCommand(miscDatarequestHandler, newValue);
                    loadSlideDataCommand.build(); // Update tissue slide box
                    loadSlideDataCommand.setOnDone(() -> {
                        List<String> slides = loadSlideDataCommand.getTask().getValue();
                        updateListViewerBox(slideBox, slides);
                    });
                    loadSlideDataCommand.setOnFailed(() -> {
                        ImmuNetLog.error("Failed to load slide data for dataset: " + newValue, loadSlideDataCommand.getTask().getException());
                    });
                    loadSlideDataCommand.start();
                    ImmuNetLog.log("Selected: " + newValue);
                }
            }
        );
    }

    private static void updateListViewerBox(ListViewerBox box, List<String> list){
        box.setItems(list);
    }
}
