package org.wildfly.managed;

import org.wildfly.managed.config.UiPaths;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/app")
public class ApplicationResource {

    @Inject
    UiPaths applicationPaths;

    @Inject
    ApplicationRepo applicationRepo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> list() {
        return applicationRepo.listAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Application create(Application application) {
        System.out.println("----> Creating " + application);
        System.out.println(application.getName());
        return applicationRepo.create(application);
    }
}