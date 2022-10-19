package org.wildfly.cli.command;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.cli.rest.client.DeploymentDto;
import org.wildfly.managed.common.model.Application;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                // Temp
                AppCommand.TempUploadCommand.class,
        })
public class AppCommand {
    private static final String INDENT = "  ";
    @Command(name = "create", description = "Creates a new application", mixinStandardHelpOptions = true)
    static class CreateCommand implements Runnable {
        @RestClient
        ApplicationService applicationService;

        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name. Must be unique.")
        String name;

        @Override
        public void run() {
            Application application = new Application();
            application.setName(name);
            applicationService.create(application);
        }
    }

    @Command(name = "use", description = "Sets the active application", mixinStandardHelpOptions = true)
    static class UseCommand implements Runnable {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name to use. Must exist.")
        String name;

        @Override
        public void run() {

        }
    }


    @Command(name = "delete", description = "Deletes an application", mixinStandardHelpOptions = true)
    static class DeleteCommand implements Runnable {
        @Override
        public void run() {

        }
    }

    @Command(name = "get", description = "Gets the current application", mixinStandardHelpOptions = true)
    static class GetCommand implements Runnable {
        @Override
        public void run() {

        }
    }

    @Command(name = "list", description = "Lists all applications", mixinStandardHelpOptions = true)
    static class ListCommand implements Runnable {
        @RestClient
        ApplicationService applicationService;

        @Override
        public void run() {
            List<Application> applications = applicationService.list();
            System.out.println("Applications:");
            if (applications.size() == 0) {
                System.out.println(INDENT + "No applications");
            }
            for (Application application : applications) {
                System.out.println(INDENT + application.getName());
            }
        }
    }

    @Command(name = "deploy", description = "Deploy the application", mixinStandardHelpOptions = true)
    static class DeployCommand implements Runnable {
        @Override
        public void run() {

        }
    }

    @Command(name = "upload", description = "TEMP", mixinStandardHelpOptions = true)
    static class TempUploadCommand implements Runnable {
        @RestClient
        ApplicationService applicationService;

        @CommandLine.Parameters(paramLabel = "<path>", description = "Path to the file.")
        java.nio.file.Path path;

        @Override
        public void run() {
            System.out.println("Try temp");
            applicationService.temp();

            System.out.println("----> " + path);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException(path + " not found");
            }
            DeploymentDto dto = new DeploymentDto(path, "test");
            applicationService.tempUpload(dto);
        }
    }


}
