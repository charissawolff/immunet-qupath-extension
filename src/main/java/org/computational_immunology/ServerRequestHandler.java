package org.computational_immunology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.computational_immunology.Tile.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequestHandler {
    private static final ServerRequestHandler INSTANCE = new ServerRequestHandler();

    public record Point(double x, double y) {}

    static Map<Point, String> slideMap;

    private ServerRequestHandler() {}

    public static ServerRequestHandler getInstance() {
        return INSTANCE;
    }

    // Get list of tiles belonging to a slide
    public static List<Tile> getAllTiles(String datasetName, String slideName) throws IOException, InterruptedException{
        String path = "v/datasets/" + datasetName + '/' + slideName + '/';
        List<Tile> tiles;
        String allTilesJson = ServerConnectionHandler.getInstance().fetchStringPage(path).body();

        ImmuNetLog.log("Path at getAllTiles is " + path);
        ImmuNetLog.log(allTilesJson);

        JSONArray parsedOutput = new JSONArray(allTilesJson);
        ImmuNetLog.log("Fetched all tiles json");
        tiles = jsonToTiles(parsedOutput, path, ImageType.THUMB);
        return tiles;
    }

    // Convert json to list of tiles
    private static List<Tile> jsonToTiles(JSONArray array, String path, ImageType type) throws IOException, InterruptedException {
        Map<Point, String> coordsToCode = new HashMap<>();
        List<Tile> tiles = new ArrayList<>();
        ImmuNetLog.log("converting json to tiles");
        for (int i = 0; i < array.length(); i++){
                JSONObject tile = array.getJSONObject(i);
                tiles.add(jsonToTile(tile, path, type));
                Point tileCoord = new Point(tile.getDouble("x"), tile.getDouble("y"));
                coordsToCode.put(tileCoord, tile.getString("code")); // map coordinates of a tile to its code
        }
        slideMap = coordsToCode;
        ImmuNetLog.log("tiles converted");
        return tiles;
    }

    public List<Tile> testJsonToTiles(JSONArray array, String path, ImageType type) throws IOException, InterruptedException {
        return jsonToTiles(array, path, type);
    }

    private static Tile jsonToTile(JSONObject json, String path, ImageType type) throws IOException, JSONException{
        return new Tile(
                json.getInt("id"),
                path,
                type,
                json.getString("code"),
                json.getDouble("x"), 
                json.getDouble("y"),
                json.getDouble("width"),
                json.getDouble("height")
        );
    }

    public Tile testJsonToTile(JSONObject json, String path, ImageType type) throws JSONException, IOException{
        return jsonToTile(json, path, type);
    }

    /**
     * Fetch a list of format JSON from v/{webpageString} and converts to List of type String.
     * 
     * @param localPath path to webpage
     * @return list of type String obtained from JSON webpage OR empty list on exception
     */
    public static List<String> getWebpageAsList(String localPath){
        List<String> datasetList = new ArrayList<>();
        try {
            String json = ServerConnectionHandler.getInstance().fetchStringPage("v/" + localPath).body();
            JSONArray parsedDatasetList = new JSONArray(json);

            // Add each element of the JSONArray to the list to convert types
            for(int i = 0; i < parsedDatasetList.length(); i++){
                datasetList.add(parsedDatasetList.getString(i));
            }
        } catch (Exception e) {
            ImmuNetLog.error("Error in fetching webpage list of items. Localpath: " + localPath, e);
        } 
        return datasetList;
    }

    public static Map<Point, String> getSlideMap(){
        return slideMap;
    }

}
