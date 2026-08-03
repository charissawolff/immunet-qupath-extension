package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.handlers.ServerConnectionHandler;

public class ConnectToServerCommand extends AbstractAsyncCommand<Boolean> {
    private final String username;
    private final String hostname;
    private final String password;
    private final String dbuser;
    private final String dbpass;

    public ConnectToServerCommand(String username, String hostname, String password, String dbuser, String dbpass){
        this.username = username;
        this.hostname = hostname;
        this.password = password;
        this.dbuser = dbuser;
        this.dbpass = dbpass;

    }

    protected Boolean execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Logging into server...");
        try {
            ServerConnectionHandler.getInstance().startSSHThread(username, hostname, password);
        } catch (Exception e) {
            ImmuNetLog.error("SSH connection failed.", e);
            progressReporter.accept("SSH connection failed. Probably wrong credentials for username, hostname and password." );
            return false;
        }
        try{
            ImmuNetLog.log("SSH connection established. Logging into database...");
            progressReporter.accept("Logging into database...");
            ServerConnectionHandler.getInstance().performDatabaseLogin(dbuser,dbpass);
        }catch (Exception e){
            ImmuNetLog.error("Database login failed.", e);
            progressReporter.accept("Database login failed: Probably wrong credentials for dbuser and dbpass.");
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
