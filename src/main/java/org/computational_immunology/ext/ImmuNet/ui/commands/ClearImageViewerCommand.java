package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;

/**
 * Clears the currently displayed slide from the active viewer and resets the
 * store's selected slide, so a stale click can't be attributed to the old tile.
 */
public class ClearImageViewerCommand {

    private final SelectedDataStore selectedDataStore;

    public ClearImageViewerCommand(SelectedDataStore selectedDataStore) {
        this.selectedDataStore = selectedDataStore;
    }

    public void execute() {
        QuPathViewer viewer = QuPathGUI.getInstance().getViewer();
        if (viewer != null) {
            viewer.resetImageData();
        }
        selectedDataStore.setSelectedSlide(null);
    }
}
