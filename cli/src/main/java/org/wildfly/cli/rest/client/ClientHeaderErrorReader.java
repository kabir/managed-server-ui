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
            String errorMessage = null;
            if (reason != null) {
                errorMessage = reason;
            } else if (responseContext.getStatus() == 500){
                errorMessage = "An internal server error occurred";
            } else {
                errorMessage = "An unknown error occurred. HTTP status code:" + responseContext.getStatus();
            }

            if (errorMessage != null) {
                errorMessage = "ERROR: " + errorMessage;
                throw new ClientHeaderErrorException(errorMessage);
            }
        }
    }
}
