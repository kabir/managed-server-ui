package org.wildfly.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.wildfly.cli.command.AppCommand;
import picocli.CommandLine;

//@QuarkusMain
@TopCommand
@CommandLine.Command(
        name = "ms-cli",
        mixinStandardHelpOptions = true,
        version = "0.1",
        subcommands = {AppCommand.class})
public class CliMain /*implements QuarkusApplication*/ {

//    public int run(String[] args) {
//        int exitCode = new CommandLine(new CliMain()).execute(args);
//        System.exit(exitCode);
//        return 0;
//    }
}
