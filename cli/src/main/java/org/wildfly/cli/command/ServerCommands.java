package org.wildfly.cli.command;

import org.wildfly.cli.context.CliContext;
import picocli.CommandLine;

import javax.inject.Inject;
import java.net.URI;

@CommandLine.Command(
        name = "server",
        description = "Operations for the backend server used to manage applications",
        mixinStandardHelpOptions = true,
        subcommands = {
                ServerCommands.SetServerCommand.class,
                ServerCommands.GetServerCommand.class
        })
public class ServerCommands {

    static abstract class BaseServerCommand {
        @Inject
        CliContext cliContext;
    }

    @CommandLine.Command(name = "set", description = "Sets the backend server", mixinStandardHelpOptions = true)
    static class SetServerCommand extends BaseServerCommand implements Runnable {

        @CommandLine.Parameters(paramLabel = "<name>", description = "Server uri.", index = "0")
        String server;

        @Override
        public void run() {
            System.out.println("Setting server to: " + server);
            cliContext.setServerBackEndUri(server);
        }
    }

    @CommandLine.Command(name = "get", description = "Gets the backend server", mixinStandardHelpOptions = true)
    static class GetServerCommand extends BaseServerCommand implements Runnable {

        @Override
        public void run() {
            URI uri = cliContext.getServerBackEndUri();
            if (uri == null) {
                System.out.println("Server not set");
            } else {
                System.out.println(cliContext.getServerBackEndUri());
            }
        }
    }
}
