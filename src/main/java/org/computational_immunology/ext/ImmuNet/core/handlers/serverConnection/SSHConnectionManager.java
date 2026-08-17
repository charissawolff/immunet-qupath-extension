package org.computational_immunology.ext.ImmuNet.core.handlers.serverConnection;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;
import org.computational_immunology.ext.ImmuNet.core.VectraException;

import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SSHConnectionManager {
    private static final SSHConnectionManager INSTANCE = new SSHConnectionManager();

    private CompletableFuture<Boolean> SSHReady;
    private Thread SSHThread;
    private SSHTunnelHandler sshTunnelHandler;
    private int localPort;

    public static SSHConnectionManager getInstance() {
        return INSTANCE;
    }

    public SSHConnectionManager() {
        SSHReady = new CompletableFuture<>();
    }

    public void startSSHThread(String username, String hostname, String password, int localPort, int remotePort) throws Exception {
        if (SSHThread != null) {
            SSHThread.interrupt();
            SSHThread = null;
        }
        if (sshTunnelHandler != null) {
            sshTunnelHandler.closeSSHTunnel();
            sshTunnelHandler = null;
        }
        SSHReady = new CompletableFuture<>();
        this.localPort = localPort;
        sshTunnelHandler = new SSHTunnelHandler(username, hostname, password, localPort, remotePort, SSHReady);
        SSHThread = new Thread(sshTunnelHandler);
        SSHThread.start();

        try {
            ImmuNetLog.log("Waiting for SSH thread.");
            SSHReady.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException ee){
            interrupt();
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            ImmuNetLog.error("SSH connection failed.", cause);
            throw getException(cause);
        } catch (TimeoutException | InterruptedException e) {
            interrupt();
            ImmuNetLog.error("SSH connection failed.", e);
            throw VectraException.timedOut(e);
        }
    }

    private VectraException getException(Throwable cause) {
        if (hasCause(cause, BindException.class)) {
            return VectraException.portInUse(cause);
        }
        if (hasCause(cause, UnknownHostException.class)) {
            return VectraException.unknownHost(cause);
        }
        if (hasCause(cause, ConnectException.class)) {
            return VectraException.hostUnreachable(cause);
        }
        if (hasCause(cause, SocketException.class)) {
            return VectraException.networkUnreachable(cause);
        }
        return VectraException.sshConnectionFailed(cause);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
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