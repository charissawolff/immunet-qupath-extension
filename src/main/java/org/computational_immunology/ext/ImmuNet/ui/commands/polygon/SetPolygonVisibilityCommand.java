package org.computational_immunology.ext.ImmuNet.ui.commands.polygon;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.OverlayOptions;
import qupath.lib.objects.classes.PathClass;

public class SetPolygonVisibilityCommand {

    private final String polygonId;
    private final boolean visible;

    public SetPolygonVisibilityCommand(String polygonId, boolean visible) {
        this.polygonId = polygonId;
        this.visible = visible;
    }

    public void execute() {
        OverlayOptions overlayOptions = QuPathGUI.getInstance().getOverlayOptions();
        if (overlayOptions == null) {
            ImmuNetLog.error("No overlay options available, cannot toggle polygon visibility for id: " + polygonId);
            return;
        }
        overlayOptions.setPathClassHidden(PathClass.getInstance(polygonId), !visible);
    }
}