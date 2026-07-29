package org.computational_immunology.ext.ImmuNet.core.handlers;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SSHTunnelHandler implements Runnable{
    String username;
    String hostname;
    String password;

    SshClient sshClient;
    ClientSession clientSession;
    CompletableFuture<Boolean> Ready;

    public SSHTunnelHandler(String username, String hostname, String password)
    {
        this.username = username;
        this.hostname = hostname;
        this.password = password;
    }

    @Override
    public void run() {
            try {
                createSSHTunnel();
            } catch (IOException e) {
                ImmuNetLog.error("Could not start SSH thread. Are your credentials wrong?", e);
            }
    }

    //Starts the SSH client and sets up the tunnel. This will wait until the connection fails.
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
        sshdSocketAddress = new SshdSocketAddress("localhost", 8082);
        remoteSocketAddress = new SshdSocketAddress("localhost", 80);
        session.startLocalPortForwarding(sshdSocketAddress, remoteSocketAddress);

        ImmuNetLog.log("Port forwarding success");

        ServerConnectionHandler.getInstance().SSHReady.complete(true); //tell main thread we are ready

        clientSession = session;
        Set<ClientSession.ClientSessionEvent> Events = session.waitFor(Set.of(ClientSession.ClientSessionEvent.CLOSED), -1);
        System.out.println("SSH session closed: ");
        for(ClientSession.ClientSessionEvent Event : Events)
        {
            System.out.println(Event);
        }
    }
}
