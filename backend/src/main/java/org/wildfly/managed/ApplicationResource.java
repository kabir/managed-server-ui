package org.wildfly.managed;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.wildfly.managed.common.util.Constants.WEB_ERROR_DESCRIPTION_HEADER_NAME;

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
    @ResponseStatus(201) // CREATED
    public Application create(Application application) {
        try {
            return applicationRepo.create(application);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ConstraintViolationException.class,
                            () -> new ServerException(Response.Status.CONFLICT, "There is already an application called: " + application.name))
                    .throwServerException(e);
            throw e;
        }
    }

    @ResponseStatus(204) // NO_CONTENT
    @DELETE
    @Path("/{appName}")
    @Transactional
    public void delete(String appName) {
        try {
            applicationRepo.delete(appName);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ResponseStatus(202) // ACCEPTED
    @POST
    @Path("/{appName}/deploy")
    public void deploy(String appName) {
        try {
            Application application = applicationRepo.findByName(appName);
            java.nio.file.Path appDir = uiPaths.getApplicationDir(appName);
            java.nio.file.Path scriptDir = uiPaths.getScriptsDir();
            java.nio.file.Path createApplicationScript =
                    uiPaths.getScriptsDir().resolve(CREATE_APPLICATION_SCRIPT);

            ProcessBuilder pb = new ProcessBuilder("./" + CREATE_APPLICATION_SCRIPT, appName, "", appDir.toString());
            pb.directory(scriptDir.toFile());
            pb.start();
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/{appName}/archive")
    public List<AppArchive> listArchives(String appName) {
        try {
            return applicationRepo.listArchivesForApp(appName);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
            return null;
        }
    }

    @ResponseStatus(201) // CREATED
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive")
    public void addArchive(String appName, @MultipartForm DeploymentData data) {
        try {
            System.out.println("--- Add");
            UploadedFileContext checker = new UploadedFileContext(appName, data);
            checker.init();
            applicationRepo.createApplicationArchive(checker.application, checker.archiveName, checker.configFileInspection);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ResponseStatus(202) // ACCEPTED
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/archive/{archiveName}")
    public void replaceArchive(String appName, String archiveName, @MultipartForm DeploymentData data) {
        try {
            System.out.println("--- Replace");
            UploadedFileContext checker = new UploadedFileContext(appName, archiveName, data);
            checker.init();
            applicationRepo.updateApplicationArchive(checker.application, checker.archiveName, checker.configFileInspection);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ResponseStatus(204) // NO_CONTENT
    @DELETE
    @Path("/{appName}/archive/{archiveName}")
    public void deleteArchive(String appName, String archiveName) {
        Application application = applicationRepo.findByName(appName);
        applicationRepo.deleteApplicationArchive(application, archiveName);
    }

    @GET
    @Path("/{appName}/config-file")
    public String getConfigFileContents(String appName, @QueryParam("type") String type) {
        System.out.println("---> get config");
        try {
            return applicationRepo.getConfigFileContents(appName, type);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
            return null;
        }
    }

    @ResponseStatus(204) // NO_CONTENT
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{appName}/config-file")
    public void setConfigFileContents(String appName, @QueryParam("type") String type, @MultipartForm DeploymentData dto) {
        System.out.println("---> replace config");
        try {
            String contents = null;
            try {
                contents = Files.readString(dto.file.uploadedFile());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            System.out.println("---> " + contents);
            applicationRepo.setConfigFileContents(appName, type, contents);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ResponseStatus(204) // NO_CONTENT
    @DELETE
    @Path("/{appName}/config-file")
    public void deleteConfigFileContents(String appName, @QueryParam("type") String type) {
        try {
            System.out.println("---> delete config");
            applicationRepo.deleteConfigFileContents(appName, type);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ServerExceptionMapper
    RestResponse<Object> mapException(ServerException e) {
        return RestResponse.ResponseBuilder
                // Sets the exception message in the body, but that is ignored on the client
                .create(e.getStatus().getStatusCode(), e.getMessage())
                // ... so try setting it in a header
                .header(WEB_ERROR_DESCRIPTION_HEADER_NAME, e.getMessage())
                .build();
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

        private void init() {
            application = applicationRepo.findByName(appName);

            java.nio.file.Path path = data.file.uploadedFile();

            if (!data.file.fileName().endsWith(".war")) {
                throw new ServerException(Response.Status.UNSUPPORTED_MEDIA_TYPE, "Only .war archives can be uploaded.");
            }

            String fileName = data.file.fileName();
            if (archiveName != null && !archiveName.equals(fileName)) {
                throw new ServerException(Response.Status.CONFLICT, "Bad request, the archive name should match the actual file name.");
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
                throw new RuntimeException("An error happened copying the files on the server");
            }
        }
    }
}