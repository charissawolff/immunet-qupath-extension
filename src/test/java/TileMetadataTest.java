import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for tile metadata, which is a record that describes a tile image and its position in the slide. 
 */
class TileMetadataTest {

    private static TileMetadata metadata(int id, String code, ImageType type, double x, double y, double w, double h) {
        return new TileMetadata(id, code, type, x, y, w, h);
    }

    @Test
    void getId(){
        Assertions.assertEquals(1, metadata(1, "16,38", ImageType.THUMB, 0, 0, 1, 1).getId());
        Assertions.assertEquals(0, metadata(0, "16,38", ImageType.THUMB, 0, 0, 1, 1).getId());

        // Verify negative values
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(-1, "16,38", ImageType.THUMB, 0, 0, 1, 1)
        );
    }

    @Test
    void getType(){
        Assertions.assertEquals(ImageType.THUMB, metadata(1, "16,38", ImageType.THUMB, 0, 0, 1, 1).getType());
        Assertions.assertEquals(ImageType.COMPOSITE, metadata(1, "16,38", ImageType.COMPOSITE, 0, 0, 1, 1).getType());

        // The lowercase form is what gets interpolated into tile image paths
        Assertions.assertEquals("thumb", ImageType.THUMB.toString());
        Assertions.assertEquals("composite", ImageType.COMPOSITE.toString());

        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "16,38", null, 0, 0, 1, 1)
        );
    }

    @Test
    void getCode(){
        Assertions.assertEquals("16,38", metadata(1, "16,38", ImageType.THUMB, 0, 0, 1, 1).getCode());
        Assertions.assertEquals("-1,38", metadata(1, "-1,38", ImageType.THUMB, 0, 0, 1, 1).getCode());
        Assertions.assertEquals("16,-1", metadata(1, "16,-1", ImageType.THUMB, 0, 0, 1, 1).getCode());
        Assertions.assertEquals("-1,-1", metadata(1, "-1,-1", ImageType.THUMB, 0, 0, 1, 1).getCode());

        // Verify incorrect value handling
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, null, ImageType.THUMB, 0, 0, 1, 1)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "", ImageType.THUMB, 0, 0, 1, 1)
        );
    }

    @Test
    void getCoordinates(){
        TileMetadata tile = metadata(1, "16,38", ImageType.THUMB, 10, 20, 1, 1);
        Assertions.assertEquals(10.0, tile.getX());
        Assertions.assertEquals(20.0, tile.getY());

        // Test zero coordinates
        tile = metadata(1, "16,38", ImageType.THUMB, 0, 0, 1, 1);
        Assertions.assertEquals(0.0, tile.getX());
        Assertions.assertEquals(0.0, tile.getY());

        // Test negative coordinates
        tile = metadata(1, "16,38", ImageType.THUMB, -10, -20, 1, 1);
        Assertions.assertEquals(-10.0, tile.getX());
        Assertions.assertEquals(-20.0, tile.getY());

        // Test decimal coordinates
        tile = metadata(1, "16,38", ImageType.THUMB, 10.5, 20.7, 1, 1);
        Assertions.assertEquals(10.5, tile.getX());
        Assertions.assertEquals(20.7, tile.getY());
    }

    @Test
    void getDimensions(){
        TileMetadata tile = metadata(1, "16,38", ImageType.THUMB, 0, 0, 900, 700);
        Assertions.assertEquals(900.0, tile.getWidth());
        Assertions.assertEquals(700.0, tile.getHeight());
    }

    @Test
    void invalidDimensions() {
        // Verify negative width throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "16,38", ImageType.THUMB, 0, 0, -1, 1)
        );

        // Verify negative height throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "16,38", ImageType.THUMB, 0, 0, 1, -1)
        );

        // Verify both negative throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "16,38", ImageType.THUMB, 0, 0, -1, -1)
        );

        // Zero is rejected too, a tile with no area is not drawable
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "16,38", ImageType.THUMB, 0, 0, 0, 1)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            metadata(1, "16,38", ImageType.THUMB, 0, 0, 1, 0)
        );
    }

    @Test
    void withTypeKeepsEverythingElse() {
        TileMetadata thumb = metadata(1, "16,38", ImageType.THUMB, 10, 20, 900, 700);
        TileMetadata composite = thumb.withType(ImageType.COMPOSITE);

        Assertions.assertEquals(ImageType.COMPOSITE, composite.getType());
        Assertions.assertEquals(ImageType.THUMB, thumb.getType(), "withType must not mutate the original");
        Assertions.assertEquals(thumb.getId(), composite.getId());
        Assertions.assertEquals(thumb.getCode(), composite.getCode());
        Assertions.assertEquals(thumb.getX(), composite.getX());
        Assertions.assertEquals(thumb.getY(), composite.getY());
        Assertions.assertEquals(thumb.getWidth(), composite.getWidth());
        Assertions.assertEquals(thumb.getHeight(), composite.getHeight());
    }
}
