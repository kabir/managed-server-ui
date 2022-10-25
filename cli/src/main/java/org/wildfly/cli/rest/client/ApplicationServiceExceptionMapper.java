package org.wildfly.cli.rest.client;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.core.Response;

public class ApplicationServiceExceptionMapper implements ResponseExceptionMapper<RuntimeException> {
    @Override
    public RuntimeException toThrowable(Response response) {
        System.out.println("ERROR!");
        System.out.println("Status: " + response.getStatus());
        System.out.println("Body: " + response.getEntity().toString());
        return null;
    }
}
