package org.computational_immunology.ext.ImmuNet.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

/**
 * Fetches raw HTTP responses from the backend api.
 */
public interface PageFetcher {
    /**
     * Fetches path and reads the body as a string. Used for JSON responses.
     * @param localPath the path to fetch
     * @return the HTTP response.
     * @throws IOException when the request fails.
     * @throws InterruptedException when user cancels the javafx task in which the fetching is done.
     */
    HttpResponse<String> fetchStringPage(String localPath) throws IOException, InterruptedException;

    /**
     * Fetches image path.
     * @param localPath the path to fetch
     * @return the HTTP response, with the body as a stream.
     * @throws IOException when the request fails.
     * @throws InterruptedException when user cancels the javafx task in which the fetching is done.
     */
    HttpResponse<InputStream> fetchImagePage(String localPath) throws IOException, InterruptedException;
}
