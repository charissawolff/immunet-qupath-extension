package org.computational_immunology.ext.ImmuNet.ui.commands;

import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.gui.QuPathGUI;

import java.util.Collection;
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
        PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
        Collection<PathObject> viewerPathObjects = hierarchy.getAnnotationObjects();
        for (PathObject viewObject : viewerPathObjects){
            if (viewObject.getMetadata().get("id") != null &&
                pathObjects.stream().anyMatch(p -> viewObject.getMetadata().get("id").equals(p.getMetadata().get("id")))) {
                viewer.getImageData().getHierarchy().removeObject(viewObject, false);
            }
        }
        viewer.getImageData().getHierarchy().addObjects(pathObjects);
    }
}