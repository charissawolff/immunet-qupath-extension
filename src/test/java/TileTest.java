import java.awt.image.BufferedImage;

import org.computational_immunology.ext.ImmuNet.core.models.Tile;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata;
import org.computational_immunology.ext.ImmuNet.core.models.TileMetadata.ImageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tile is now a TileMetadata plus the image it describes, so what is left to cover here is the
 * pairing itself: the null guards, the getters that delegate to the metadata, and resizeImage.
 * The metadata validation is covered by TileMetadataTest.
 */
class TileTest {

    private static final TileMetadata METADATA = new TileMetadata(1, "16,38", ImageType.THUMB, 10, 20, 900, 700);

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void rejectsMissingParts(){
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new Tile(null, image(4, 4))
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new Tile(METADATA, null)
        );
    }

    @Test
    void exposesWhatItWasBuiltFrom(){
        BufferedImage img = image(4, 4);
        Tile tile = new Tile(METADATA, img);

        Assertions.assertSame(METADATA, tile.getMetadata());
        Assertions.assertSame(img, tile.getImage());
    }

    @Test
    void gettersDelegateToMetadata(){
        Tile tile = new Tile(METADATA, image(4, 4));

        Assertions.assertEquals(1, tile.getId());
        Assertions.assertEquals("16,38", tile.getCode());
        Assertions.assertEquals(ImageType.THUMB, tile.getType());
        Assertions.assertEquals(10.0, tile.getTileX());
        Assertions.assertEquals(20.0, tile.getTileY());
        Assertions.assertEquals(900.0, tile.getTileW());
        Assertions.assertEquals(700.0, tile.getTileH());
    }

    @Test
    void resizeImageProducesRequestedSize(){
        Tile tile = new Tile(METADATA, image(8, 8));

        BufferedImage resized = tile.resizeImage(4, 6, false);
        Assertions.assertEquals(4, resized.getWidth());
        Assertions.assertEquals(6, resized.getHeight());

        // Quality path is a different set of rendering hints, same resulting dimensions
        BufferedImage quality = tile.resizeImage(16, 16, true);
        Assertions.assertEquals(16, quality.getWidth());
        Assertions.assertEquals(16, quality.getHeight());
    }

    @Test
    void resizeImageKeepsSourceTypeUnlessOverridden(){
        Tile tile = new Tile(METADATA, image(8, 8));

        // No override, so the source image type carries over
        Assertions.assertEquals(BufferedImage.TYPE_INT_RGB, tile.resizeImage(4, 4, false).getType());

        // QuPath's viewer needs a specific type, hence the override
        BufferedImage overridden = tile.resizeImage(4, 4, false, BufferedImage.TYPE_INT_ARGB);
        Assertions.assertEquals(BufferedImage.TYPE_INT_ARGB, overridden.getType());
    }

    @Test
    void resizeImageLeavesOriginalAlone(){
        BufferedImage img = image(8, 8);
        Tile tile = new Tile(METADATA, img);

        tile.resizeImage(4, 4, false);

        Assertions.assertEquals(8, img.getWidth());
        Assertions.assertEquals(8, img.getHeight());
        Assertions.assertSame(img, tile.getImage());
    }
}
