package org.wildfly.managed;

import javax.ws.rs.core.Response;

public class ServerException extends RuntimeException {
    private final Response.Status status;

    public ServerException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public ServerException(Response.Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
