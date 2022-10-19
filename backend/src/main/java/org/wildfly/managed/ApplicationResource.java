package org.wildfly.managed;

import org.jboss.resteasy.reactive.MultipartForm;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/app")
public class ApplicationResource {

    @Inject
    UiPaths applicationPaths;

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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("upload")
    void tempUpload(@MultipartForm DeploymentData data) {
        java.nio.file.Path path = data.file.uploadedFile();
        System.out.println("----> " + path);
    }

    @GET
    @Path("temp")
    void temp() {
        System.out.println("----> TEMP!!!");
    }

}