import org.junit.jupiter.api.Test;
import org.computational_immunology.ext.ImmuNet.core.Dimensions;
import org.junit.jupiter.api.Assertions;


class DimensionsTest {
    @Test
    void getHeight(){
        Dimensions dim = new Dimensions(10, 10);
        Assertions.assertEquals(10, dim.getHeight());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dimensions(0, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dimensions(-10, 5));
    }

    @Test
    void getWidth(){
        Dimensions dim = new Dimensions(10, 10);
        Assertions.assertEquals(10, dim.getWidth());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dimensions(0, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dimensions(5, -10));
    }
}

