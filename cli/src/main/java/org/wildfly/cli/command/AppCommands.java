package org.wildfly.cli.command;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.cli.CliContext;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.cli.rest.client.CommandFailedException;
import org.wildfly.cli.rest.client.DeploymentDto;
import org.wildfly.cli.util.TableOutputter;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.nio.file.Files;
import java.util.List;

@Command(
        name = "app",
        description = "Application commands",
        mixinStandardHelpOptions = true,
        subcommands = {
                AppCommands.UseCommand.class,
                AppCommands.GetCommand.class,
                AppCommands.CreateCommand.class,
                AppCommands.DeleteCommand.class,
                AppCommands.ListCommand.class,
                AppCommands.DeployCommand.class,
                AppCommands.ArchiveCommands.class,
                AppCommands.ConfigCommands.class
        })
public class AppCommands {

    static abstract class BaseAppCommand implements Runnable {
        @RestClient
        ApplicationService applicationService;

        @Inject
        CliContext cliContext;

        protected String validateActiveApp() {
            String activeApp = cliContext.getActiveApp();
            if (activeApp == null) {
                System.err.println("No active application is set. Cannot deploy.");
                System.exit(1);
            }
            return activeApp;
        }
    }

    private static final String INDENT = "  ";

    @Command(name = "create", description = "Creates a new application", mixinStandardHelpOptions = true)
    static class CreateCommand extends BaseAppCommand {

        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name. Must be unique.")
        String name;

