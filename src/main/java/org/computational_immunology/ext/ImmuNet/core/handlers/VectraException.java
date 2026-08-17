package org.computational_immunology.ext.ImmuNet.core.handlers;

import java.io.IOException;

public class VectraException extends IOException {
    private VectraException(String message, Throwable cause) {
        super(message, cause);
    }

    public static VectraException wrongCredentials(Throwable cause) {
        return new VectraException("Invalid username or password.", cause);
    }
    public static VectraException loginFailed(Throwable cause) {
        return new VectraException("Could not log in to the database. Please try again.", cause);
    }
    public static VectraException portInUse(Throwable cause) {
        return new VectraException("That local port is already in use. Choose a different port or close the other connection.", cause);
    }
    public static VectraException unknownHost(Throwable cause) {
        return new VectraException("Could not find that hostname. Check it and try again.", cause);
    }
    public static VectraException hostUnreachable(Throwable cause) {
        return new VectraException("Could not reach the server. Check the hostname and that the server is online.", cause);
    }
    public static VectraException timedOut(Throwable cause) {
        return new VectraException("Connecting to the server timed out. Check your network and try again.", cause);
    }
    public static VectraException sshConnectionFailed(Throwable cause) {
        return new VectraException("Could not establish an SSH connection. Check your username, hostname, and password.", cause);
    }
    public static VectraException networkUnreachable(Throwable cause) {
        return new VectraException("Your network is unreachable. Check your internet connection and try again.", cause);
    }
}