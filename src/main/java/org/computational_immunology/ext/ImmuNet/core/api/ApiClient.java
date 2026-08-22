package org.computational_immunology.ext.ImmuNet.core.api;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.VectraException;
import org.computational_immunology.ext.ImmuNet.core.serverConnection.SSHConnectionManager;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends HTTP requests to the backend api via the SSH tunnel. Implements {@link PageFetcher} and
 * {@link PagePoster} to fetch and post data. Includes function to login, since this database login is what sets the cookies for all other
 * requests to work.
 */
public class ApiClient implements PageFetcher, PagePoster<JSONArray> {
    private static final String LOGIN_PATH_FORMAT = "v/login";
    private static final String LOGIN_BODY_FORMAT = "username=%s&password=%s";
    private static final Duration REQUEST_TIMEOUT_SECONDS = Duration.ofSeconds(60);
    private static final int MAX_POST_ATTEMPTS = 3;
    private static final Duration POST_RETRY_DELAY = Duration.ofSeconds(20);
    private static final ApiClient INSTANCE = new ApiClient();

    private final HttpClient client = HttpClient.newHttpClient();
    private String sessionCookie;

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    private URI buildUri(String path) {
        return URI.create("http://localhost:" + SSHConnectionManager.getInstance().getLocalPort() + "/" + path);
    }

    /**
     * Logs into the database and sets the sessionCookie with the correct information for other requests to work after. 
     * @param dbuser the username for database login.
     * @param dbpass the password for database login.
     * @throws IOException when the api can not process the request.
     * @throws InterruptedException when the user interrupts the task that calls this function.
     */
    public void performDatabaseLogin(String dbuser, String dbpass) throws IOException, InterruptedException {
        try {
            sessionCookie = getDatabaseSessionCookie(dbuser, dbpass);
            ImmuNetLog.log("Successfully got session cookie");
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Database login failed. Interrupting SSH thread.", e);
            SSHConnectionManager.getInstance().interrupt();
            if (e instanceof VectraException) {
                throw e;
            }
            throw VectraException.loginFailed(e);
        }
    }

    public String getDatabaseSessionCookie(String username, String password) throws IOException, InterruptedException {
        HttpResponse<String> response = postRequestVectraLogin(username, password);
        List<String> cookies = response.headers().allValues("Set-Cookie");
        if (cookies.isEmpty()) {
            throw VectraException.wrongCredentials(null);
        }
        return cookies.stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .collect(Collectors.joining("; "));
    }

    public HttpResponse<String> postRequestVectraLogin(String username, String password) throws InterruptedException, IOException {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(buildUri(LOGIN_PATH_FORMAT))
                .timeout(REQUEST_TIMEOUT_SECONDS)
                .POST(HttpRequest.BodyPublishers.ofString(String.format(LOGIN_BODY_FORMAT, username, password)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            return sendPostWithRetry(postRequest);
        } catch (IOException e) {
            ImmuNetLog.error("Could not log into Vectra database", e);
            throw e;
        }
    }

    public HttpResponse<InputStream> fetchImagePage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .header("Cookie", sessionCookie)
                .GET()
                .build();
            try {
                HttpResponse<InputStream> response = client.send(getRequest, BodyHandlers.ofInputStream());
                StatusUtils.checkStatusCode(response.statusCode());
                return response;
            } catch (IOException ioe) {
                ImmuNetLog.error("Could not fetch page {}", localPath, ioe);
                throw ioe;
            } catch (InterruptedException ie){
                return null;
            }
        }

    public HttpResponse<String> fetchStringPage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .header("Cookie", sessionCookie)
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

    public HttpResponse<String> postObject(String localPath, JSONArray payload) throws IOException, InterruptedException {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .timeout(REQUEST_TIMEOUT_SECONDS)
                .header("Cookie", sessionCookie)
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

    private HttpResponse<String> sendPostWithRetry(HttpRequest request) throws IOException, InterruptedException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_POST_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                int statusCode = response.statusCode();
                if (statusCode == 401 || statusCode == 403) {
                    throw VectraException.wrongCredentials(null);
                }
                StatusUtils.checkStatusCode(statusCode);
                return response;
            } catch (VectraException e) {
                throw e;
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_POST_ATTEMPTS) {
                    Thread.sleep(POST_RETRY_DELAY.toMillis());
                }
            }
        }
        throw lastException;
    }
}