        @Override
        public void run() {
            try {
                Application application = new Application();
                application.name = name;
                applicationService.create(application);
                cliContext.setActiveApp(name);
                System.out.println("Application " + name + " created and set as the active application.");
            } catch (CommandFailedException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Command(name = "use", description = "Sets the active application", mixinStandardHelpOptions = true)
    static class UseCommand extends BaseAppCommand {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name to use as the active application.")
        String name;

        @Override
        public void run() {
            cliContext.setActiveApp(name);
            System.out.println("Application " + name + " set as the active application.");
        }
    }


    @Command(name = "delete", description = "Deletes an application", mixinStandardHelpOptions = true)
    static class DeleteCommand extends BaseAppCommand {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name to delete.")
        String name;

        @Override
        public void run() {
            String activeApp = cliContext.getActiveApp();
            try {
                applicationService.delete(name);
                System.out.println("Application '" + name + "' deleted");
                if (name.equals(activeApp)) {
                    System.out.println("Since this was the currently active application, the active application has been cleared");
                }
            } catch (CommandFailedException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Command(name = "get", description = "Gets the current application", mixinStandardHelpOptions = true)
    static class GetCommand extends BaseAppCommand {

        @Inject
        ArchiveCommands.ListCommand listCommand;

        @CommandLine.Option(names = {"-v", "--verbose"}, description = "Output detailed information")
        boolean verbose;

        @Override
        public void run() {
            String name = cliContext.getActiveApp();
            if (name == null) {
                System.out.println("No application is currently active");
            } else {
                System.out.println(name);
                if (verbose) {
                    System.out.println("");
                    listCommand.fromCommandLine = false;
                    listCommand.run();
                }

            }
        }
    }

    @Command(name = "list", description = "Lists all applications", mixinStandardHelpOptions = true)
    static class ListCommand extends BaseAppCommand {
        @Override
        public void run() {
            cliContext.getActiveApp();
            List<Application> applications = applicationService.list();
            System.out.println("Applications:");
            if (applications.size() == 0) {
                System.out.println(INDENT + "No applications");
            } else {
                String activeApp = cliContext.getActiveApp();
                TableOutputter outputter = TableOutputter.builder()
                        .addColumn(30, "Application")
                        .addColumn(15, "Status")
                        .build();
                for (Application application : applications) {

                    String activeMarker = application.name.equals(activeApp) ? "* " : "";
                    outputter.addRow()
                            .addColumns(activeMarker + application.name, application.state.name())
                            .output();
                }
            }
        }
    }

    @Command(name = "deploy", description = "Deploy the application", mixinStandardHelpOptions = true)
    static class DeployCommand extends BaseAppCommand {
        @RestClient
        ApplicationService applicationService;

        @Override
        public void run() {
            String activeApp = validateActiveApp();

            applicationService.deploy(activeApp);
        }
    }

    @Command(
            name = "archive",
            description = "Application archive commands",
            mixinStandardHelpOptions = true,
            subcommands = {
                    ArchiveCommands.ListCommand.class,
                    ArchiveCommands.AddCommand.class,
                    ArchiveCommands.ReplaceCommand.class,
                    ArchiveCommands.DeleteCommand.class
            })
    static final class ArchiveCommands {

        @Command(name = "list", description = "List archives in the application.", mixinStandardHelpOptions = true)
        static class ListCommand extends BaseAppCommand {
            public boolean fromCommandLine = true;

            @Override
            public void run() {
                String activeApp = validateActiveApp();
                List<AppArchive> appArchives = applicationService.listArchives(activeApp);
                if (fromCommandLine) {
                    System.out.println("Archives in " + activeApp + ":");
                } else {
                    System.out.println("Archives:");
                }

                if (appArchives.size() == 0) {
                    System.out.println(INDENT + "None");
                } else {
                    TableOutputter outputter = TableOutputter.builder()
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
        }

        @Command(name = "add", description = "Add one or more archives to the application.", mixinStandardHelpOptions = true)
        static class AddCommand extends BaseAppCommand {
            @CommandLine.Parameters(paramLabel = "<paths>", description = "Comma-separated paths to files to add.", split = ",")
            List<java.nio.file.Path> paths;

            @Override
            public void run() {
                String activeApp = validateActiveApp();

                for (java.nio.file.Path path : paths) {
                    if (!Files.exists(path)) {
                        System.err.println(path + " not found");
                        System.exit(1);
                    }
                    if (Files.isDirectory(path)) {
                        System.err.println(path + " is a directory");
                        System.exit(1);
                    }
                    DeploymentDto dto = new DeploymentDto(path);
                    String fileName = path.getFileName().toString();
                    System.out.println("Adding " + fileName + " to application " + activeApp + "");

                    System.out.println("Uploading " + activeApp);
                    applicationService.addArchive(activeApp, dto);
                }
            }
        }

        @Command(name = "replace", description = "Replaces one or more archives in the application.", mixinStandardHelpOptions = true)
        static class ReplaceCommand extends BaseAppCommand {
            @RestClient
            ApplicationService applicationService;

            @CommandLine.Parameters(paramLabel = "<paths>", description = "Comma-separated paths to files to replace.", split = ",")
            List<java.nio.file.Path> paths;

            @Override
            public void run() {
                String activeApp = validateActiveApp();

                for (java.nio.file.Path path : paths) {
                    if (!Files.exists(path)) {
                        System.err.println(path + " not found");
                        System.exit(1);
                    }
                    if (Files.isDirectory(path)) {
                        System.err.println(path + " is a directory");
                        System.exit(1);
                    }
                    DeploymentDto dto = new DeploymentDto(path);
                    String fileName = path.getFileName().toString();
                    System.out.println("Uploading " + fileName + " to application " + activeApp + " for replacement");
                    applicationService.replaceArchive(activeApp, fileName, dto);
                }
            }
        }

        @Command(name = "delete", description = "Deletes one or more archives in the application.", mixinStandardHelpOptions = true)
        static class DeleteCommand extends BaseAppCommand {

            @CommandLine.Parameters(paramLabel = "<names>", description = "Comma-separated names of archives to delete.", split = ",")
            List<String> names;

            @Override
            public void run() {
                String activeApp = validateActiveApp();
                for (String name : names) {
                    System.out.println("Deleting " + name + " from " + activeApp);
                    applicationService.deleteArchive(activeApp, name);
                }
            }
        }
    }

    @Command(
            name = "config",
            description = "Application config commands",
            mixinStandardHelpOptions = true,
            subcommands = {
                    ConfigCommands.GetCommand.class,
                    ConfigCommands.SetCommand.class,
                    ConfigCommands.DeleteCommand.class
            })
    static final class ConfigCommands {

        static abstract class BaseConfigCommand extends BaseAppCommand {
            @CommandLine.Parameters(paramLabel = "<type>", description = "xml, cli, yml or all. " +
                    "The xml is used to configure the Galleon layers, while the cli and yml is used to configure the server")
            String type;

            void validateType() {
                switch (type) {
                    case "xml":
                    case "cli":
                    case "yml":
                    return;
                    default:
                        System.err.println("Not a valid type");
                        System.exit(1);
                }
            }
        }

        @Command(name = "get", description = "Gets the config file contents.", mixinStandardHelpOptions = true)
        static class GetCommand extends BaseConfigCommand {

            @Override
            public void run() {
                String activeApp = validateActiveApp();
                validateType();
                String config = applicationService.getConfigFileContents(activeApp, type);
                System.out.println(config);
            }
        }

        @Command(name = "set", description = "Adds config file contents.", mixinStandardHelpOptions = true)
        static class SetCommand extends BaseConfigCommand {
            @CommandLine.Parameters(paramLabel = "<path>", description = "Path to file containing the contents.")
            java.nio.file.Path path;

            @Override
            public void run() {
                String activeApp = validateActiveApp();
                validateType();
                applicationService.setConfigFileContents(activeApp, type, new DeploymentDto(path));
            }
        }

        @Command(name = "delete", description = "Deletes the config file.", mixinStandardHelpOptions = true)
        static class DeleteCommand extends BaseConfigCommand {


            @Override
            public void run() {
                String activeApp = validateActiveApp();
                validateType();
                applicationService.deleteConfigFileContents(activeApp, type);
            }
        }

    }
}
