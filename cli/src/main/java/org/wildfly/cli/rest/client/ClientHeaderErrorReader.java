package org.wildfly.cli.rest.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.io.IOException;

import static org.wildfly.managed.common.util.Constants.WEB_ERROR_DESCRIPTION_HEADER_NAME;

/**
 * Use this instead of a ResponseExceptionMapper since the Response object passed in to the exeption mapper
 * has the headers and the response body removed.
 *
 */
public class ClientHeaderErrorReader implements ClientResponseFilter {
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() >= 300) {
            String reason = responseContext.getHeaderString(WEB_ERROR_DESCRIPTION_HEADER_NAME);
            if (reason != null) {
                throw new CommandFailedException("ERROR: " + reason, responseContext.getStatus());
            } else if (responseContext.getStatus() == 500){
                throw new CommandFailedException("ERROR: An internal server error occurred", 500);
            } else {
                throw new CommandFailedException("ERROR: An unknown error occurred. HTTP status code:" + responseContext.getStatus(), responseContext.getStatus());
            }
        }
    }
}
