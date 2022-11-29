package org.wildfly.managed;

import org.wildfly.managed.common.model.DatabaseConnection;
import org.wildfly.managed.parser.FormattingXMLStreamWriter;
import org.wildfly.managed.parser.serverconfig.ServerConfig;
import org.wildfly.managed.parser.serverconfig.ServerConfigParser;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigAdjuster {
    List<String> commands = new ArrayList<>();
    Set<String> layers = new HashSet<>();

    public ConfigAdjuster adjustConfig(ApplicationRepo applicationRepo, String appName) {
        List<DatabaseConnection> connections = applicationRepo.getDatabaseConnections(appName);

        // grab the layers
        Set<String> databaseLayers = connections.stream().map(c -> c.type.layer).collect(Collectors.toSet());
        layers.addAll(databaseLayers);

        // Add the drivers first
        Set<DatabaseConnection.Type> types = connections.stream().map(c -> c.type).collect(Collectors.toSet());
        for (DatabaseConnection.Type type : types) {
            addDriver(type);
        }

        // Now add the datasources

        for (DatabaseConnection connection : connections) {
            addDatasource(connection);
        }

        return this;
    }


    private void addDatasource(DatabaseConnection connection) {
        // TODO make this configurable - or always do xa? For now just do datasource
        String datasourceType = true ? "data-source" : "xa-data-source";
        String command = "/subsystem=datasources/%s=\"%s\":add(jndi-name=\"%s\", user-name=\"%s\", password=\"%s\", driver-name=\"%s\", connection-url=\"%s\")";
        command = String.format(command, datasourceType, connection.jndiName, connection.jndiName, connection.username, connection.password, connection.type.toString(), connection.url);
        commands.add(command);
    }

    private void addDriver(DatabaseConnection.Type type) {
        String command = "/subsystem=datasources/jdbc-driver=%s:add(driver-name=\"%s\", driver-module-name=\"%s\", driver-xa-datasource-class-name=\"%s\")";
        command = String.format(command, type, type, type.module, type.xaDataSourceClass);
        commands.add(command);
    }

    public void updateConfigs(Path applicationDir) {
        updateServerConfigXml(applicationDir.resolve("server-config.xml"));
        updateServerInitCli(applicationDir.resolve("server-init.cli"));
    }

    private void updateServerConfigXml(Path serverConfigXml) {
        try {
            ServerConfigParser serverConfigParser = new ServerConfigParser(serverConfigXml);
            ServerConfig serverConfig = serverConfigParser.parse();
            serverConfig.getLayers().mergeLayers(layers);

            // Write the updated pom
            FormattingXMLStreamWriter writer =
                    new FormattingXMLStreamWriter(
                            XMLOutputFactory.newInstance().createXMLStreamWriter(
                                    new BufferedWriter(
                                            new FileWriter(serverConfigXml.toFile()))));
            try {
                serverConfig.marshall(writer);
            } finally {
                writer.close();
            }
        } catch (IOException | XMLStreamException e) {
            e.printStackTrace();
            throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void updateServerInitCli(Path serverInitCli) {
        if (commands.size() == 0) {
            // Nothing to do
            return;
        }
        try {
            System.out.println(serverInitCli.toAbsolutePath());
            Path backup = null;
            List<String> userCommands = new ArrayList<>();
            if (Files.exists(serverInitCli)) {
                userCommands = Files.readAllLines(serverInitCli);
                backup = serverInitCli.getParent().resolve(serverInitCli.getFileName().toString() + ".bak");
                if (Files.exists(backup)) {
                    Files.delete(backup);
                }
                Files.move(serverInitCli, backup);
            }


            List<String> commands = new ArrayList<>();
            commands.add("batch");
            commands.addAll(this.commands);
            // TODO Validate user commands are not batched (or possibly remove the batch commands?)
            commands.addAll(userCommands);
            commands.add("run-batch");

            try {
                Files.write(serverInitCli, commands);
            } catch (IOException e) {
                try {
                    if (!Files.exists(serverInitCli) && backup != null && Files.exists(backup)) {
                        Files.move(backup, serverInitCli);
                    }
                } catch (IOException ignore) {
                }
            }
        } catch (IOException e) {
            throw new ServerException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public static void main(String[] args) {
        Path path = Paths.get("/Users/kabir/temp/server-config.xml");
        ConfigAdjuster adjuster = new ConfigAdjuster();
        adjuster.layers.add("test");
        adjuster.updateServerConfigXml(path);

    }
}
