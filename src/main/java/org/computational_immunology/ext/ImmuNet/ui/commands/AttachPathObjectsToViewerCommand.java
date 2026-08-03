package org.computational_immunology.ext.ImmuNet.ui.commands;

import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.gui.QuPathGUI;

import java.util.List;

public class AttachPathObjectsToViewerCommand {
    private final List<PathObject> pathObjects;

    public AttachPathObjectsToViewerCommand(List<PathObject> pathObjects) {
        this.pathObjects = pathObjects;
    }

    public void execute() {
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer == null || viewer.getImageData() == null) {
            return;
        }
        viewer.getImageData().getHierarchy().addObjects(pathObjects);
    }
}