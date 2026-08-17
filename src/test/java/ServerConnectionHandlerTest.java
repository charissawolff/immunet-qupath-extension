import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.api.serverConnection.ApiClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServerConnectionHandlerTest {
    @Test
    void setServerCookieTest(){
        ApiClient.getInstance().testSetSessionCookie("test cookie");
        Assertions.assertEquals("test cookie", ApiClient.getInstance().getSessionCookie());
    }

    @Test
    void checkStatusCode(){
        Assertions.assertDoesNotThrow(() -> ApiClient.getInstance().testCheckStatusCode(200));
        Assertions.assertDoesNotThrow(() -> ApiClient.getInstance().testCheckStatusCode(300));
        Assertions.assertThrows(IOException.class, () -> ApiClient.getInstance().testCheckStatusCode(400));
        Assertions.assertThrows(IOException.class, () -> ApiClient.getInstance().testCheckStatusCode(404));
        Assertions.assertThrows(IOException.class, () -> ApiClient.getInstance().testCheckStatusCode(500));
        Assertions.assertThrows(IOException.class, () -> ApiClient.getInstance().testCheckStatusCode(550));
    }
}
