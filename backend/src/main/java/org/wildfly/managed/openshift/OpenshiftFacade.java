package org.wildfly.managed.openshift;

import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftHandlers;
import org.wildfly.managed.ServerException;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.util.Constants;
import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.repo.ApplicationConfigs;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wildfly.managed.common.util.Constants.SERVER_CONFIG_XML;
import static org.wildfly.managed.common.util.Constants.SERVER_INIT_CLI;
import static org.wildfly.managed.common.util.Constants.SERVER_INIT_YML;

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
            throw new ServerException(Response.Status.CONFLICT, "Cannot deploy application since it has no archives added.");
        }

        outputConfigFilesToAppDirectory(appName);

        // TODO - check we're not already running a build. We'll have to check all the builds

        Path appDir = uiPaths.getApplicationDir(appName);
        File tarBall = Packaging.packageFile(appDir, appDir);
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
            try {
                deleteIfExists(appDir.resolve(SERVER_CONFIG_XML), true);
                deleteIfExists(appDir.resolve(SERVER_CONFIG_XML), true);
                deleteIfExists(appDir.resolve(SERVER_CONFIG_XML), true);
            } catch (IOException ignore) {
                // Won't happen since we swallow it in the deleteIfExists call
            }
        }
        System.out.println("Started build " + build.getFullResourceName());
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
