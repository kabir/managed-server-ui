package org.wildfly.cli.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.wildfly.managed.common.model.Application;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

@Path("/app")
@RegisterRestClient(configKey = "managed-server-ui-backend")
public interface ApplicationService {
    @GET
    public List<Application> list();
}
