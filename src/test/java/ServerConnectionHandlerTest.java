import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.ServerConnectionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServerConnectionHandlerTest {
    @Test
    void setServerCookieTest(){
        ServerConnectionHandler.getInstance().testSetSessionCookie("test cookie");
        Assertions.assertEquals("test cookie", ServerConnectionHandler.getInstance().getSessionCookie());
    }

    @Test
    void checkStatusCode(){
        Assertions.assertDoesNotThrow(() -> ServerConnectionHandler.getInstance().testCheckStatusCode(200));
        Assertions.assertDoesNotThrow(() -> ServerConnectionHandler.getInstance().testCheckStatusCode(300));
        Assertions.assertThrows(IOException.class, () -> ServerConnectionHandler.getInstance().testCheckStatusCode(400));
        Assertions.assertThrows(IOException.class, () -> ServerConnectionHandler.getInstance().testCheckStatusCode(404));
        Assertions.assertThrows(IOException.class, () -> ServerConnectionHandler.getInstance().testCheckStatusCode(500));
        Assertions.assertThrows(IOException.class, () -> ServerConnectionHandler.getInstance().testCheckStatusCode(550));
    }
}
