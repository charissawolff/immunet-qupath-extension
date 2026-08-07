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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ServerConnectionHandler implements PageFetcher,PagePoster<JSONArray> {
    private static final Duration REQUEST_TIMEOUT_SECONDS = Duration.ofSeconds(60);
    private static final ServerConnectionHandler INSTANCE = new ServerConnectionHandler();

    //used to tell main thread that the ssh tunnel is now ready
    CompletableFuture<Boolean> SSHReady;
    Thread SSHThread;
    SSHTunnelHandler sshTunnelHandler;
    private String sessionCookie; // obtained with successful webpage login
    private int localPort; // local end of the SSH tunnel; user-configurable
    HttpClient client = HttpClient.newHttpClient();

    public static ServerConnectionHandler getInstance() {
        return INSTANCE;
    }

    public ServerConnectionHandler (){
        SSHReady = new CompletableFuture<>();
    }

    /**
     * Creates and starts an SSH Thread that connects to the remote machine. Creates a tunnel on the given
     * local port, forwarding to the given remote port.
     *
     * @throws IOException if there were issues with the credentials.
     */
    public void startSSHThread(String username, String hostname, String password, int localPort, int remotePort) throws Exception {

        //resetting
        if (SSHThread != null){
           SSHReady = new CompletableFuture<>();
           SSHThread.interrupt();
           SSHThread = null;
        }
        if (sshTunnelHandler != null){
           sshTunnelHandler.closeSSHTunnel();
           sshTunnelHandler = null;
        }
        this.localPort = localPort;
        sshTunnelHandler = new SSHTunnelHandler(username, hostname, password, localPort, remotePort);
        SSHThread = new Thread(sshTunnelHandler);
        SSHThread.start();

        try {
            ImmuNetLog.log("Waiting for SSH thread.");
            SSHReady.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            //END THREAD and ssh tunnel IF IT WAS STARTED
            if (SSHThread != null) {
                SSHThread.interrupt();
                SSHThread = null;
                ImmuNetLog.log("SSH thread interrupted.");
                sshTunnelHandler.closeSSHTunnel();
            }
            ImmuNetLog.error("SSH connection failed.", e);
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
            //close the SSH tunnel/ thread if we can't log into the database
            if (SSHThread != null) {
                SSHThread.interrupt();
                SSHThread = null;
                ImmuNetLog.log("SSH thread interrupted.");
                sshTunnelHandler.closeSSHTunnel();
            }
            throw new IOException(e);
        }
    }

    /**
     * Builds the URI for a path on the local end of the SSH tunnel.
     */
    private URI buildUri(String path) {
        return URI.create("http://localhost:" + localPort + "/" + path);
    }

    /**
     * Fetch the content of a page with a GET request
     *
     * @param localPath path to webpage, appended to the local tunnel's base URL
     * @return full response package of the webpage incl. headers and body
     * @throws IOException
     * @throws InterruptedException
     */
    public HttpResponse<InputStream> fetchPage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .header("Cookie", sessionCookie)
                .timeout(REQUEST_TIMEOUT_SECONDS)
                .GET()
                .build();
        try{
            HttpResponse<InputStream> response = client.send(getRequest, BodyHandlers.ofInputStream());
            checkStatusCode(response.statusCode());
            return response;
        } catch (InterruptedException e) {
            throw e;
        } catch(IOException e){
            ImmuNetLog.error("Could not fetch page {}", localPath, e);
            return null;
        }
    }

    // Fetch content with type String
    public HttpResponse<String> fetchStringPage(String localPath) throws IOException, InterruptedException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(buildUri(localPath))
                .header("Cookie", sessionCookie)
                .timeout(REQUEST_TIMEOUT_SECONDS)
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
                .uri(buildUri("v/login"))
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
                .uri(buildUri(localPath))
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
