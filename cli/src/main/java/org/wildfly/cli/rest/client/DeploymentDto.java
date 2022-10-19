package org.wildfly.cli.rest.client;

import org.jboss.resteasy.reactive.PartType;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import java.nio.file.Path;

public class DeploymentDto {
    //@RestForm
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public Path file;

    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    public String fileName;

    public DeploymentDto(Path file, String fileName) {
        this.file = file;
        this.fileName = fileName;
    }

    public DeploymentDto() {
    }
}
