package org.wildfly.cli.app;

import picocli.CommandLine;

@CommandLine.Command(name = "deployments", description = "Manage deployments for an application", mixinStandardHelpOptions = true)
public class DeploymentCommand {
}
