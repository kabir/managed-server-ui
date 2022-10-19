package org.wildfly.managed;

import org.jboss.resteasy.reactive.MultipartForm;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Path("/app")
public class ApplicationResource {

    @Inject
    UiPaths uiPaths;

    @Inject
    ApplicationRepo applicationRepo;

    @GET
    public List<Application> list() {
        return applicationRepo.listAll();
    }

    @POST
    public Application create(Application application) {
        System.out.println("----> Creating " + application);
        System.out.println(application.getName());
        return applicationRepo.create(application);
    }

    @DELETE
    @Path("/{name}")
    @Transactional
    public Response delete(String name) {
        Application application = applicationRepo.findByName(name);
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        applicationRepo.delete(name);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}/upload")
    public Response upload(String name, @MultipartForm DeploymentData data) {
        try {
            java.nio.file.Path path = data.file.uploadedFile();
            System.out.println("----> " + path);
            System.out.println("----> " + data.file.fileName());

            if (!data.file.fileName().endsWith(".war")) {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
            }

            java.nio.file.Path dir = uiPaths.getApplicationDir(name);
            Files.move(data.file.uploadedFile(), dir.resolve(data.file.fileName()));

            return Response.status(Response.Status.ACCEPTED).build();
            // TODO record the deployment in the db
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}