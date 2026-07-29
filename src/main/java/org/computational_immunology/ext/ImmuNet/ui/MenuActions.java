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

    // setStreamedServer/createSparseImageServer disabled: depended on ServerRequestHandler
    // and Tile's old self-fetching image. Will be replaced by SelectSlideCommand once
    // ImageRequestHandler can fetch tile images (Step 2/3) and the slide-server loader exists.
    //
    // public static void setStreamedServer(String datasetName, String slideName) {
    //     try {
    //         List<Tile> tiles = ServerRequestHandler.getAllTiles(
    //                 datasetName, slideName
    //         );
    //         try {
    //             SparseImageServer server = createSparseImageServer(tiles);
    //             QuPathGUI.getInstance().getViewer().setImageData(new ImageData<>(server));
    //             ImmuNetLog.log("Successfully set Image Data.");
    //         } catch (IOException e) {
    //             ImmuNetLog.error("Could not set image data",e);
    //         }
    //     } catch (IOException | InterruptedException e) {
    //         ImmuNetLog.error("Could not fetch tiles. Are you connected to the server?",e);
    //     }
    // }
    //
    // private static SparseImageServer createSparseImageServer(List<Tile> tiles) throws IOException, InterruptedException {
    //     SparseImageServer.Builder builder = new SparseImageServer.Builder();
    //
    //     for (var tile : tiles) {
    //         StreamedImageServer tileServer = new StreamedImageServer(tile);
    //         ImageRegion region = ImageRegion.createInstance(
    //                 (int)tile.getTileX(),
    //                 (int)tile.getTileY(),
    //                 (int)tile.tileW, (int)tile.tileH, 1,0
    //         );
    //         ImmuNetLog.log("serverRegion add: ({}), ({}), ({}, {})", tileServer, region, tile.tileW, tile.tileH);
    //         builder.serverRegion(region, 1, tileServer);
    //     }
    //
    //     return builder.build();
    // }

    private MenuActions() {
        /* This utility class should not be instantiated */
    }

}
