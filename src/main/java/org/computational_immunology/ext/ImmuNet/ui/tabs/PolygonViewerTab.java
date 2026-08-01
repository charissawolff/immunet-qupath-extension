package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.HashMap;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.Polygon;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.ui.commands.LoadPolygonDataCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.SetPolygonVisibilityCommand;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import javafx.scene.control.ListView;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.util.StringConverter;

import java.util.Map;

public class PolygonViewerTab extends CustomSidePanelTab {

    private final AnnotationRequestHandler annotationRequestHandler;
    private final SelectedDataStore selectedDataStore;

    public PolygonViewerTab(AnnotationRequestHandler annotationRequestHandler,
                               SelectedDataStore selectedDataStore) {
        super("Polygon viewer");
        this.annotationRequestHandler = annotationRequestHandler;
        this.selectedDataStore = selectedDataStore;
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

        ListView<Polygon> listView = new ListView<>(polygonNames);
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
        LoadPolygonDataCommand loadPolygonDataCommand = new LoadPolygonDataCommand(annotationRequestHandler, selectedDataStore);
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

        Button addDataBtn = makeButton("Add polygon", new Dimensions(40, 120));
        addDataBtn.setOnAction(e -> {
            ImmuNetLog.log("Add polygon button clicked");
        });




        sidePanelTab.getChildren().addAll(loadDataBtn,statusLabel,c,listView);

        return sidePanelTab;
    }
}