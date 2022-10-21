package org.wildfly.cli;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.managed.common.model.Application;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Dependent
public class CliContext {

    @RestClient
    ApplicationService applicationService;

    private Path contextDir;
    private Map<ContextKey, String> values = new HashMap<>();


    String activeApp;

    @PostConstruct
    public void init() throws Exception {
        String userHome = System.getProperty("user.home");
        Path path = Paths.get(userHome).resolve(".managed-server");
        contextDir = path;
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        } else {
            for (ContextKey key : ContextKey.values()) {
                Path valuePath = contextDir.resolve(key.fileName);
                if (Files.exists(valuePath)) {
                    byte[] contents = Files.readAllBytes(valuePath);
                    String value = new String(contents, StandardCharsets.UTF_8);
                    values.put(key, value);
                }
            }
        }

        String activeApp = getActiveApp();
        if (activeApp != null) {
            boolean foundActiveApp = false;
            for (Application application : applicationService.list()) {
                if (application.name.equals(activeApp)) {
                    foundActiveApp = true;
                    break;
                }
            }
            if (!foundActiveApp) {
                System.out.println("Clearing active application. It was set to '" + activeApp + "', which no longer exists.");
                setActiveApp(null);
            }
        }
    }

    public String getActiveApp() {
        return values.get(ContextKey.ACTIVE_APP);
    }

    public void setActiveApp(String app) {
        setValue(ContextKey.ACTIVE_APP, app);
    }


    private void setValue(ContextKey key, String value) {
        Path path = contextDir.resolve(key.fileName);
        try {
            if (value == null) {
                Files.delete(path);
            } else {
                Files.write(path, value.getBytes(StandardCharsets.UTF_8));
            }
            values.put(key, value);
        } catch (IOException e) {
            throw new IllegalStateException("Error setting value", e);
        }
    }

    private enum ContextKey {
        ACTIVE_APP("active-app");

        private final String fileName;

        ContextKey(String fileName) {
            this.fileName = fileName;
        }

    }

}
