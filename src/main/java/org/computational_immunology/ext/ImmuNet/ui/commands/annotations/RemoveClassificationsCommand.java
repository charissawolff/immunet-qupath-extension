package org.computational_immunology.ext.ImmuNet.ui.commands.annotations;

import qupath.lib.gui.QuPathGUI;

public class RemoveClassificationsCommand {
    private RemoveClassificationsCommand() {
        // Private constructor to prevent instantiation
    }

    public static void execute(){
    QuPathGUI.getInstance().getAvailablePathClasses().clear();

    }
    
}
