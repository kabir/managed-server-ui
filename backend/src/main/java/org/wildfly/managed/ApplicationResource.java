package org.wildfly.managed;

import org.jboss.resteasy.reactive.MultipartForm;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
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
        System.out.println(application.name);
        return applicationRepo.create(application);
    }

    @DELETE
    @Path("/{appName}")
    @Transactional
    public Response delete(String appName) {
        Application application = applicationRepo.findByName(appName);
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        applicationRepo.delete(appName);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/{appName}/deploy")
    public Response deploy(String appName) {
        try {
            Application application = applicationRepo.findByName(appName);
            if (application == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            java.nio.file.Path appDir = uiPaths.getApplicationDir(appName);
            java.nio.file.Path scriptDir = uiPaths.getScriptsDir();
            java.nio.file.Path createApplicationScript =
                    uiPaths.getScriptsDir().resolve(CREATE_APPLICATION_SCRIPT);


            ProcessBuilder pb = new ProcessBuilder("./" + CREATE_APPLICATION_SCRIPT, appName, "", appDir.toString());
            pb.directory(scriptDir.toFile());
            pb.start();

            return Response.status(Response.Status.ACCEPTED).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{appName}/archive")
    public List<AppArchive> listArchives(String appName) {
        return Collections.emptyList();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive")
    public Response uploadArchive(String appName, @MultipartForm DeploymentData data) {
        UploadedFileContext checker = new UploadedFileContext(appName, data);
        Response response = checker.init();
        if (response == null) {
            return response;
        }

        applicationRepo.saveApplicationArchive(checker.application, checker.archiveName, checker.configFileInspection);

        return Response.status(Response.Status.ACCEPTED).build();

    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive/{archiveName}")
    public Response replaceArchive(String appName, String archiveName, @MultipartForm DeploymentData data) {
        UploadedFileContext checker = new UploadedFileContext(appName, archiveName, data);
        Response response = checker.init();
        if (response == null) {
            return response;
        }

        applicationRepo.saveApplicationArchive(checker.application, checker.archiveName, checker.configFileInspection);

        return Response.status(Response.Status.ACCEPTED).build();
    }

    @DELETE
    @Path("/{appName}/archive/{archiveName}")
    public Response deleteArchive(String appName, String archiveName) {
        Application application = applicationRepo.findByName(appName);
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }

    private class UploadedFileContext {
        private final String appName;
        private String archiveName;
        private final DeploymentData data;
        private Application application;
        private java.nio.file.Path dest;
        private ConfigFileInspection configFileInspection;

        UploadedFileContext(String appName, @MultipartForm DeploymentData data) {
            this(appName, null, data);
        }

        UploadedFileContext(String appName, String archiveName, @MultipartForm DeploymentData data) {
            this.appName = appName;
            this.archiveName = archiveName;
            this.data = data;
        }

        private Response init() {
            application = applicationRepo.findByName(appName);
            if (application == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            java.nio.file.Path path = data.file.uploadedFile();

            if (!data.file.fileName().endsWith(".war")) {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
            }

            String fileName = data.file.fileName();
            if (archiveName != null && !archiveName.equals(fileName)) {
                return Response.status(Response.Status.CONFLICT).build();
            }
            archiveName = fileName;
            dest = uiPaths.getApplicationDir(appName).resolve(fileName);
            try {
                if (Files.exists(dest)) {
                    // TODO: This will do for now to avoid the FileAlreadyExistsException. Will probably need some delete/update commands
                    Files.delete(dest);
                }
                Files.move(data.file.uploadedFile(), dest);
                configFileInspection = ConfigFileInspection.inspect(dest);
            } catch (IOException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return null;
        }
    }
}