package org.computational_immunology.ext.ImmuNet.core.handlers;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ServerConnectionHandler implements PageFetcher,PagePoster<JSONArray> {
    private static final ServerConnectionHandler INSTANCE = new ServerConnectionHandler();

    //service port is 8082
    //forward to local 8080
    //used to tell main thread that the ssh tunnel is now ready
    CompletableFuture<Boolean> SSHReady;
    Thread SSHThread;
    private String sessionCookie; // obtained with successful webpage login
    HttpClient client = HttpClient.newHttpClient();

    public static ServerConnectionHandler getInstance() {
        return INSTANCE;
    }

    public ServerConnectionHandler (){
        SSHReady = new CompletableFuture<>();
    }

    /**
     * Creates and starts an SSH Thread that connects to the remote machine. Creates a tunnel on local port 8082
     * for remote port 80.
     *
     * @throws IOException if there were issues with the credentials.
     */
    public void startSSHThread(String username, String hostname, String password) throws Exception {

        //resetting
       if (SSHThread != null)
       {
           SSHReady = new CompletableFuture<>();
           SSHThread.interrupt();
           SSHThread = null;
       }

        SSHTunnelHandler handler = new SSHTunnelHandler(username, hostname, password);
        SSHThread = new Thread(handler);
        SSHThread.start();

        try {
            ImmuNetLog.log("Waiting for SSH thread.");
            SSHReady.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new Exception (e);
        }
    }

    /*
    Logs into the Vectra database.
     */
    public void performDatabaseLogin(String dbuser, String dbpass) throws Exception {
        try {
            setSessionCookie(getDatabaseSessionCookie(dbuser, dbpass));
            ImmuNetLog.log("Successfully got session cookie");
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Invalid database credentials. Interrupting SSH Thread.", e);
            throw new IOException(e);
        }
    }

    /**
     * Fetch the content of a page with a GET request
     *
     * @param localPath path to webpage. Appended to http://localhost:8082/
     * @return full response package of the webpage incl. headers and body
     * @throws IOException
     * @throws InterruptedException
     */
    public HttpResponse<InputStream> fetchPage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8082/" + localPath))
                .header("Cookie", sessionCookie)
                .GET()
                .build();
        try{
            HttpResponse<InputStream> response = client.send(getRequest, BodyHandlers.ofInputStream());
            checkStatusCode(response.statusCode());
            return response;
        } catch (InterruptedException e) {
            throw e;
        } catch(IOException e){
            ImmuNetLog.error("Could not fetch page {}", localPath );
            return null;
        }
    }

    // Fetch content with type String
    public HttpResponse<String> fetchStringPage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8082/" + localPath))
                .header("Cookie", sessionCookie)
                .GET()
                .build();
        try{
            HttpResponse<String> response = client.send(getRequest, BodyHandlers.ofString());
            checkStatusCode(response.statusCode());
            return response;
        } catch (InterruptedException e) {
            throw e;
        } catch(IOException ioe){
            ImmuNetLog.error("Could not fetch page : {}", localPath, ioe);
            throw ioe;
        }
    }

    /**
     * Send a post request with credentials to login page
     *
     * @return HTTP reponse of the server as String
     * @throws InterruptedException
     * @throws IOException
     */
    public HttpResponse<String> postRequestVectraLogin(String username, String password) throws InterruptedException {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8082/v/login"))
                .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + password))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            HttpResponse<String> response = client.send(postRequest, BodyHandlers.ofString());
            checkStatusCode(response.statusCode());
            return response;
        } catch (InterruptedException e) {
            throw e;
        } catch(IOException e){
            ImmuNetLog.error("Could not log into Vectra database", e);
            return null;
        }
    }

    /**
     * Post request to ../v/login page with credentials
     *
     * @return session cookie
     * @throws IOException
     * @throws InterruptedException
     */
    public String getDatabaseSessionCookie(String username, String password) throws IOException, InterruptedException {
        HttpResponse<String> response = postRequestVectraLogin(username, password);
        HttpHeaders headers = response.headers();
        List<String> cookies = headers.allValues("Set-Cookie");
        setSessionCookie(cookies.get(0));
        return cookies.get(0);
    }

    public HttpResponse<String> postObject(String localPath, JSONArray payload) throws IOException, InterruptedException {
        // Send a POST request to the specified local path with the provided JSON payload
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8082/" + localPath))
                .header("Cookie", sessionCookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        try{
            HttpResponse<String> response = client.send(postRequest, BodyHandlers.ofString());
            checkStatusCode(response.statusCode());
            return response;
        } catch (IOException | InterruptedException e) {
            ImmuNetLog.error("Could not post object", e);
            throw e;
        }
    }

    private void setSessionCookie(String cookie) {
        sessionCookie = cookie;
    }

    public void testSetSessionCookie(String cookie) {
        setSessionCookie(cookie);
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    /**
     * Checks provided HTTP response status code for client or server errors. 
     * 
     * @param statusCode HTTP response status code
     * @throws IOException
     */
    private void checkStatusCode(int statusCode) throws IOException{
        if (statusCode >= 400 && statusCode < 500){
            // Handle client errors
            IOException e = new IOException("Client error. HTTP statuscode: " + statusCode);
            ImmuNetLog.error("Client error with fetching webpage", e);
            throw e;
        }
        if (statusCode >= 500){
            // Handle server errors
            IOException e = new IOException("Server error. HTTP statuscode: " + statusCode);
            ImmuNetLog.error("Server error with fetching webpage", e);
            throw e;
        }
    }

    public void testCheckStatusCode(int statusCode) throws IOException{
        checkStatusCode(statusCode);
    }
}
