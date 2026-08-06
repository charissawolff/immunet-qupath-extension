package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Polygon;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.JsonDataUploadHandler;
import org.computational_immunology.ext.ImmuNet.ui.commands.SelectPathObjectCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.LoadPolygonCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.MergePolygonsCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.SetPolygonVisibilityCommand;
import org.computational_immunology.ext.ImmuNet.ui.listeners.PolygonTracker;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.util.StringConverter;
import qupath.lib.objects.PathObject;

import java.util.Map;

public class PolygonViewerTab extends CustomSidePanelTab {

    private final AnnotationRequestHandler annotationRequestHandler;
    private final JsonDataUploadHandler dataUploadHandler;
    private final SelectedDataStore selectedDataStore;
    private static PolygonTracker polygonTracker;

    public PolygonViewerTab(AnnotationRequestHandler annotationRequestHandler,
                            JsonDataUploadHandler dataUploadHandler,
                            SelectedDataStore selectedDataStore, PolygonTracker polygonTracker) {
        super("Polygon viewer");
        this.annotationRequestHandler = annotationRequestHandler;
        this.dataUploadHandler = dataUploadHandler;
        this.selectedDataStore = selectedDataStore;
        PolygonViewerTab.polygonTracker = polygonTracker;
    }

     /**
     * Creates a panel with a button to load the polygons, a cross button to view or not, and a box with individual polygons to
     * view or not
     */
    @Override
    public VBox getContent(){

        VBox sidePanelTab = new VBox();
        sidePanelTab.setPadding(new Insets(10, 10, 10, 10)); // Box margins
        sidePanelTab.setSpacing(5); // Space between buttons and boxes
        
        ObservableList<Polygon> polygonNames = FXCollections.observableArrayList();
        Map<String, BooleanProperty> checkedMap = new HashMap<>();


        Label listViewTitle = new Label("Polygon list");
        listViewTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        ListView<Polygon> listView = new ListView<>(polygonNames);
        listView.setPrefHeight(150);
        VBox.setMargin(listView, new Insets(1, 2, 5, 2)); // Space between list and buttons

        listView.setCellFactory(CheckBoxListCell.forListView(
                p -> checkedMap.computeIfAbsent(p.getId(), id -> {
                    BooleanProperty visible = new SimpleBooleanProperty(true); // checked = visible, matches "Show polygons" label
                    visible.addListener((obs, was, isVisible) -> new SetPolygonVisibilityCommand(id, isVisible).execute());
                    return visible;
                }),
                // need a string converter to display the polygon name in the list view
                new StringConverter<Polygon>() {
                    @Override
                    public String toString(Polygon p) {
                        return p.getDisplayedName();
                    }

                    @Override
                    public Polygon fromString(String s) {
                        return null; // list is display-only, never edited back from text
                    }
                }
        ));
        Button loadDataBtn = makeButton("Load polygons", new Dimensions(40, 120));
        Label statusLabel = new Label();

        // bind to the selected slide property of the datastore, so that the button is only enabled when a slide is selected
        // this continues working after the slide is cleaered, because when we click this button, we also
        // clear the data from the datastore, which in turn enables the button again
        loadDataBtn.disableProperty().bind(Bindings.isNull(selectedDataStore.selectedSlideProperty()));


        loadDataBtn.setOnAction(e -> {
        LoadPolygonCommand loadPolygonDataCommand = new LoadPolygonCommand(annotationRequestHandler, selectedDataStore);
        loadPolygonDataCommand.build();
            if (selectedDataStore.getSelectedSlide() == null) {
                ImmuNetLog.log("No slide selected, cannot load polygons");
                statusLabel.setText("No slide selected, cannot load polygons");
                return;
            }
            ImmuNetLog.log("Load polygons button clicked");
            polygonNames.clear();
            checkedMap.clear();
            loadPolygonDataCommand.start();
            
            statusLabel.setText("Loading polygons...");
             // refresh the list right after loading
            loadPolygonDataCommand.setOnDone(() -> {
                //if there is no polygons, clear the list and update status label
                if (selectedDataStore.getPolygons().isEmpty()) {
                    polygonNames.clear();
                    checkedMap.clear();
                    ImmuNetLog.log("No polygons found");
                    statusLabel.setText("No polygons found");
                    return;
                }
                statusLabel.setText("Polygons loaded: " + selectedDataStore.getPolygons().size());
                polygonNames.setAll(
                        selectedDataStore.getPolygons()
                );
            });

            loadPolygonDataCommand.setOnFailed(() -> {
                ImmuNetLog.error("Failed to load polygons");
                statusLabel.setText("Failed to load polygons");
            });
        });
    

        // checkbox to show or not the polygons
        CheckBox c = new CheckBox("Show polygons");
        c.setSelected(true); // matches the default-visible state of each per-item checkbox
        c.setOnAction(e -> {
            ImmuNetLog.log("Show polygons checkbox clicked");
            checkedMap.forEach((id, visible) -> {
                visible.set(c.isSelected());
                new SetPolygonVisibilityCommand(id, c.isSelected()).execute();
            });
        });

        ObservableList<PathObject> userAddedPolygons = polygonTracker.getNewAnnotations();
        //bind the userAddedPolygons list to the polygonTracker's newAnnotations list, so that any new polygons added by the user are automatically added to the list view

        //add Title to the list view
        Label newPolygonListTitle = new Label("User added polygons");
        newPolygonListTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        NewPolygonViewerBox newPolygonViewerBox = new NewPolygonViewerBox(userAddedPolygons, dataUploadHandler);
        newPolygonViewerBox.setPrefHeight(250);
        VBox.setMargin(newPolygonListTitle, new Insets(10, 2, 0, 2)); // Space between list and buttons
        VBox.setMargin(newPolygonViewerBox, new Insets(2, 2, 10, 2)); // Space between list and buttons
        VBox.setVgrow(newPolygonViewerBox, Priority.ALWAYS);
        newPolygonViewerBox.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        newPolygonViewerBox.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            //what the user selects in the list view
            SelectPathObjectCommand selectAnnotationCommand = new SelectPathObjectCommand(newSel);
            selectAnnotationCommand.execute();
        });    
        


        Button mergeBtn = makeButton("Merge selected polygons", new Dimensions(20, 180));
        mergeBtn.disableProperty().bind(Bindings.createBooleanBinding(() -> {
                var selected = newPolygonViewerBox.getSelectionModel().getSelectedItems();
                return selected.size() < 2 || selected.stream().anyMatch(PathObject::isLocked);
            },
            newPolygonViewerBox.getSelectionModel().getSelectedItems()
        ));

        mergeBtn.setOnAction(e -> {
            List<PathObject> selectedPolygons = new ArrayList<>(newPolygonViewerBox.getSelectionModel().getSelectedItems());
            MergePolygonsCommand mergePolygonsCommand = new MergePolygonsCommand(selectedPolygons);
            mergePolygonsCommand.execute();
        });

        sidePanelTab.getChildren().addAll(loadDataBtn,statusLabel,listViewTitle,c,listView, newPolygonListTitle, newPolygonViewerBox, mergeBtn, mergePolygonsLabel);

        return sidePanelTab;
    }
}