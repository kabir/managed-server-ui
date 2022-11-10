package org.wildfly.cli.command;

import org.wildfly.cli.context.CliContext;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.cli.rest.client.DeploymentDto;
import org.wildfly.cli.util.ColouredWriter;
import org.wildfly.cli.util.TableRenderer;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.value.AppState;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.inject.Inject;
import java.nio.file.Files;
import java.util.List;

import static org.wildfly.cli.util.ColouredWriter.printlnError;
import static org.wildfly.cli.util.ColouredWriter.printlnSuccess;
import static org.wildfly.cli.util.ColouredWriter.printlnWarning;

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
                AppCommands.StopCommand.class,
                AppCommands.StatusCommand.class,
                AppCommands.ArchiveCommands.class,
                AppCommands.ConfigCommands.class
        })
public class AppCommands {


    static abstract class BaseAppCommand implements Runnable {

        @Inject
        CliContext cliContext;

        protected ApplicationService applicationService() {
            return ApplicationService.createInstance(cliContext);
        }
    }

    @Command(name = "create", description = "Creates a new application", mixinStandardHelpOptions = true)
    static class CreateCommand extends BaseAppCommand {

        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name. Must be unique.", index = "0")
        String name;

        @Override
        public void run() {
            Application application = new Application();
            application.name = name;
            applicationService().create(application);
            cliContext.setActiveApp(name);
            printlnSuccess("Application " + name + " created and set as the active application.");
        }
    }

    @Command(name = "use", description = "Sets the active application", mixinStandardHelpOptions = true)
    static class UseCommand extends BaseAppCommand {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name to use as the active application.")
        String name;

        @Override
        public void run() {
            cliContext.setActiveApp(name);
            printlnSuccess("Application " + name + " set as the active application.");
        }
    }


    @Command(name = "delete", description = "Deletes an application", mixinStandardHelpOptions = true)
    static class DeleteCommand extends BaseAppCommand {
        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
        String appName;

        @CommandLine.Option(names = {"-f", "--force"}, description = "Cancel any running builds and deploy")
        boolean force;

        @Override
        public void run() {
            ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
            System.out.println("Deleting application '" + appSelector.name + "'...");
            applicationService().delete(appSelector.name, force);
            printlnSuccess("Application '" + appSelector.name + "' deleted");
            if (appSelector.active) {
                printlnWarning("Since this was the currently active application, the active application has been cleared");
            }
        }
    }

    @Command(name = "stop", description = "Stops an application", mixinStandardHelpOptions = true)
    static class StopCommand extends BaseAppCommand {
        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
        String appName;

        @Override
        public void run() {
            ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
            System.out.println("Stopping application '" + appSelector.name + "'...");
            applicationService().stop(appSelector.name);
            printlnSuccess("Application '" + appSelector.name + "' stopped");
        }
    }

    @Command(name = "get", description = "Gets information about an application", mixinStandardHelpOptions = true)
    static class GetCommand extends BaseAppCommand {

        @CommandLine.Option(names = {"-v", "--verbose"}, description = "Output detailed information")
        boolean verbose;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
        String appName;

        @Override
        public void run() {

            ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);

            if (!verbose) {
                System.out.println(appSelector.name);
            } else {
                System.out.println("Name: " + appSelector.name);
            }

            // Load it to check it is there whether verbose or not.
            // We do this after printing the name so people can see the active application
            Application app = applicationService().get(appSelector.name, false);

