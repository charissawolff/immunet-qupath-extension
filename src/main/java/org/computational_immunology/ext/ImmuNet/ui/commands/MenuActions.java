package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.ui.tabs.ListViewerBox;

public class MenuActions {
    //todo: figure out what this is for
    public static void updateListViewerBox(ListViewerBox box, List<String> list){
        box.setItems(list);
    }

    private MenuActions() {
        /* This utility class should not be instantiated */
    }

}
