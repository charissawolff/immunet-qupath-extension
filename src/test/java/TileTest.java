import org.computational_immunology.Tile;
import org.computational_immunology.Tile.ImageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TileTest {
    @Test
    void getId(){
        Tile tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals(1, tile1.getId());
        tile1 = new Tile(0, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals(0, tile1.getId());

        // Verify negative values
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(-1, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1)
        );
    }

    @Test
    void getPath(){
        Tile tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals("/16,38/thumb.jpg", tile1.getPath());
        tile1 = new Tile(1, "", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals("/16,38/thumb.jpg", tile1.getPath());

        // Verify null handling
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, null, ImageType.THUMB, "16,38", 0, 0, 1, 1)
        );
    }

    @Test 
    void getType(){
        Tile tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals("thumb", tile1.getType());
        tile1 = new Tile(1, "/", ImageType.COMPOSITE, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals("composite", tile1.getType());
    }

    @Test
    void getCode(){
        Tile tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals("16,38", tile1.getCode());

        tile1 = new Tile(1, "/", ImageType.THUMB, "-1,38", 0, 0, 1, 1);
        Assertions.assertEquals("-1,38", tile1.getCode());

        tile1 = new Tile(1, "/", ImageType.THUMB, "16,-1", 0, 0, 1, 1);
        Assertions.assertEquals("16,-1", tile1.getCode());

        tile1 = new Tile(1, "/", ImageType.THUMB, "-1,-1", 0, 0, 1, 1);
        Assertions.assertEquals("-1,-1", tile1.getCode());

        // Verify incorrect value handling
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, "/", ImageType.THUMB, null, 0, 0, 1, 1)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, "/", ImageType.THUMB, "", 0, 0, 1, 1)
        );
    }

    @Test
    void getCoordinates(){
        Tile tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 10, 20, 1, 1);
        Assertions.assertEquals(10, tile1.getTileX());
        Assertions.assertEquals(20, tile1.getTileY());
        
        // Test zero coordinates
        tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, 1, 1);
        Assertions.assertEquals(0, tile1.getTileX());
        Assertions.assertEquals(0, tile1.getTileY());
        
        // Test negative coordinates
        tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", -10, -20, 1, 1);
        Assertions.assertEquals(-10, tile1.getTileX());
        Assertions.assertEquals(-20, tile1.getTileY());
        
        // Test decimal coordinates
        tile1 = new Tile(1, "/", ImageType.THUMB, "16,38", 10.5, 20.7, 1, 1);
        Assertions.assertEquals(10.5, tile1.getTileX());
        Assertions.assertEquals(20.7, tile1.getTileY());
    }

    @Test
    void invalidDimensions() {
        // Verify negative width throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, -1, 1)
        );
        
        // Verify negative height throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, 1, -1)
        );
        
        // Verify both negative throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new Tile(1, "/", ImageType.THUMB, "16,38", 0, 0, -1, -1)
        );
    }
}
