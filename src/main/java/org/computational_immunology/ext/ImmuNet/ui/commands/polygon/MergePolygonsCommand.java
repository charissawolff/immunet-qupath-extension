package org.computational_immunology.ext.ImmuNet.ui.commands.polygon;

import java.util.List;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjectTools;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

public class MergePolygonsCommand {
    private final List<PathObject> polygons;

    public MergePolygonsCommand(List<PathObject> polygons) {
        this.polygons = polygons;
    }

    public void execute() {
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
        PathObject mergedPolygon = PathObjectTools.mergeObjects(polygons);
        //now remove old polygons from the hierarchy and add the merged polygon
        hierarchy.removeObjects(polygons, false);
        hierarchy.addObject(mergedPolygon);
    }
    
}
