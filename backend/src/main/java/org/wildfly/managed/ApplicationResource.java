package org.wildfly.managed;

import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.common.model.Application;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/app")
public class ApplicationResource {

    @Inject
    UiPaths applicationPaths;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> list() {
        return Application.listAll();
    }

}