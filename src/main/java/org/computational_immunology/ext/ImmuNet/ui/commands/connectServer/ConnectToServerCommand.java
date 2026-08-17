package org.computational_immunology.ext.ImmuNet.ui.commands.connectServer;

import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection.SSHConnectionManager;
import org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection.SessionManager;
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
            progressReporter.accept("SSH connection failed." );
            return false;
        }
        try{
            ImmuNetLog.log("SSH connection established. Logging into database...");
            progressReporter.accept("Logging into database...");
            SessionManager.getInstance().performDatabaseLogin(dbuser, dbpass);
        }catch (Exception e){
            ImmuNetLog.error("Database login failed.", e);
            progressReporter.accept("Database login failed.");
            return false;
        }
        return true;
    }

    @Override
    protected void onSuccess(Boolean result) {
        ImmuNetLog.log("Successfully connected to server: " + hostname + " with user: " + username);
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
