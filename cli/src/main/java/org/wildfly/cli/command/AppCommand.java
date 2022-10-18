package org.wildfly.cli.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.cli.rest.client.ApplicationService;
import org.wildfly.managed.common.model.Application;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

//@TopCommand
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

//
//    @RestClient
//    ApplicationService applicationService;
//
//    @PostConstruct
//    public void init() {
//        System.out.println("---> INIT");
//    }

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
//        @Inject
//        TestBean testBean;

        @Override
        public void run() {
//            System.out.println("---> " + testBean);
            List<Application> applications = applicationService.list();
            if (applications == null) {
                System.out.println("\n\tNo applications");
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
