package org.wildfly.cli.context;

import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.managed.common.model.Application;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Dependent
public class CliContext {

    private Path contextDir;
    private Map<ContextKey, String> values = new HashMap<>();

    private boolean initialised = false;

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
            if (getServerBackEndUri() == null) {
                System.out.println("Not connected to server. Clearing active application " + activeApp);
                setActiveApp(null);
            } else {
                ApplicationService applicationService = ApplicationService.createInstance(this);
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

        initialised = true;
    }

    public boolean isInitialised() {
        return initialised;
    }

    public String getActiveApp() {
        return values.get(ContextKey.ACTIVE_APP);
    }

    public void setActiveApp(String app) {
        setValue(ContextKey.ACTIVE_APP, app);
    }

    public URI getServerBackEndUri() {
        return fromString(values.get(ContextKey.SERVER_BACKEND_URI), s -> URI.create(s));
    }

    public void setServerBackEndUri(String s) {
        if (s != null) {
            try {
                URI.create(s);
            } catch (Exception e) {
                System.err.println(s + " is not a valid uri");
                System.exit(1);
            }
        }
        setValue(ContextKey.SERVER_BACKEND_URI, s);
    }

    private <T> T fromString(String value, Function<String, T> converter) {
        if (value == null) {
            return null;
        }
        return converter.apply(value);
    }

    private <T> String toString(T value, Function<T, String> converter) {
        if (value == null) {
            return null;
        }
        return converter.apply(value);
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
        ACTIVE_APP("active-app"),
        SERVER_BACKEND_URI("server-backend-uri");

        private final String fileName;

        ContextKey(String fileName) {
            this.fileName = fileName;
        }
    }

}
