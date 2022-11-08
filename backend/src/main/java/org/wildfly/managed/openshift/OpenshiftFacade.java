package org.wildfly.managed.openshift;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildList;
import io.fabric8.openshift.api.model.BuildStatus;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.managed.ServerException;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.value.AppState;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.repo.ApplicationConfigs;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.wildfly.managed.common.util.Constants.SERVER_CONFIG_XML;
import static org.wildfly.managed.common.util.Constants.SERVER_INIT_CLI;
import static org.wildfly.managed.common.util.Constants.SERVER_INIT_YML;

@ApplicationScoped
public class OpenshiftFacade {
    private static final String INSTALL_HELM_SCRIPT = "install-helm.sh";
    private static final String UNINSTALL_HELM_SCRIPT = "uninstall-helm.sh";

    private static final String OPENSHIFT_LOGIN = "openshift-login.sh";

    @Inject
    ApplicationRepo applicationRepo;

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    UiPaths uiPaths;

    @ConfigProperty(name = "managed.server.openshift.do.login", defaultValue = "false")
    boolean loginToOpenshift;

    @ConfigProperty(name = "managed.server.openshift.token")
    Optional<String> openshiftToken;

    @ConfigProperty(name = "managed.server.openshift.token")
    Optional<String> openshiftServer;


    @PostConstruct
    public void ensureLoggedIn() {
        if (loginToOpenshift) {
            String server = openshiftServer.orElseThrow();
            String token = openshiftToken.orElseThrow();
            runScript(OPENSHIFT_LOGIN, server, token);
        }
        // TODO do we need to log in periodically?
    }

    public String deploy(String appName, boolean force, boolean refresh) {

        // TODO eventually we should install the Helm chart via `helm repository add`

        runScript(INSTALL_HELM_SCRIPT, appName, uiPaths.getTempHelmChart().toString());

        List<AppArchive> archives = applicationRepo.listArchivesForApp(appName);
        if (archives.size() == 0) {
            throw new ServerException(Response.Status.CONFLICT, "Cannot deploy application since it has no archives added.");
        }

        if (refresh) {
            AppState.DeploymentState deploymentState = getDeploymentStatus(appName);
            if (deploymentState != AppState.DeploymentState.RUNNING) {
                throw new ServerException(Response.Status.CONFLICT, "Can only refresh a running application");
            }
            // TODO should check the configs haven't changed and error if they have.
        }

        if (hasRunningBuilds(appName)) {
            if (!force) {
                throw new ServerException(Response.Status.CONFLICT, "The application is currently building. You can stop the build by doing a force deploy");
            } else {
                // It would be nice to cancel the running builds here but I don't see how to just cancel.
                // The delete we are doing later
            }
        }

        deleteAllBuilds(appName);

        outputConfigFilesToAppDirectory(appName);

        Path appDir = uiPaths.getApplicationDir(appName);
        File tarBall = Packaging.packageFile(appDir, appDir);
        Build build;
        try {
            String buildName = !refresh ? appName + "-deployment-build" : appName + "-update-build";

            build = openShiftClient.buildConfigs()
                    .inNamespace("kkhan1-dev")
                    .withName(buildName)
                    .instantiateBinary()
                    .fromFile(tarBall);
        } finally {
            try {
                Files.delete(Paths.get(tarBall.toURI()));
            } catch (IOException nonFatal) {
                System.out.println("Could not delete temporary tarball for app '" + appName + "' " + tarBall.getAbsolutePath() + ". Message: " + nonFatal.getMessage());
            }
            try {
                deleteIfExists(appDir.resolve(SERVER_CONFIG_XML), true);
                deleteIfExists(appDir.resolve(SERVER_CONFIG_XML), true);
                deleteIfExists(appDir.resolve(SERVER_CONFIG_XML), true);
            } catch (IOException ignore) {
                // Won't happen since we swallow it in the deleteIfExists call
            }
        }
        return build.getMetadata().getName();
    }

