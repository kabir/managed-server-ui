package org.wildfly.cli.command;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.cli.CliContext;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.cli.rest.client.DeploymentDto;
import org.wildfly.cli.util.TableOutputter;
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
                AppCommand.UseCommand.class,
                AppCommand.GetCommand.class,
                AppCommand.CreateCommand.class,
                AppCommand.DeleteCommand.class,
                AppCommand.ListCommand.class,
                AppCommand.UploadCommand.class,
                AppCommand.DeployCommand.class
        })
public class AppCommand {

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
            Application application = new Application();
            application.setName(name);
            applicationService.create(application);
            cliContext.setActiveApp(name);
            System.out.println("Application " + name + " created and set as the active application.");
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
            applicationService.delete(name);
            System.out.println("Application '" + name + "' deleted");
            if (name.equals(activeApp)) {
                System.out.println("Since this was the currently active applicaiton, the active application has been cleared");
            }

        }
    }

    @Command(name = "get", description = "Gets the current application", mixinStandardHelpOptions = true)
    static class GetCommand extends BaseAppCommand {
        @Override
        public void run() {
            String name = cliContext.getActiveApp();
            if (name == null) {
                System.out.println("No application is currently active");
            } else {
                System.out.println(name);
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

                    String activeMarker = application.getName().equals(activeApp) ? "* " : "";
                    //System.out.println(activeMarker + application.getName() + " " + application.getState());
                    outputter.addRow()
                            .addColumns(activeMarker + application.getName(), application.getState().name())
                            .output();
                }
            }
        }
    }


    @Command(name = "upload", description = "Upload", mixinStandardHelpOptions = true)
    static class UploadCommand extends BaseAppCommand {
        @RestClient
        ApplicationService applicationService;

        @CommandLine.Parameters(paramLabel = "<path>", description = "Path to the file.")
        java.nio.file.Path path;

        @Override
        public void run() {
            String activeApp = validateActiveApp();

            if (!Files.exists(path)) {
                System.err.println(path + " not found");
                System.exit(1);
            }
            if (Files.isDirectory(path)) {
                System.err.println(path + " is a directory");
                System.exit(1);
            }
            //String fileName = path.getFileName().toString();
            DeploymentDto dto = new DeploymentDto(path/*, fileName*/);
            applicationService.upload(activeApp, dto);
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

}
