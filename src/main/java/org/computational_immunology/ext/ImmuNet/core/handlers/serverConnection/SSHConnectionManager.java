package org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SSHConnectionManager {
    private static final SSHConnectionManager INSTANCE = new SSHConnectionManager();

    CompletableFuture<Boolean> SSHReady;
    Thread SSHThread;
    SSHTunnelHandler sshTunnelHandler;
    private int localPort;

    public static SSHConnectionManager getInstance() {
        return INSTANCE;
    }

    public SSHConnectionManager() {
        SSHReady = new CompletableFuture<>();
    }

    public void startSSHThread(String username, String hostname, String password, int localPort, int remotePort) throws Exception {
        if (SSHThread != null) {
            SSHReady = new CompletableFuture<>();
            SSHThread.interrupt();
            SSHThread = null;
        }
        if (sshTunnelHandler != null) {
            sshTunnelHandler.closeSSHTunnel();
            sshTunnelHandler = null;
        }
        this.localPort = localPort;
        sshTunnelHandler = new SSHTunnelHandler(username, hostname, password, localPort, remotePort, SSHReady);
        SSHThread = new Thread(sshTunnelHandler);
        SSHThread.start();

        try {
            ImmuNetLog.log("Waiting for SSH thread.");
            SSHReady.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            interrupt();
            ImmuNetLog.error("SSH connection failed.", e);
            throw new Exception(e);
        }
    }

    public void interrupt() {
        if (SSHThread != null) {
            SSHThread.interrupt();
            SSHThread = null;
            ImmuNetLog.log("SSH thread interrupted.");
            try {
                sshTunnelHandler.closeSSHTunnel();
            } catch (Exception e) {
                ImmuNetLog.error("Error closing SSH tunnel.", e);
            }
        }
    }

    public int getLocalPort() {
        return localPort;
    }
}