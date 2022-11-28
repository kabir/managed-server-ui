package org.wildfly.cli.command;

import org.wildfly.cli.util.TableRenderer;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.DatabaseConnection;
import org.wildfly.managed.common.value.AppState;

import java.util.List;

class Renderers {
    static final String INDENT = "  ";

    static void renderAppStatus(AppState appStatus) {
        System.out.println("Deployment: " + appStatus.getDeploymentState());
        System.out.println("Build: " + appStatus.getBuildState());
        System.out.println("Stage: " + appStatus.getStageState());
    }

    static void renderAppArchives(List<AppArchive> appArchives) {
        if (appArchives.size() == 0) {
            System.out.println(INDENT + "None");
        } else {
            TableRenderer outputter = TableRenderer.builder()
                    .addColumn(30, "Archive")
                    .addColumn(7, "Has XML")
                    .addColumn(7, "Has CLI")
                    .addColumn(8, "Has YAML")
                    .build();
            for (AppArchive appArchive : appArchives) {
                outputter.addRow()
                        .addColumns(
                                appArchive.fileName,
                                appArchive.serverConfigXml ? "*" : "",
                                appArchive.serverInitCli ? "*" : "",
                                appArchive.serverInitYml ? "*" : ""
                        )
                        .output();

            }
        }
    }

    static void renderDatabaseConnections(List<DatabaseConnection> connections) {
        System.out.println("----> " + connections.size());
        if (connections.size() == 0) {
            System.out.println(INDENT + "None");
        } else {
            TableRenderer outputter = TableRenderer.builder()
                    .addColumn( 20, "JNDI Name")
                    .addColumn(20, "Database Name")
                    .addColumn(15, "User")
                    .addColumn(30, "URL")
                    .build();
            for (DatabaseConnection connection : connections) {
                outputter.addRow()
                        .addColumns(
                                connection.jndiName,
                                connection.databaseName,
                                connection.username,
                                connection.url
                        )
                        .output();
            }
        }
    }
}
