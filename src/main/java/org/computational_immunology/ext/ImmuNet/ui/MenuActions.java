package org.computational_immunology.ext.ImmuNet.ui;

import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.handlers.ServerConnectionHandler;

public class MenuActions {
    public static void connectToServer(String username, String hostname, String password, String dbuser, String dbpass) throws Exception {
        ServerConnectionHandler.getInstance().startSSHThread(username, hostname, password);
        ServerConnectionHandler.getInstance().performDatabaseLogin(dbuser,dbpass);
    }
    //todo: figure out what this is for
    public static void updateListViewerBox(ListViewerBox box, List<String> list){
        box.setItems(list);
    }

    private MenuActions() {
        /* This utility class should not be instantiated */
    }

}
