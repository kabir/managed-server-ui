package org.wildfly.managed;

import io.smallrye.mutiny.Uni;
import org.wildfly.managed.config.UiPaths;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/cli")
public class DownloadCliResource {

    private static final Pattern MANAGED_SERVER_CLI_JAR_MATCHER = Pattern.compile("managed-server-cli.*\\.jar");
    private static final Pattern MANAGED_SERVER_CLI_JAR = Pattern.compile("managed-server-cli.jar");

    @Inject
    UiPaths uiPaths;

    java.nio.file.Path managedServer;

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> downloadCli() {
        java.nio.file.Path path = getManagedServerJar();
        Response.ResponseBuilder builder = Response.ok(path.toFile())
                .header("Content-Disposition", "attachment;filename=" + MANAGED_SERVER_CLI_JAR);
        Uni<Response> uni = Uni.createFrom().item(builder.build());
        return uni;
    }

    private java.nio.file.Path getManagedServerJar() {
        if (managedServer != null) {
            System.out.println("Using existing!");
            return managedServer;
        }
        // A bit convoluted to be able to work locally
        System.out.println("Searching....");
        java.nio.file.Path dir = uiPaths.getDownloadsDir().toAbsolutePath();
        if (!Files.exists(dir) && !Files.isDirectory(dir)) {
            throw new IllegalStateException(dir + " does not exist");
        }
        List<java.nio.file.Path> paths = null;
        try {
            paths = Files.list(dir)
                    .filter(path -> MANAGED_SERVER_CLI_JAR_MATCHER.matcher(path.getFileName().toString()).matches())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Matching paths");
        if (paths.size() == 0) {
            throw new IllegalStateException("No cli file was found on the server for download");
        }
        if (paths.size() > 1) {
            throw new IllegalStateException("Several candidate CLI files were found:" + paths);
        }
        return paths.get(0);
    }
}