            if (verbose) {
                System.out.println("");
                System.out.println("Status");
                System.out.println("------");
                AppState appStatus = applicationService().status(appSelector.name);
                Renderers.renderAppStatus(appStatus);

                System.out.println("");
                System.out.println("Configs");
                System.out.println("-------");
                System.out.println("XML: " + (app.hasServerConfigXml ? "y" : ""));
                System.out.println("CLI: " + (app.hasServerInitCli ? "y" : ""));
                System.out.println("YML: " + (app.hasServerInitYml ? "y" : ""));

                System.out.println("");
                System.out.println("Routes");
                System.out.println("------");
                List<String> routes = applicationService().routes(appSelector.name);
                for (String route : routes) {
                    System.out.println(route);
                }

                System.out.println("");
                System.out.println("Archives:");
                List<AppArchive> appArchives = applicationService().listArchives(appSelector.name);
                Renderers.renderAppArchives(appArchives);
            }
        }
    }

    @Command(name = "list", description = "Lists all applications", mixinStandardHelpOptions = true)
    static class ListCommand extends BaseAppCommand {
        @Override
        public void run() {
            cliContext.getActiveApp();
            List<Application> applications = applicationService().list();
            if (applications.size() == 0) {
                System.out.println(Renderers.INDENT + "No applications");
            } else {
                String activeApp = cliContext.getActiveApp();
                TableRenderer outputter = TableRenderer.builder()
                        .addColumn(30, "Application")
                        .addColumn(15, "Deployment")
                        .addColumn(15, "Build")
                        .build();
                for (Application application : applications) {
                    AppState appState = applicationService().status(application.name);
                    String activeMarker = application.name.equals(activeApp) ? "* " : "";
                    outputter.addRow()
                            .addColumns(
                                    activeMarker + application.name,
                                    appState.getDeploymentState().toString(),
                                    appState.getBuildState().toString())
                            .output();
                }
            }
        }
    }

    @Command(name = "deploy", description = "Deploy the application", mixinStandardHelpOptions = true)
    static class DeployCommand extends BaseAppCommand {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
        String appName;

        @CommandLine.Option(names = {"-f", "--force"}, description = "Cancel any running builds and deploy")
        boolean force;

        @CommandLine.Option(names = {"-r", "--refresh"}, description = "Refreshes the archives in a running application.")
        boolean refresh;

        @Override
        public void run() {
            ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
            System.out.println("Deploying application...");
            applicationService().deploy(appSelector.name, force, refresh);
            printlnSuccess("Application deployment registered. Monitor the status with 'app status'");
        }
    }

    @Command(name = "status", description = "Gets the application status", mixinStandardHelpOptions = true)
    static class StatusCommand extends BaseAppCommand {
        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
        String appName;

        @Override
        public void run() {
            ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
            AppState appStatus = applicationService().status(appSelector.name);
            Renderers.renderAppStatus(appStatus);
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
            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
                List<AppArchive> appArchives = applicationService().listArchives(appSelector.name);
                System.out.println("Archives in " + appSelector.name + ":");
                Renderers.renderAppArchives(appArchives);
            }
        }

        @Command(name = "add", description = "Add one or more archives to the application.", mixinStandardHelpOptions = true)
        static class AddCommand extends BaseAppCommand {
            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @CommandLine.Parameters(paramLabel = "<paths>", description = "Comma-separated paths to files to add.", split = ",")
            List<java.nio.file.Path> paths;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);

                if (paths == null || paths.size() == 0) {
                    printlnError("Need top specify at least one path to an archive");
                    System.exit(1);
                }

                for (java.nio.file.Path path : paths) {
                    if (!Files.exists(path)) {
                        printlnError(path + " not found");
                        System.exit(1);
                    }
                    if (Files.isDirectory(path)) {
                        printlnError(path + " is a directory");
                        System.exit(1);
                    }
                    DeploymentDto dto = new DeploymentDto(path);
                    String fileName = path.getFileName().toString();
                    System.out.println("Adding " + fileName + " to application " + appSelector.name + "");

                    System.out.println("Uploading " + fileName + "...");
                    applicationService().addArchive(appSelector.name, dto);
                    printlnSuccess(fileName + " uploaded.");
                }
            }
        }

        @Command(name = "replace", description = "Replaces one or more archives in the application.", mixinStandardHelpOptions = true)
        static class ReplaceCommand extends BaseAppCommand {

            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @CommandLine.Parameters(paramLabel = "<paths>", description = "Comma-separated paths to files to replace.", split = ",")
            List<java.nio.file.Path> paths;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);

                for (java.nio.file.Path path : paths) {
                    if (!Files.exists(path)) {
                        printlnError(path + " not found");
                        System.exit(1);
                    }
                    if (Files.isDirectory(path)) {
                        printlnError(path + " is a directory");
                        System.exit(1);
                    }
                    DeploymentDto dto = new DeploymentDto(path);
                    String fileName = path.getFileName().toString();
                    System.out.println("Uploading " + fileName + " to application " + appSelector.name + " for replacement...");
                    applicationService().replaceArchive(appSelector.name, fileName, dto);
                    printlnSuccess("Upload done");
                }
            }
        }

        @Command(name = "delete", description = "Deletes one or more archives in the application.", mixinStandardHelpOptions = true)
        static class DeleteCommand extends BaseAppCommand {

            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @CommandLine.Parameters(paramLabel = "<names>", description = "Comma-separated names of archives to delete.", split = ",")
            List<String> names;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
                for (String name : names) {
                    System.out.println("Deleting " + name + " from " + appSelector.name + "...");
                    applicationService().deleteArchive(appSelector.name, name);
                    printlnSuccess(name + " deleted.");
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
                        printlnError("Not a valid type");
                        System.exit(1);
                }
            }
        }

        @Command(name = "get", description = "Gets the config file contents.", mixinStandardHelpOptions = true)
        static class GetCommand extends BaseConfigCommand {
            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
                validateType();
                String config = applicationService().getConfigFileContents(appSelector.name, type);
                System.out.println(config);
            }
        }

        @Command(name = "set", description = "Adds config file contents.", mixinStandardHelpOptions = true)
        static class SetCommand extends BaseConfigCommand {
            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @CommandLine.Parameters(paramLabel = "<path>", description = "Path to file containing the contents.")
            java.nio.file.Path path;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
                validateType();
                System.out.println("Uploading " + type + "...");
                applicationService().setConfigFileContents(appSelector.name, type, new DeploymentDto(path));
                printlnSuccess("Content uploaded");
            }
        }

        @Command(name = "delete", description = "Deletes the config file.", mixinStandardHelpOptions = true)
        static class DeleteCommand extends BaseConfigCommand {
            @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the application. If omitted, the current application is used.")
            String appName;

            @Override
            public void run() {
                ApplicationSelector appSelector = ApplicationSelector.create(cliContext, appName);
                validateType();
                System.out.println("Deleting " + type + "...");
                applicationService().deleteConfigFileContents(appName, type);
                printlnSuccess("Content deleted");
            }
        }

    }

    private static class ApplicationSelector {
        private final String name;
        private final boolean active;

        public ApplicationSelector(String appToExecuteOn, boolean active) {

            this.name = appToExecuteOn;
            this.active = active;
        }

        static ApplicationSelector create(CliContext cliContext, String nameOption) {

            String appToExecuteOn;
            boolean active = false;

            String activeApp = cliContext.getActiveApp();
            if (nameOption != null) {
                appToExecuteOn = nameOption;
            } else {
                if (activeApp == null) {
                    printlnError("No application is active, and no application set via --name. Cannot continue.");
                    System.exit(1);
                }
                appToExecuteOn = activeApp;
                active = true;
            }

            return new ApplicationSelector(appToExecuteOn, active);
        }

    }
}
