package org.wildfly.cli.app;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@TopCommand
@Command(
        name = "app",
        description = "Application commands",
        mixinStandardHelpOptions = true,
        subcommands = {
                AppCommand.Use.class,
                AppCommand.Get.class,
                AppCommand.Create.class,
                AppCommand.Delete.class,
                AppCommand.List.class
        })
public class AppCommand {


    @Command(name = "create", description = "Creates a new application", mixinStandardHelpOptions = true)
    static class Create implements Runnable {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name. Must be unique.")
        String name;

        @Override
        public void run() {
            System.out.println("create " + name);
        }
    }

    @Command(name = "use", description = "Sets the active application", mixinStandardHelpOptions = true)
    static class Use implements Runnable {
        @CommandLine.Parameters(paramLabel = "<name>", description = "Application name to use. Must exist.")
        String name;

        @Override
        public void run() {

        }
    }


    @Command(name = "delete", description = "Deletes an application", mixinStandardHelpOptions = true)
    static class Delete implements Runnable {
        @Override
        public void run() {

        }
    }

    @Command(name = "get", description = "Gets the current application", mixinStandardHelpOptions = true)
    static class Get implements Runnable {
        @Override
        public void run() {

        }
    }

    @Command(name = "list", description = "Lists all applications", mixinStandardHelpOptions = true)
    static class List implements Runnable {
        @Override
        public void run() {

        }
    }

    @Command(name = "deploy", description = "Deploy the application", mixinStandardHelpOptions = true)
    static class Deploy implements Runnable {
        @Override
        public void run() {

        }
    }

}
