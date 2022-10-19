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

    private Path workingDir;
    private Path scriptsDir;

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
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public Path getApplicationDir(String appName) {
        try {
            Path path = workingDir.resolve(appName);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
