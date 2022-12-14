package org.wildfly.managed;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.model.DatabaseConnection;
import org.wildfly.managed.common.model.DeploymentRecord;
import org.wildfly.managed.common.value.AppState;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.openshift.OpenshiftFacade;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.inject.Inject;
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


    @Inject
    UiPaths uiPaths;

    @Inject
    ApplicationRepo applicationRepo;

    @Inject
    OpenshiftFacade openshiftFacade;

    @GET
    public List<Application> list() {
        return applicationRepo.listAll();
    }

    @GET
    @Path("/{name}")
    public Application get(String name, @QueryParam("verbose") boolean verbose) {
        return applicationRepo.getApplication(name, verbose);
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
    public void delete(String appName, @QueryParam("force") Boolean force) {
        boolean forceDelete = force == null ? false : force;
        try {
            applicationRepo.findByName(appName);
            AppState state = openshiftFacade.getStatus(appName);
            if (!forceDelete && (state.getDeploymentState() != AppState.DeploymentState.NOT_DEPLOYED || state.getBuildState() == AppState.BuildState.RUNNING)) {
                throw new ServerException(Response.Status.CONFLICT, "Can't delete a running application, or one in the process of being built. Stop it first, or force delete. If the status shows it is being built and you want to keep the application running, cancel the deploy.");
            }
            applicationRepo.delete(appName);
            openshiftFacade.delete(appName);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ResponseStatus(204) // NO_CONTENT
    @PUT
    @Path("/{appName}/stop")
    public void stop(String appName) {
        AppState.BuildState buildState = null;
        try {
            applicationRepo.findByName(appName);
            buildState = openshiftFacade.stop(appName);
            DeploymentRecord.Status state = null;
            if (buildState == AppState.BuildState.RUNNING || buildState == AppState.BuildState.NOT_RUNNING) {
                applicationRepo.recordDeploymentEnd(appName, DeploymentRecord.Status.CANCELLED);
                // Don't think we need to handle Completed/Failed since that should have been recorded elsewhere. Probably/possibly...
            }

        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
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
            applicationRepo.deleteConfigFileContents(appName, type);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @ResponseStatus(202) // ACCEPTED
    @POST
    @Path("/{appName}/deploy")
     public void deploy(String appName, @QueryParam("force") Boolean force, @QueryParam("refresh") Boolean refresh, @QueryParam("cancel") Boolean cancel) {
        try {
            boolean forceBuild = force == null ? false : force;
            boolean refreshBuild = refresh == null ? false : refresh;
            boolean cancelBuild = cancel == null ? false : cancel;
            // Check application exists
            System.out.println("----> Looking for app " + appName);
            if (!cancelBuild) {
                try {
                    System.out.println("----> Calling deploy " + appName);
                    openshiftFacade.deploy(appName, forceBuild, refreshBuild);
                } catch (RuntimeException e) {
                    throw e;
                }
            } else {
                openshiftFacade.cancelBuild(appName);
            }
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
        }
    }

    @GET
    @Path("/{appName}/status")
    public AppState status(String appName) {
        try {
            // Check application exists
            applicationRepo.findByName(appName);
            return openshiftFacade.getStatus(appName);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
            return null;
        }
    }

    @GET
    @Path("/{appName}/routes")
    public List<String> routes(String appName) {
        try {
            // Check application exists
            applicationRepo.findByName(appName);
            return openshiftFacade.getRoutes(appName);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ServerException.class, () -> (ServerException) e)
                    .throwServerException(e);
            return null;
        }
    }

    @POST
    @Path("/{appName}/db")
    public void createDatabaseConnection(String appName, DatabaseConnection dbConn) {
        try {
            applicationRepo.createDatabaseConnection(appName, dbConn);
        } catch (RuntimeException e) {
            ExceptionUnwrapper
                    .create(ConstraintViolationException.class,
                            () -> new ServerException(Response.Status.CONFLICT, "There is already a database connection with the JNDI name: " + dbConn.jndiName))
                    .throwServerException(e);
            throw e;
        }

    }

    @DELETE
    @Path("/{appName}/db/{jndiName}")
    public void deleteDatabaseConnection(String appName, String jndiName) {
        applicationRepo.deleteDatabaseConnection(appName, jndiName);
    }

    @GET
    @Path("/{appName}/db")
    public List<DatabaseConnection> listDatabaseConnections(String appName) {
        return applicationRepo.getDatabaseConnections(appName);
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