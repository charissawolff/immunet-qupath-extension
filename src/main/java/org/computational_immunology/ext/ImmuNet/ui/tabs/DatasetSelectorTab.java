package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverController;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.ClearImageViewerCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.LoadDatasetsCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.LoadSlideDataCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.SlideLoadWorkflow;
import org.computational_immunology.ext.ImmuNet.ui.tabBoxes.ListViewerBox;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;

public class DatasetSelectorTab extends CustomSidePanelTab {

    private final ServerGateway serverGateway;
    private final TileHoverController tileHoverController;
    private final SelectedDataStore selectedDataStore;
    private SlideLoadWorkflow currentWorkflow;
    private final PauseTransition buttonPause = new PauseTransition((Duration.seconds(2)));

    public DatasetSelectorTab(ServerGateway serverGateway, SelectedDataStore selectedDataStore,
                                TileHoverController tileHoverController) {
        super("Image selector");
        this.serverGateway = serverGateway;
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

        //SEARCH BAR for dsBox
        ObservableList<String> dsItems = dsBox.getListView().getItems(); // 
        FilteredList<String> filteredItems = new FilteredList<>(dsItems, s -> true);
        dsBox.getListView().setItems(filteredItems);
        TextField searchField = new TextField();
        searchField.setPromptText("Search for a dataset...");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredItems.setPredicate(item ->
                query.isEmpty() || item.toLowerCase().contains(query)
            );
        });

        //two buttons next to each other, one for loading datasets and one for clearing the current selection from viewer
        Button loadDataBtn = makeButton("Load Datasets", new Dimensions(30, 120));
        loadDataBtn.setOnAction(e -> {
            LoadDatasetsCommand loadDatasetCommand = new LoadDatasetsCommand(serverGateway);
            loadDatasetCommand.build();
            loadDatasetCommand.setOnDone(() -> {
                List<String> datasets = loadDatasetCommand.getTask().getValue();
                 dsItems.setAll(datasets);
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
            

        Button clearSelectionBtn = makeButton("Clear Image", new Dimensions(30, 120));
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
        

        Button openImgBtn = makeButton("Open Slide", new Dimensions(30, 100));
        Label statusLabel = new Label();

        // add a slider to see what user wants to transition composite to be. The smaller the better
        // set the the button will return to default when button pause; after button is "done" showing whatever it had to show
        buttonPause.setOnFinished(event -> {
            openImgBtn.setStyle("-fx-text-fill: black;");
            openImgBtn.setText("Open Slide");
            //statusLabel.setVisible(false);
            //statusLabel.setManaged(false);
        });

        //a toggle radio button for jpg or .tiff composite
        ToggleGroup RadioButtongroup = new ToggleGroup();
        RadioButton buttonJpg = new RadioButton(".jpg");
        RadioButton buttonTiff = new RadioButton(".tiff");
        buttonJpg.setSelected(true); // default to jpg
        buttonJpg.setToggleGroup(RadioButtongroup);
        buttonTiff.setToggleGroup(RadioButtongroup);

        SimpleBooleanProperty useTiffComposite = new SimpleBooleanProperty(false);
        RadioButtongroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            useTiffComposite.set(newToggle == buttonTiff);
        });

        HBox formatRow = new HBox(5, new Label("Composite format:"), buttonJpg, buttonTiff);


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
                SlideLoadWorkflow workflow = new SlideLoadWorkflow(dsName, tsName, serverGateway, useTiffComposite.get(), selectedDataStore);
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

        sidePanelTab.getChildren().addAll(buttonRow, formatRow, searchField,dsBox.getBox(), tsBox.getBox(), openImgBtn, statusLabel, tileOverlayCheckbox);

        return sidePanelTab;
    }

    private void updateSlideByDataset(ListViewerBox datasetBox, ListViewerBox slideBox){
        datasetBox.getListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null){
                    LoadSlideDataCommand loadSlideDataCommand = new LoadSlideDataCommand(serverGateway, newValue);
                    loadSlideDataCommand.build(); // Update tissue slide box
                    loadSlideDataCommand.setOnDone(() -> {
                        List<String> slides = loadSlideDataCommand.getTask().getValue();
                        slideBox.setItems(slides);
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
}
