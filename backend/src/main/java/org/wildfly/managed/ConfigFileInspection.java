package org.wildfly.managed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

public class ConfigFileInspection {
    public static final String ROOT_LOCATION = "WEB-INF/classes/META-INF/";
    public static final String SERVER_CONFIG_XML = ROOT_LOCATION + "server-config.xml";
    public static final String SERVER_INIT_CLI = ROOT_LOCATION + "server-init.cli";
    public static final String SERVER_INIT_YML = ROOT_LOCATION + "server-init.yml";


    private final boolean serverConfigXml;

    private final boolean serverInitCli;

    private final boolean serverInitYml;

    private ConfigFileInspection(boolean serverConfigXml, boolean serverInitCli, boolean serverInitYml) {
        this.serverConfigXml = serverConfigXml;
        this.serverInitCli = serverInitCli;
        this.serverInitYml = serverInitYml;
    }

    public boolean isServerConfigXml() {
        return serverConfigXml;
    }

    public boolean isServerInitCli() {
        return serverInitCli;
    }

    public boolean isServerInitYml() {
        return serverInitYml;
    }

    public boolean hasConfigFiles() {
        return serverConfigXml || serverInitCli || serverInitYml;
    }

    static ConfigFileInspection inspect(Path archiveFile) throws IOException {
        AtomicBoolean serverConfigXml = new AtomicBoolean(false);
        AtomicBoolean serverInitCli = new AtomicBoolean(false);
        AtomicBoolean serverInitYml = new AtomicBoolean(false);

        JarFile jar = new JarFile(archiveFile.toFile());
        System.out.println("=== Inspecting");
        jar.stream().forEach(je -> {
            switch (je.getName()) {
                case SERVER_CONFIG_XML:
                    serverConfigXml.set(true);
                    break;
                case SERVER_INIT_CLI:
                    serverInitCli.set(true);
                    break;
                case SERVER_INIT_YML:
                    serverInitYml.set(true);
                    break;
            }
            System.out.println(je.getName());
        });

        System.out.printf("=== Inspecting %s, %s, %s", serverConfigXml.get(), serverInitCli.get(), serverInitYml.get());

        return new ConfigFileInspection(serverConfigXml.get(), serverInitCli.get(), serverInitYml.get());
    }
}
