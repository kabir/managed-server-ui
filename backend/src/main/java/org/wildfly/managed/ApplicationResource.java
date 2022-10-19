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
    private static final String CREATE_APPLICATION_SCRIPT = "create-application.sh";
    private static final String UPDATE_APPLICATION_SCRIPT = "update-application.sh";

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

            java.nio.file.Path dest = uiPaths.getApplicationDir(name).resolve(data.file.fileName());
            if (Files.exists(dest)) {
                // TODO: This will do for now to avoid the FileAlreadyExistsException. Will probably need some delete/update commands
                Files.delete(dest);
            }
            Files.move(data.file.uploadedFile(), dest);

            // TODO record the deployment in the db

            return Response.status(Response.Status.ACCEPTED).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/{name}/deploy")
    public Response deploy(String name) {
        try {
            Application application = applicationRepo.findByName(name);
            if (application == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            java.nio.file.Path appDir = uiPaths.getApplicationDir(name);
            java.nio.file.Path scriptDir = uiPaths.getScriptsDir();
            java.nio.file.Path createApplicationScript =
                    uiPaths.getScriptsDir().resolve(CREATE_APPLICATION_SCRIPT);


//            #!/bin/sh
//
//            appName="${1}"
//            helmChart="${2}"
//            appFolder="${3}"
            ProcessBuilder pb = new ProcessBuilder("./" + CREATE_APPLICATION_SCRIPT, name, "", appDir.toString());
            pb.directory(scriptDir.toFile());
            pb.start();

            return Response.status(Response.Status.ACCEPTED).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}