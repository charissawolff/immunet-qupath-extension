package org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.PageFetcher;
import org.computational_immunology.ext.ImmuNet.core.handlers.PagePoster;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class ServerConnectionHandler implements PageFetcher, PagePoster<JSONArray> {
    private static final Duration REQUEST_TIMEOUT_SECONDS = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT_IMAGE_SECONDS = Duration.ofSeconds(2400);
    private static final int MAX_POST_ATTEMPTS = 3;
    private static final Duration POST_RETRY_DELAY = Duration.ofSeconds(20);
    private static final int MAX_IMAGE_ATTEMPTS = 2;
    private static final Duration IMAGE_RETRY_DELAY = Duration.ofMillis(500);
    private static final ServerConnectionHandler INSTANCE = new ServerConnectionHandler();

    HttpClient client = HttpClient.newHttpClient();

    public static ServerConnectionHandler getInstance() {
        return INSTANCE;
    }

    private URI buildUri(String path) {
        return URI.create("http://localhost:" + SSHConnectionManager.getInstance().getLocalPort() + "/" + path);
    }

    public HttpResponse<InputStream> fetchImagePage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .header("Cookie", SessionManager.getInstance().getSessionCookie())
                .timeout(REQUEST_TIMEOUT_IMAGE_SECONDS)
                .GET()
                .build();
        for (int attempt = 1; attempt <= MAX_IMAGE_ATTEMPTS; attempt++) {
            try {
                HttpResponse<InputStream> response = client.send(getRequest, BodyHandlers.ofInputStream());
                StatusUtils.checkStatusCode(response.statusCode());
                return response;
            } catch (IOException e) {
                ImmuNetLog.error("Could not fetch page {}", localPath, e);
                if (attempt < MAX_IMAGE_ATTEMPTS) {
                    Thread.sleep(IMAGE_RETRY_DELAY.toMillis());
                }
            }
        }
        return null;
    }

    public HttpResponse<String> fetchStringPage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .header("Cookie", SessionManager.getInstance().getSessionCookie())
                .timeout(REQUEST_TIMEOUT_SECONDS)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(getRequest, BodyHandlers.ofString());
            StatusUtils.checkStatusCode(response.statusCode());
            return response;
        } catch (InterruptedException e) {
            throw e;
        } catch (IOException ioe) {
            ImmuNetLog.error("Could not fetch page : {}", localPath, ioe);
            throw ioe;
        }
    }

    private HttpResponse<String> sendPostWithRetry(HttpRequest request) throws IOException, InterruptedException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_POST_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                StatusUtils.checkStatusCode(response.statusCode());
                return response;
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_POST_ATTEMPTS) {
                    Thread.sleep(POST_RETRY_DELAY.toMillis());
                }
            }
        }
        throw lastException;
    }

    public HttpResponse<String> postObject(String localPath, JSONArray payload) throws IOException, InterruptedException {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .timeout(REQUEST_TIMEOUT_SECONDS)
                .header("Cookie", SessionManager.getInstance().getSessionCookie())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        try {
            return sendPostWithRetry(postRequest);
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Could not post object", e);
            throw e;
        }
    }
}
