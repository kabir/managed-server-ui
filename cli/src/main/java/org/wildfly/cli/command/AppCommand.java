package org.wildfly.cli.command;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.managed.common.model.Application;
import picocli.CommandLine;
import picocli.CommandLine.Command;

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
                AppCommand.ListCommand.class
        })
public class AppCommand {

    @Command(name = "create", description = "Creates a new application", mixinStandardHelpOptions = true)
    static class CreateCommand implements Runnable {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name. Must be unique.")
        String name;

        @Override
        public void run() {
            System.out.println("create " + name);
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
            if (applications.size() == 0) {
                System.out.println("No applications");
            }
        }
    }

    @Command(name = "deploy", description = "Deploy the application", mixinStandardHelpOptions = true)
    static class DeployCommand implements Runnable {
        @Override
        public void run() {

        }
    }

}
