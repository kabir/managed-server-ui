package org.wildfly.cli.rest.client;

public class CommandFailedException extends RuntimeException {
    private final int statusCode;

    public CommandFailedException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
