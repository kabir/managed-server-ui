package org.wildfly.managed.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class UiPaths {

    @ConfigProperty(name = "managed.server.ui.backend.workdir")
    String workingDirName;

    @ConfigProperty(name = "managed.server.ui.backend.scriptsdir")
    String scriptsDirName;

    // We should eventually install the Helm chart as a repository
    @ConfigProperty(name = "managed.server.helm.chart.location", defaultValue = "/scripts/managed-wildfly-chart-0.1.0.tgz")
    String tempHelmChartLocationName;

    private Path workingDir;
    private Path scriptsDir;
    private Path tempHelmChartLocation;

    @PostConstruct
    public void init() throws Exception {
        workingDir = Paths.get(workingDirName).toAbsolutePath();
        if (!Files.exists(workingDir)) {
            Files.createDirectories(workingDir);
        }

        scriptsDir = Paths.get(scriptsDirName).toAbsolutePath();
        if (!Files.exists(scriptsDir)) {
            throw new IllegalStateException("Scripts dir does not exist: " + scriptsDir);
        }

        tempHelmChartLocation = Paths.get(tempHelmChartLocationName).toAbsolutePath();
        if (!Files.exists(tempHelmChartLocation)) {
            throw new IllegalStateException("Helm chart does not exist: " + tempHelmChartLocation);
        }

        System.out.println("-> workingDir " + workingDir);
        System.out.println("-> scriptsDir " + scriptsDir);
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public Path getApplicationDir(String appName) {
        try {
            Path path = workingDir.resolve(appName).toAbsolutePath();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getScriptsDir() {
        return scriptsDir;
    }

    public Path getTempHelmChart() {
        return tempHelmChartLocation;
    }
}