    private void outputConfigFilesToAppDirectory(String appName) {
        Application application = applicationRepo.findByName(appName);
        Path appDir = uiPaths.getApplicationDir(appName);
        if (application.hasServerConfigXml || application.hasServerInitYml || application.hasServerInitCli) {
            ApplicationConfigs configs = applicationRepo.getConfigFileContents(appName);
            try {
                if (application.hasServerConfigXml) {
                    Path file = appDir.resolve(SERVER_CONFIG_XML);
                    deleteIfExists(file, false);
                    Files.write(file, configs.getXml().getBytes(StandardCharsets.UTF_8));
                }
                if (application.hasServerInitCli) {
                    Path file = appDir.resolve(SERVER_INIT_CLI);
                    deleteIfExists(file, false);
                    Files.write(appDir.resolve(file), configs.getCli().getBytes(StandardCharsets.UTF_8));
                }
                if (application.hasServerInitYml) {
                    Path file = appDir.resolve(SERVER_INIT_YML);
                    deleteIfExists(file, false);
                    Files.write(appDir.resolve(file), configs.getYml().getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR, "Error saving config files to " + appDir + ". " + e.getMessage());
            }
        }
    }

    private void deleteIfExists(Path path, boolean swallowException) throws IOException {
        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                if (swallowException) {
                    e.printStackTrace();
                } else {
                    throw e;
                }
            }
        }
    }

    private void runScript(String script, String... arguments) {
        java.nio.file.Path scriptDir = uiPaths.getScriptsDir();
        java.nio.file.Path scriptPath =
                uiPaths.getScriptsDir().resolve(script).toAbsolutePath();
        if (!Files.exists(scriptPath)) {
            throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR, "Missing " + scriptPath + ". Notify the service admins");
        }

        List<String> command = new ArrayList<>();
        command.add(scriptPath.toString());
        command.addAll(Arrays.asList(arguments));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(scriptDir.toFile());
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        try {
            int exit = process.waitFor();
            System.out.println("Exit code: " + exit);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public AppState getStatus(String appName) {
        AppState.DeploymentState deploymentState = getDeploymentStatus(appName);
        AppState.BuildState buildState = getBuildState(appName);


        if (deploymentState == AppState.DeploymentState.RUNNING) {
            RouteList routes = openShiftClient.routes().withLabel("app", appName).list();
            List<String> appRoutes = new ArrayList<>();
            for (Route route : routes.getItems()) {
                // TODO get the route information
            }
        }
        return new AppState(deploymentState, buildState);
    }

    public void delete(String appName) {
        openShiftClient.apps().deployments().withLabel("app", appName).delete();
        runScript(UNINSTALL_HELM_SCRIPT, appName);
    }

    public void stop(String appName) {
        openShiftClient.builds().withLabel("app", appName).delete();
        openShiftClient.apps().deployments().withLabel("app", appName).delete();
    }

    private AppState.DeploymentState getDeploymentStatus(String appName) {
        Deployment deployment = openShiftClient.apps().deployments().withName(appName).get();
        if (deployment == null) {
            return AppState.DeploymentState.NOT_DEPLOYED;
        }
        DeploymentStatus status = deployment.getStatus();
        if (status.getReplicas().equals(status.getReadyReplicas())) {
            // Is this the right check?
            return AppState.DeploymentState.RUNNING;
        }

        // TODO needs more tightening up. There might be errors etc, or scaling considerations etc.
        return AppState.DeploymentState.DEPLOYING;
    }

    private AppState.BuildState getBuildState(String appName) {
        Application app = applicationRepo.findByName(appName);
        BuildList buildList = openShiftClient.builds().withLabel("app", appName).list();

        // TODO don't include old builds
        System.out.println("Checking build statuses");
        for (Build build : buildList.getItems()) {
            BuildStatus status = build.getStatus();
            String start = status.getStartTimestamp();
            String completion = status.getCompletionTimestamp();
            if (start != null && completion == null) {
                return AppState.BuildState.RUNNING;
            }
            if (start == null && completion == null) {
                // This really means the build is pending. For the purposes of the CLI, a soon to start
                // build indicates the overall build is running.
                return AppState.BuildState.RUNNING;
            }
            // TODO how to find errors?
        }

        // TODO figure out how to determine the state of each build from their BuildStatus
        return AppState.BuildState.COMPLETED;
    }

    private boolean hasRunningBuilds(String appName) {
        for (Build build : openShiftClient.builds().withLabel("app", appName).list().getItems()) {
            BuildStatus status = build.getStatus();
            if (status.getStartTimestamp() == null) {
                return true;
            }
            if (status.getStartTimestamp() != null && status.getCompletionTimestamp() == null) {
                return true;
            }
        }
        return false;
    }

    private void deleteAllBuilds(String appName) {
        openShiftClient.builds().withLabel("app", appName).delete();
    }

}
