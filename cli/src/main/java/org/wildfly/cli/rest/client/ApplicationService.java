package org.wildfly.cli.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import org.wildfly.managed.common.model.Application;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/app")
@RegisterRestClient(configKey = "managed-server-ui-backend")
public interface ApplicationService {
    @GET
    List<Application> list();

    @POST
    Application create(Application application);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/upload")
    void tempUpload(@MultipartForm DeploymentDto deploymentDto);
}
