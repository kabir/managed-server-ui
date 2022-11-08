package org.wildfly.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.wildfly.cli.command.AppCommands;
import org.wildfly.cli.command.ServerCommands;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
        name = "ms-cli",
        mixinStandardHelpOptions = true,
        version = "0.1",
        subcommands = {AppCommands.class, ServerCommands.class})
public class CliMain {

}
