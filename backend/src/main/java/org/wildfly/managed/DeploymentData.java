package org.wildfly.managed;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import javax.ws.rs.core.MediaType;

public class DeploymentData {
    @RestForm("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public FileUpload file;


}
