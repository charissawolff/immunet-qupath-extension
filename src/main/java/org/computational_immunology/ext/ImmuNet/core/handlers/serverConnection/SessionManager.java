package org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.VectraException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;

public class SessionManager {
    private static final String LOGIN_PATH_FORMAT = "v/login";
    private static final String LOGIN_BODY_FORMAT = "username=%s&password=%s";
    private static final Duration REQUEST_TIMEOUT_SECONDS = Duration.ofSeconds(20);
    private static final int MAX_POST_ATTEMPTS = 3;
    private static final Duration POST_RETRY_DELAY = Duration.ofSeconds(20);
    private static final SessionManager INSTANCE = new SessionManager();

    private String sessionCookie;
    private final HttpClient client = HttpClient.newHttpClient();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void performDatabaseLogin(String dbuser, String dbpass) throws IOException, InterruptedException {
        try {
            setSessionCookie(getDatabaseSessionCookie(dbuser, dbpass));
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

    private URI buildUri(String path) {
        return URI.create("http://localhost:" + SSHConnectionManager.getInstance().getLocalPort() + "/" + path);
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

    public String getDatabaseSessionCookie(String username, String password) throws IOException, InterruptedException {
        HttpResponse<String> response = postRequestVectraLogin(username, password);
        HttpHeaders headers = response.headers();
        List<String> cookies = headers.allValues("Set-Cookie");
        if (cookies.isEmpty()) {
            throw VectraException.wrongCredentials(null);
        }
        setSessionCookie(cookies.get(0));
        return cookies.get(0);
    }

    private void setSessionCookie(String cookie) {
        sessionCookie = cookie;
    }

    public String getSessionCookie() {
        return sessionCookie;
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