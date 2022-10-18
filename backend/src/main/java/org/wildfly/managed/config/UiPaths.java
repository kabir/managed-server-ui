package org.wildfly.managed.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class UiPaths {

    @ConfigProperty(name = "managed.server.ui.backend.workdir")
    String workingDirName;

    private Path workingDir;

    @PostConstruct
    public void init() throws Exception {
        workingDir = Paths.get(workingDirName).toAbsolutePath();
        if (!Files.exists(workingDir)) {
            Files.createDirectories(workingDir);
        }
    }

    public Path getWorkingDir() {
        return workingDir;
    }
}
