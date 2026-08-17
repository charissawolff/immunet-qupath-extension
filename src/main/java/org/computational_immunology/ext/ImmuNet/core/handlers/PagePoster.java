package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;
import java.net.http.HttpResponse;

/**
 * Posts a payload (usually JSON) to backend api to save in database.
 * @param <T> the type of the payload being posted.
 */
public interface PagePoster<T> {
    /**
     * POsts payload to localPath.
     * @param localPath the api route to post to.
     * @param payload the payload containing the data (usually in JSON format) to send.
     * @return the raw HTTP response, with the body as a String (usually JSON)
     * @throws IOException when the post request fails.
     * @throws InterruptedException when user cancels the javafx task in which the posting is done.
     */
    HttpResponse<String> postObject(String localPath, T payload) throws IOException, InterruptedException;
}
