package org.computational_immunology.ext.ImmuNet.ui.tabs;

import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerUploadGateway;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.models.Dimensions;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.LoadPolygonCommand;
import org.computational_immunology.ext.ImmuNet.ui.commands.polygon.MergePolygonsCommand;
import org.computational_immunology.ext.ImmuNet.ui.listeners.PolygonTracker;
import org.computational_immunology.ext.ImmuNet.ui.tabBoxes.NewPolygonViewerBox;
import org.computational_immunology.ext.ImmuNet.ui.tabBoxes.PolygonListBox;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;
import qupath.lib.objects.PathObject;


public class PolygonViewerTab extends CustomSidePanelTab {

    private final ServerGateway serverGateway;
    private final ServerUploadGateway dataUploadHandler;
    private final SelectedDataStore selectedDataStore;
    private static PolygonTracker polygonTracker;

    public PolygonViewerTab(ServerGateway serverGateway,
                            ServerUploadGateway dataUploadHandler,
                            SelectedDataStore selectedDataStore, PolygonTracker polygonTracker) {
        super("Polygon viewer");
        this.serverGateway = serverGateway;
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

        PolygonListBox polygonListBox = new PolygonListBox();

        Button loadDataBtn = makeButton("Load polygons", new Dimensions(30, 120));
        Label statusLabel = new Label();

        // bind to the selected slide property of the datastore, so that the button is only enabled when a slide is selected
        // this continues working after the slide is cleaered, because when we click this button, we also
        // clear the data from the datastore, which in turn enables the button again
        loadDataBtn.disableProperty().bind(Bindings.isNull(selectedDataStore.selectedSlideProperty()));

        loadDataBtn.setOnAction(e -> {
        LoadPolygonCommand loadPolygonDataCommand = new LoadPolygonCommand(serverGateway, selectedDataStore);
        loadPolygonDataCommand.build();
            if (selectedDataStore.getSelectedSlide() == null) {
                ImmuNetLog.log("No slide selected, cannot load polygons");
                statusLabel.setText("No slide selected, cannot load polygons");
                return;
            }
            ImmuNetLog.log("Load polygons button clicked");
            polygonListBox.clear();
            loadPolygonDataCommand.start();
            
            statusLabel.setText("Loading polygons...");
             // refresh the list right after loading
            loadPolygonDataCommand.setOnDone(() -> {
                //if there is no polygons, clear the list and update status label
                if (selectedDataStore.getPolygons().isEmpty()) {
                    polygonListBox.clear();
                    ImmuNetLog.log("No polygons found");
                    statusLabel.setText("No polygons found");
                    return;
                }
                statusLabel.setText("Polygons loaded: " + selectedDataStore.getPolygons().size());
                polygonListBox.setPolygons(selectedDataStore.getPolygons());
            });

            loadPolygonDataCommand.setOnFailed(() -> {
                ImmuNetLog.error("Failed to load polygons");
                statusLabel.setText("Failed to load polygons");
            });
        });


        ObservableList<PathObject> userAddedPolygons = polygonTracker.getNewAnnotations();
        //bind the userAddedPolygons list to the polygonTracker's newAnnotations list, so that any 
        // new polygons added by the user are automatically added to the list view

        // add the user added polygons
        NewPolygonViewerBox newPolygonViewerBox = new NewPolygonViewerBox(userAddedPolygons, dataUploadHandler, selectedDataStore);
        VBox.setMargin(newPolygonViewerBox, new Insets(2, 2, 10, 2)); // Space between list and buttons
        VBox.setVgrow(newPolygonViewerBox, Priority.ALWAYS);

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

        sidePanelTab.getChildren().addAll(loadDataBtn,statusLabel,polygonListBox, newPolygonViewerBox, mergeBtn);

        return sidePanelTab;
    }
}