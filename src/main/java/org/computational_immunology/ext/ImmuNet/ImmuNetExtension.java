package org.computational_immunology.ext.ImmuNet;

import org.computational_immunology.ext.ImmuNet.core.handlers.AnnotationRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ImageRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerConnectionHandler;
import org.computational_immunology.ext.ImmuNet.ui.DatasetSelectorTab;
import org.computational_immunology.ext.ImmuNet.ui.ServerConnectionTab;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.prefs.PathPrefs;

public class ImmuNetExtension implements QuPathExtension {

    // make the point radius larger 
    // This is a global QuPath preference, not per-object.
    private static final int ANNOTATION_POINT_RADIUS = 8;

    @Override
    public void installExtension(QuPathGUI qupath) {
        qupath.getMenu("ImmuNet", true); // Add new tab to top menu bar

        PathPrefs.pointRadiusProperty().set(ANNOTATION_POINT_RADIUS);
        // fill the detections
        //qupath.getOverlayOptions().setFillDetections(true);

        // Built once here and injected down, this will be used to retrieve specifically tile images from the server
        ImageRequestHandler imageRequestHandler = new ImageRequestHandler(ServerConnectionHandler.getInstance());
        AnnotationRequestHandler annotationRequestHandler = new AnnotationRequestHandler(ServerConnectionHandler.getInstance());

        // Side bar
        ServerConnectionTab serverConnectionTab = new ServerConnectionTab();
        serverConnectionTab.addCustomTab(qupath.getAnalysisTabPane());

        DatasetSelectorTab datasetTab = new DatasetSelectorTab(imageRequestHandler, annotationRequestHandler);
        datasetTab.addCustomTab(qupath.getAnalysisTabPane());
    }

    @Override
    public String getName() {
        return "ImmuNet Extension";
    }

    @Override
    public String getDescription() {
        return "Extension adding connection to Immunology Department.";
    }
}
