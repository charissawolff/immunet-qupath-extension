package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.HashMap;

import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverController;
import org.computational_immunology.ext.ImmuNet.ui.commands.ClearImageViewerCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.SlideLoadWorkflow;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.HashMap;
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

        
        ObservableList<String> items = FXCollections.observableArrayList("Dataset A", "Dataset B", "Dataset C");

        Map<String, BooleanProperty> checkedMap = new HashMap<>();

        ListView<String> listView = new ListView<>(items);
        listView.setCellFactory(CheckBoxListCell.forListView(
                item -> checkedMap.computeIfAbsent(item, k -> new SimpleBooleanProperty(false))
        ));
        Button loadDataBtn = makeButton("Load Datasets", new Dimensions(40, 120));
        loadDataBtn.setOnAction(e -> ImmuNetLog.log("Load Datasets button clicked"));
        // checkbox to show or not the polygons
        CheckBox c = new CheckBox("Show polygons");
        c.setOnAction(e -> ImmuNetLog.log("Show polygons checkbox clicked"));

        sidePanelTab.getChildren().addAll(loadDataBtn,c,listView);

        return sidePanelTab;
    }
}