package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverController;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.ClearImageViewerCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector.LoadDatasetsCommand;
import org.computational_immunology.ext.ImmuNet.ui.controls.SlideOpenerControl;
import org.computational_immunology.ext.ImmuNet.ui.tabBoxes.DataListViewerBox;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

public class DatasetSelectorTab extends CustomSidePanelTab {

    private final ServerGateway serverGateway;
    private final TileHoverController tileHoverController;
    private final SelectedDataStore selectedDataStore;

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

        DataListViewerBox dataListViewerBox = new DataListViewerBox(serverGateway, 250 , (int)sidePanelTab.getMaxWidth());

        //two buttons next to each other, one for loading datasets and one for clearing the current selection from viewer
        Button loadDataBtn = makeButton("Load Datasets", new Dimensions(30, 120));
        loadDataBtn.setOnAction(e -> {
            LoadDatasetsCommand loadDatasetCommand = new LoadDatasetsCommand(serverGateway);
            loadDatasetCommand.build();
            loadDatasetCommand.setOnDone(() -> {
                List<String> datasets = loadDatasetCommand.getTask().getValue();
                dataListViewerBox.setDatasets(datasets);
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
        clearSelectionBtn.disableProperty().bind(Bindings.isNull(selectedDataStore.selectedSlideProperty()));

        BorderPane buttonRow = new BorderPane();
        buttonRow.setLeft(loadDataBtn);
        buttonRow.setRight(clearSelectionBtn);


        //a toggle radio button for jpg or .tiff composite
        ToggleGroup RadioButtongroup = new ToggleGroup();
        RadioButton buttonJpg = new RadioButton(".jpg");
        RadioButton buttonTiff = new RadioButton(".tiff");
        HBox formatRow = new HBox(5, new Label("Composite format:"), buttonJpg, buttonTiff);
        buttonJpg.setSelected(true); // default to jpg
        buttonJpg.setToggleGroup(RadioButtongroup);
        buttonTiff.setToggleGroup(RadioButtongroup);

        SimpleBooleanProperty useTiffComposite = new SimpleBooleanProperty(false);
        RadioButtongroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            useTiffComposite.set(newToggle == buttonTiff);
        });

        // the load image button
        SlideOpenerControl slideOpenerControl = new SlideOpenerControl(serverGateway, 
                selectedDataStore, tileHoverController, dataListViewerBox, useTiffComposite::get);

        sidePanelTab.getChildren().addAll(buttonRow, formatRow, dataListViewerBox, slideOpenerControl, tileOverlayCheckbox);

        return sidePanelTab;
    }
    
}
