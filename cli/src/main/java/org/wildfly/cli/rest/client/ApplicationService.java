package org.wildfly.cli.rest.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.value.AppState;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/app")
@RegisterRestClient(configKey = "managed-server-ui-backend")
@RegisterProvider(ClientHeaderErrorReader.class)
@ExitingOnError
public interface ApplicationService {
    @GET
    List<Application> list();

    @POST
    Application create(Application application);

    @GET
    @Path("/{name}")
    Application get(String name, @QueryParam("verbose") boolean verbose);

    @DELETE
    @Path("/{name}")
    void delete(String name, @QueryParam("force") Boolean force);

    @POST
    @Path("/{name}/deploy")
    void deploy(String name, @QueryParam("force") Boolean force, @QueryParam("refresh") Boolean refresh);

    @ResponseStatus(204) // NO_CONTENT
    @PUT
    @Path("/{appName}/stop")
    void stop(String appName);

    @GET
    @Path("/{appName}/archive")
    List<AppArchive> listArchives(String appName);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive")
    void addArchive(String appName, @MultipartForm DeploymentDto deploymentDto);

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive/{archiveName}")
    void replaceArchive(String appName, String archiveName, @MultipartForm DeploymentDto dto);

    @DELETE
    @Path("/{appName}/archive/{archiveName}")
    void deleteArchive(String appName, String archiveName);

    @GET
    @Path("/{appName}/config-file")
    String getConfigFileContents(String appName, @QueryParam("type") String type);

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/config-file")
    void setConfigFileContents(String appName, @QueryParam("type") String type, @MultipartForm DeploymentDto dto);

    @DELETE
    @Path("/{appName}/config-file")
    void deleteConfigFileContents(String appName, @QueryParam("type") String type);

    @GET
    @Path("/{appName}/status")
    AppState status(String appName);

}
