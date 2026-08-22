package org.computational_immunology.ext.ImmuNet.ui.commands.dataSelector;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;

/**
 * Clears the currently displayed slide from the active viewer.
 */
public class ClearImageViewerCommand {


    private ClearImageViewerCommand() {
        /*Souldn't be initialized */
    }

    public static void execute() {
        //check if there are multiple viewers open
        List<QuPathViewer> viewers = QuPathGUI.getInstance().getAllViewers();
        if (viewers.size() > 1) {
            ImmuNetLog.log("Multiple viewers open, clearing the first open viewer");
        }
        ImmuNetLog.log("Clearing single viewer");
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer != null) {
            viewer.resetImageData();
        }
    }
}
