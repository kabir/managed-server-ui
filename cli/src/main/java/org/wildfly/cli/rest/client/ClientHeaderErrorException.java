package org.wildfly.cli.rest.client;

public class ClientHeaderErrorException extends RuntimeException {
    public ClientHeaderErrorException(String message) {
        super(message);
    }
}
