package org.computational_immunology.ext.ImmuNet;

import org.computational_immunology.ext.ImmuNet.core.handlers.ServerUploadGateway;

import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerConnectionHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerGateway;
import org.computational_immunology.ext.ImmuNet.core.store.SelectedDataStore;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverController;
import org.computational_immunology.ext.ImmuNet.ui.TileHoverOverlay;
import org.computational_immunology.ext.ImmuNet.ui.listeners.PolygonMetadataAdder;
import org.computational_immunology.ext.ImmuNet.ui.listeners.PolygonTracker;
import org.computational_immunology.ext.ImmuNet.ui.tabs.DatasetSelectorTab;
import org.computational_immunology.ext.ImmuNet.ui.tabs.EnableExtensionCheckbox;
import org.computational_immunology.ext.ImmuNet.ui.tabs.PolygonViewerTab;
import org.computational_immunology.ext.ImmuNet.ui.tabs.ServerConnectionTab;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import qupath.ext.template.ui.InterfaceController;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.prefs.controlsfx.PropertyItemBuilder;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.prefs.PathPrefs;

public class ImmuNetExtension implements QuPathExtension {
    private static final String EXTENSION_NAME = "ImmuNet Extension";
    private static final String EXTENSION_DESCRIPTION = "Extension for accessing and editing bioimage-related data from the Immunology Department.";
    private static final BooleanProperty enableExtensionProperty = PathPrefs.createPersistentPreference(
			"enableExtension", true);
    private Stage enableExtensionStage;


    @Override
    public void installExtension(QuPathGUI qupath) {
        addPreferenceToPane(qupath);
        addMenuItem(qupath);
        //qupath.getMenu("ImmuNet", true); // Add new tab to top menu bar

        // fill the detections
        //qupath.getOverlayOptions().setFillDetections(true);

        // Built once here and injected down, this will be used to retrieve data and images from the server
        ServerGateway serverGateway = new ServerGateway(ServerConnectionHandler.getInstance());
        ServerUploadGateway jsonDataUploadHandler = new ServerUploadGateway(ServerConnectionHandler.getInstance());

        // Built once and injected down. THis tracks the currently loaded slide and the currently
        // selected tile, and wires mouse hover/click on the viewer to the tile highlight overlay.
        SelectedDataStore selectedDataStore = new SelectedDataStore();
        TileHoverOverlay tileHoverOverlay = new TileHoverOverlay(qupath.getOverlayOptions(), selectedDataStore);
        TileHoverController tileHoverController = new TileHoverController(selectedDataStore, tileHoverOverlay, enableExtensionProperty);

        // Side bar
        ServerConnectionTab serverConnectionTab = new ServerConnectionTab();
        gateTab(serverConnectionTab.addCustomTab(qupath.getAnalysisTabPane()));

        DatasetSelectorTab datasetTab = new DatasetSelectorTab(serverGateway, selectedDataStore, tileHoverController);
        gateTab(datasetTab.addCustomTab(qupath.getAnalysisTabPane()));


            //polygon tracker listener
        PolygonTracker polygonTracker = new PolygonTracker(enableExtensionProperty);
        // polygon metadata added
        PolygonMetadataAdder polygonMetadataAdder = new PolygonMetadataAdder(polygonTracker, selectedDataStore);
        //polygon viewer tab
        PolygonViewerTab polygonViewerTab = new PolygonViewerTab(serverGateway, jsonDataUploadHandler, selectedDataStore, polygonTracker);
        gateTab(polygonViewerTab.addCustomTab(qupath.getAnalysisTabPane()));
    }

    private void gateTab(Tab tab) {
        // gate tab in that we make sure it's only clickable when the extension is enabled
        tab.disableProperty().bind(enableExtensionProperty.not());
    }

    private void addPreferenceToPane(QuPathGUI qupath) {
        var propertyItem = new PropertyItemBuilder<>(enableExtensionProperty, Boolean.class)
				.name("Enable extension")
				.category("ImmuNet Extension")
				.description("Enable ImmuNet extension")
				.build();
		qupath.getPreferencePane()
				.getPropertySheet()
				.getItems()
				.add(propertyItem);
	}

    private void addMenuItem(QuPathGUI qupath) {
		var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
		MenuItem menuItem = new MenuItem("Enable/Disable ImmuNet Extension");
		menuItem.setOnAction(e -> createEnableExtensionStage());
		///menuItem.disableProperty().bind(enableExtensionProperty.not());
		menu.getItems().add(menuItem);
	}

    private void createEnableExtensionStage() {
        if (enableExtensionStage == null) {
            enableExtensionStage = new Stage();
            Scene scene = new Scene(EnableExtensionCheckbox.getInstance());
            enableExtensionStage.initOwner(QuPathGUI.getInstance().getStage());
            enableExtensionStage.setTitle("Enable ImmuNet Extension");
            enableExtensionStage.setScene(scene);
            enableExtensionStage.setResizable(false);
            enableExtensionStage.setMinWidth(320);
            enableExtensionStage.setMinHeight(120);
        }
        enableExtensionStage.show();
        enableExtensionStage.toFront();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public String getDescription() {
        return EXTENSION_DESCRIPTION;
    }

    public static BooleanProperty enableExtensionProperty() {
        return enableExtensionProperty;
    }
}
