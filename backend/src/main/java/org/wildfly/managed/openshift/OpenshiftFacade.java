package org.wildfly.managed.openshift;

import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftHandlers;
import org.wildfly.managed.ServerException;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class OpenshiftFacade {
    private static final String CREATE_APPLICATION_SCRIPT = "create-application.sh";
    private static final String UPDATE_APPLICATION_SCRIPT = "update-application.sh";

    @Inject
    ApplicationRepo applicationRepo;

    @Inject
    OpenShiftClient openShiftClient;

    @Inject
    UiPaths uiPaths;


    public void deploy(String appName) {

        runScript(appName, CREATE_APPLICATION_SCRIPT, appName, "");

        List<AppArchive> archives = applicationRepo.listArchivesForApp(appName);
        if (archives.size() == 0) {
            throw new ServerException(Response.Status.CONFLICT, "Cannot deploy application since it has no archives");
        }

        // Create the chart
        java.nio.file.Path appDir = uiPaths.getApplicationDir(appName);

        // TODO - check we're not already running a build. We'll have to check all the builds

        Path path = uiPaths.getApplicationDir(appName);
        File tarBall = Packaging.packageFile(path, path);
        Build build;
        try {
            build = openShiftClient.buildConfigs()
                    .inNamespace("kkhan1-dev")
                    .withName(appName + "-deployment-build")
                    .instantiateBinary()
                    .fromFile(tarBall);
        } finally {
            try {
                Files.delete(Paths.get(tarBall.toURI()));
            } catch (IOException nonFatal) {
                System.out.println("Could not delete temporary tarball for app '" + appName + "' " + tarBall.getAbsolutePath() + ". Message: " + nonFatal.getMessage());
            }
        }
        System.out.println("Started build " + build.getFullResourceName());
    }

    private void runScript(String appName, String script, String... arguments) {
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

}
