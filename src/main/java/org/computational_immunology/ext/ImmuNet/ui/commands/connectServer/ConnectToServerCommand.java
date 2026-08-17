package org.computational_immunology.ext.ImmuNet.ui.commands.connectServer;

import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.api.ApiClient;
import org.computational_immunology.ext.ImmuNet.core.serverConnection.SSHConnectionManager;
import org.computational_immunology.ext.ImmuNet.ui.commands.AbstractAsyncCommand;

public class ConnectToServerCommand extends AbstractAsyncCommand<Boolean> {
    private final String username;
    private final String hostname;
    private final String password;
    private final String dbuser;
    private final String dbpass;
    private final int localPort;
    private final int remotePort;

    public ConnectToServerCommand(String username, String hostname, String password, String dbuser, String dbpass, int localPort, int remotePort){
        this.username = username;
        this.hostname = hostname;
        this.password = password;
        this.dbuser = dbuser;
        this.dbpass = dbpass;
        this.localPort = localPort;
        this.remotePort = remotePort;
    }

    protected Boolean execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Logging into server...");
        try {
            SSHConnectionManager.getInstance().startSSHThread(username, hostname, password, localPort, remotePort);
        } catch (Exception e) {
            ImmuNetLog.error("SSH connection failed.", e);
            progressReporter.accept(e.getMessage());
            return false;
        }
        try{
            ImmuNetLog.log("SSH connection established. Logging into database...");
            progressReporter.accept("Logging into database...");
            ApiClient.getInstance().performDatabaseLogin(dbuser, dbpass);
        }catch (Exception e){
            ImmuNetLog.error("Database login failed.", e);
            progressReporter.accept(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected void onSuccess(Boolean result) {
        if (result) {
            ImmuNetLog.log("Successfully connected to server: " + hostname + " with user: " + username);
        } else {
            ImmuNetLog.log("Failed to connect to server: " + hostname + " with user: " + username);
        }
    }

    @Override
    protected void onCancellation() {
        ImmuNetLog.log("Cancelled while connecting to server: " + hostname + " with user: " + username);
    }

    @Override
    protected void onFailure(Throwable exception) {
        ImmuNetLog.error("Failed to connect to server: " + hostname + " with user: " + username, exception);
    }

}
