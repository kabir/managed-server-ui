package org.wildfly.cli.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/app")
@RegisterRestClient(configKey = "managed-server-ui-backend")
public interface ApplicationService {
    @GET
    List<Application> list();

    @POST
    Application create(Application application);

    @DELETE
    @Path("/{name}")
    void delete(String name);

    @POST
    @Path("/{name}/deploy")
    void deploy(String name);


    @GET
    @Path("/{appName}/archive")
    List<AppArchive> listArchives(String appName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive")
    void uploadArchive(String appName, @MultipartForm DeploymentDto deploymentDto);

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive/{archiveName}")
    void replaceArchive(String appName, String archiveName, @MultipartForm DeploymentDto dto);

    @DELETE
    @Path("/{appName}/archive/{archiveName}")
    void deleteArchive(String appName, String archiveName);

}
