package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.Collections;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class SelectPathObjectCommand {
    private final PathObject selectedObject;

    public SelectPathObjectCommand(PathObject selectedObject) {
        this.selectedObject = selectedObject;
    }

    public void execute() {
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer == null || viewer.getImageData() == null) {
            ImmuNetLog.error("No image open, cannot select annotation: " + selectedObject);
            return;
        }
        PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
        if (selectedObject == null) {
            //clear the selection if no object is selected
            hierarchy.getSelectionModel().setSelectedObjects(Collections.emptyList(), null);
            return;
        }
        List<PathObject> selectedSet = Collections.singletonList(selectedObject);
        hierarchy.getSelectionModel().setSelectedObjects(selectedSet, selectedObject);
    }
}
