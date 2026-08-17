import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;

import org.computational_immunology.ext.ImmuNet.core.api.ImageRequestHandler;
import org.computational_immunology.ext.ImmuNet.core.api.PageFetcher;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Covers the JSON to TileMetadata conversion.
 */
public class ImageRequestHandlerTests {

    private static final PageFetcher NO_FETCHING = new PageFetcher() {
        @Override
        public HttpResponse<InputStream> fetchPage(String localPath) {
            throw new UnsupportedOperationException("JSON conversion must not fetch anything");
        }

        @Override
        public HttpResponse<String> fetchStringPage(String localPath) {
            throw new UnsupportedOperationException("JSON conversion must not fetch anything");
        }
    };

    private final ImageRequestHandler handler = new ImageRequestHandler(NO_FETCHING);

    String exampleTilesJson = "[{\"id\": \"1\", \"code\": \"16,38\", \"width\": 900, \"height\": 700, \"dx\": 0.50, \"dy\": 0.50, \"x\": 10, \"y\": 0}, {\"id\": \"2\", \"code\": \"17,38\", \"width\": 900, \"height\": 700, \"dx\": 0.50, \"dy\": 0.50, \"x\": 11, \"y\": 0}]";
    String exampleTileJson = "{\"id\": \"1\", \"code\": \"16,38\", \"width\": 900, \"height\": 700, \"dx\": 0.50, \"dy\": 0.50, \"x\": 10, \"y\": 0}";

    @Test
    void jsonToTileMetadata() throws Exception {
        TileMetadata result = handler.testJsonToTileMetadata(new JSONObject(exampleTileJson), ImageType.THUMB);

        Assertions.assertEquals(1, result.getId());
        Assertions.assertEquals("16,38", result.getCode());
        Assertions.assertEquals(ImageType.THUMB, result.getType());
        Assertions.assertEquals(10.0, result.getX());
        Assertions.assertEquals(0.0, result.getY());
        Assertions.assertEquals(900.0, result.getWidth());
        Assertions.assertEquals(700.0, result.getHeight());
    }

    @Test
    void jsonToTileMetadataAppliesRequestedType() throws Exception {
        // The type is passed in rather than read from the JSON, the same payload serves both
        TileMetadata composite = handler.testJsonToTileMetadata(new JSONObject(exampleTileJson), ImageType.COMPOSITE);
        Assertions.assertEquals(ImageType.COMPOSITE, composite.getType());

        TileMetadata thumb = handler.testJsonToTileMetadata(new JSONObject(exampleTileJson), ImageType.THUMB);
        Assertions.assertEquals(ImageType.THUMB, thumb.getType());
    }

    @Test
    void jsonToTileMetadataEmptyObject() {
        // Verify empty JSONObject handling
        Assertions.assertThrows(JSONException.class, () ->
            handler.testJsonToTileMetadata(new JSONObject(), ImageType.COMPOSITE)
        );
    }

    @Test
    void jsonToTileMetadatas() throws Exception {
        JSONArray array = new JSONArray(exampleTilesJson);
        List<TileMetadata> result = handler.testJsonToTileMetadatas(array, ImageType.THUMB);

        // Verify list size and contents
        Assertions.assertEquals(2, result.size());

        // Verify first tile
        TileMetadata tile1 = result.get(0);
        Assertions.assertEquals(1, tile1.getId());
        Assertions.assertEquals("16,38", tile1.getCode());
        Assertions.assertEquals(10.0, tile1.getX());
        Assertions.assertEquals(0.0, tile1.getY());

        // Verify second tile
        TileMetadata tile2 = result.get(1);
        Assertions.assertEquals(2, tile2.getId());
        Assertions.assertEquals("17,38", tile2.getCode());
        Assertions.assertEquals(11.0, tile2.getX());
        Assertions.assertEquals(0.0, tile2.getY());
    }

    @Test
    void jsonToTileMetadatasEmpty() throws Exception {
        JSONArray array = new JSONArray("[]");
        List<TileMetadata> result = handler.testJsonToTileMetadatas(array, ImageType.THUMB);

        Assertions.assertEquals(0, result.size());
    }

    @Test
    void jsonToTileMetadatasMalformed() {
        JSONArray array = new JSONArray("[{\"id\": \"invalid\"}]");

        Assertions.assertThrows(JSONException.class, () ->
            handler.testJsonToTileMetadatas(array, ImageType.THUMB)
        );
    }
}
