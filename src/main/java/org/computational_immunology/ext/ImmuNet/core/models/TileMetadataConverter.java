package org.computational_immunology.ext.ImmuNet.core.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TileMetadataConverter {

    private TileMetadataConverter() {
        // Private constructor to prevent instantiation
    }
    // Convert json to list of tiles
    public static List<TileMetadata> jsonToTileMetadatas(JSONArray array, ImageType type) throws IOException, InterruptedException {
        List<TileMetadata> tileMetadatas = new ArrayList<>();
        ImmuNetLog.log("converting json to tiles");
        for (int i = 0; i < array.length(); i++) {
            JSONObject tile = array.getJSONObject(i);
            tileMetadatas.add(jsonToTileMetadata(tile, type));
        }
        ImmuNetLog.log("tiles converted");
        return tileMetadatas;
    }

    public static TileMetadata jsonToTileMetadata(JSONObject json, ImageType type) throws IOException, JSONException {
        return new TileMetadata(
                json.getInt("id"),
                json.getString("code"),
                type,
                json.getDouble("x"),
                json.getDouble("y"),
                json.getDouble("width"),
                json.getDouble("height")
        );
    }
}
