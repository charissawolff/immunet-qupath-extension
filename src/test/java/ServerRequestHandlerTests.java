import org.computational_immunology.ext.ImmuNet.core.ServerRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.Tile;
import org.computational_immunology.ext.ImmuNet.core.Tile.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class ServerRequestHandlerTests {
    String exampleTilesJson = "[{\"id\": \"1\", \"code\": \"16,38\", \"width\": 900, \"height\": 700, \"dx\": 0.50, \"dy\": 0.50, \"x\": 10, \"y\": 0}, {\"id\": \"2\", \"code\": \"17,38\", \"width\": 900, \"height\": 700, \"dx\": 0.50, \"dy\": 0.50, \"x\": 11, \"y\": 0}]";
    String exampleTileJson = "{\"id\": \"1\", \"code\": \"16,38\", \"width\": 900, \"height\": 700, \"dx\": 0.50, \"dy\": 0.50, \"x\": 10, \"y\": 0}";

    @Test
    void jsonToTile() throws JSONException, IOException{
        Tile result = ServerRequestHandler.getInstance().testJsonToTile(new JSONObject(exampleTileJson), "path/", ImageType.THUMB);
        Assertions.assertEquals(1, result.getId());
        Assertions.assertEquals("path/16,38/thumb.jpg", result.getPath());
        Assertions.assertEquals("thumb", result.getType());
        Assertions.assertEquals("16,38", result.getCode());
        Assertions.assertEquals(10, result.getTileX());
        Assertions.assertEquals(0, result.getTileY());

        // Verify path handling
        result = ServerRequestHandler.getInstance().testJsonToTile(new JSONObject(exampleTileJson), "path", ImageType.THUMB);
        Assertions.assertEquals("path/16,38/thumb.jpg", result.getPath());
        result = ServerRequestHandler.getInstance().testJsonToTile(new JSONObject(exampleTileJson), "", ImageType.THUMB);
        Assertions.assertEquals("/16,38/thumb.jpg", result.getPath());

        // Verify image type handing
        result = ServerRequestHandler.getInstance().testJsonToTile(new JSONObject(exampleTileJson), "path", ImageType.COMPOSITE);
        Assertions.assertEquals("path/16,38/composite.jpg", result.getPath());

        // Verify empty JSONObject handling
        Assertions.assertThrows(JSONException.class, () -> 
            ServerRequestHandler.getInstance().testJsonToTile(new JSONObject(), "path", ImageType.COMPOSITE)
        );
    }

    @Test
    void jsonToTiles() throws JSONException, IOException, InterruptedException {
        JSONArray array = new JSONArray(exampleTilesJson);
        List<Tile> result = ServerRequestHandler.getInstance().testJsonToTiles(array, "path/", ImageType.THUMB);
        
        // Verify list size and contents
        Assertions.assertEquals(2, result.size());
        
        // Verify first tile
        Tile tile1 = result.get(0);
        Assertions.assertEquals(1, tile1.getId());
        Assertions.assertEquals("16,38", tile1.getCode());
        Assertions.assertEquals(10, tile1.getTileX());
        Assertions.assertEquals(0, tile1.getTileY());
        
        // Verify second tile
        Tile tile2 = result.get(1);
        Assertions.assertEquals(2, tile2.getId());
        Assertions.assertEquals("17,38", tile2.getCode());
        Assertions.assertEquals(11, tile2.getTileX());
        Assertions.assertEquals(0, tile2.getTileY());
        
        // Verify slideMap was populated correctly
        Assertions.assertEquals("16,38", ServerRequestHandler.getSlideMap().get(new ServerRequestHandler.Point(10, 0)));
        Assertions.assertEquals("17,38", ServerRequestHandler.getSlideMap().get(new ServerRequestHandler.Point(11, 0)));
    }

    @Test
    void jsonToTilesEmpty() throws JSONException, IOException, InterruptedException {
        JSONArray array = new JSONArray("[]");
        List<Tile> result = ServerRequestHandler.getInstance().testJsonToTiles(array, "path/", ImageType.THUMB);
        
        Assertions.assertEquals(0, result.size());
        Assertions.assertTrue(ServerRequestHandler.getSlideMap().isEmpty());
    }

    @Test
    void jsonToTilesMalformed() throws JSONException {
        JSONArray array = new JSONArray("[{\"id\": \"invalid\"}]");
        
        Assertions.assertThrows(JSONException.class, () -> 
            ServerRequestHandler.getInstance().testJsonToTiles(array, "path/", ImageType.THUMB)
        );
    }
}