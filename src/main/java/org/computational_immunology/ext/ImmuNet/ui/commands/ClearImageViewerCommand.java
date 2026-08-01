package org.computational_immunology.ext.ImmuNet.ui.commands;

import org.computational_immunology.ext.ImmuNet.core.SelectedDataStore;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;

/**
 * Clears the currently displayed slide from the active viewer and resets the
 * selection state of the store so in order to not hace clicking be on the old tile
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