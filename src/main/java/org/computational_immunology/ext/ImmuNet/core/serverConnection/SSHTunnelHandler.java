package org.computational_immunology.ext.ImmuNet.core.serverConnection;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Connects to a remote SSH host and opens a local tunnel to it. Runs on its own thread,
 * started and interrupted by {@link SSHConnectionManager}, and signals readiness or failure to that
 * thread via the given {@link CompletableFuture}.
 */

public class SSHTunnelHandler implements Runnable{
    String username;
    String hostname;
    String password;
    int localPort;
    int remotePort;

    CompletableFuture<Boolean> ready;
    SshClient sshClient;
    ClientSession clientSession;

    /**
     * Constructs a handler for the given SSH connection to run on its own thread.
     * @param username the SSH username
     * @param hostname the SSH hostname
     * @param password the SSH password
     * @param localPort the local end of the tunnel
     * @param remotePort the remote port to forward to
     * @param ready completed once the tunnel is open, or exception if it could not be opened
     */
    public SSHTunnelHandler(String username, String hostname, String password, int localPort, int remotePort, CompletableFuture<Boolean> ready)
    {
        this.username = username;
        this.hostname = hostname;
        this.password = password;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.ready = ready;
    }

    /**
     * {@inheritDoc}
     * Opens the SSH tunnel. Reports that it failed to open the tunnel via {@code ready}.
     */
    @Override
    public void run() {
            try {
                createSSHTunnel();
            } catch (IOException  | IllegalStateException e) {
                ready.completeExceptionally(e);
                ImmuNetLog.error("Could not start SSH thread", e);
            } 
    }

    /**
     * Starts the SSH client, perform authentication and forwards the local port to the remote port.
     * @throws IOException if the connection or authentication fails
     */
    public void createSSHTunnel() throws IOException {
        SshClient client = SshClient.setUpDefaultClient();
        sshClient = client;
        client.start();

        ClientSession session = client.connect(username, hostname, 22)
                .verify(5000) //timeout in ms
                .getSession();

        session.addPasswordIdentity(password);
        session.auth().verify(5000);

        SshdSocketAddress remoteSocketAddress;
        SshdSocketAddress  sshdSocketAddress;
        sshdSocketAddress = new SshdSocketAddress("localhost", localPort);
        remoteSocketAddress = new SshdSocketAddress("localhost", remotePort);
        session.startLocalPortForwarding(sshdSocketAddress, remoteSocketAddress);

        ImmuNetLog.log("Port forwarding success");

        ready.complete(true); //tell main thread we are ready

        clientSession = session;
        Set<ClientSession.ClientSessionEvent> Events = session.waitFor(Set.of(ClientSession.ClientSessionEvent.CLOSED), -1);
        System.out.println("SSH session closed: ");
        for(ClientSession.ClientSessionEvent Event : Events)
        {
            System.out.println(Event);
        }
    }

    public void closeSSHTunnel() throws IOException {
        if (clientSession != null) {
            clientSession.close();
        }
        if (sshClient != null) {
            sshClient.stop();
        }
    }
}

