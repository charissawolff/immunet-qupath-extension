package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.handlers.ServerConnectionHandler;

public class ConnectToServerCommand extends AbstractAsyncCommand<Void> {
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

    protected Void execute(Consumer<String> progressReporter) throws Exception {
        progressReporter.accept("Logging into server...");
        try {
            ServerConnectionHandler.getInstance().startSSHThread(username, hostname, password);
        } catch (Exception e) {
            progressReporter.accept("SSH connection failed: " + e.getMessage());
            throw e;
        }
        try{
            ServerConnectionHandler.getInstance().performDatabaseLogin(dbuser,dbpass);
        }catch (Exception e){
            progressReporter.accept("Database login failed: " + e.getMessage());
            throw e;
        }
        return null;
    }

}
