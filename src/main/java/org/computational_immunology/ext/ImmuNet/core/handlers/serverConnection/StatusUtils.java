package org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection;

import java.io.IOException;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

public class StatusUtils {

    public static void checkStatusCode(int statusCode) throws IOException {
        if (statusCode >= 400 && statusCode < 500) {
            IOException e = new IOException("Client error. HTTP statuscode: " + statusCode);
            ImmuNetLog.error("Client error with fetching webpage", e);
            throw e;
        }
        if (statusCode >= 500) {
            IOException e = new IOException("Server error. HTTP statuscode: " + statusCode);
            ImmuNetLog.error("Server error with fetching webpage", e);
            throw e;
        }
    }

    private StatusUtils(){
        // should not be initialized
    }
    
}
