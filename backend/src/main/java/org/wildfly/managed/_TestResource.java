package org.wildfly.managed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

//DETETE THIS CLASS
@Path("/test")
public class _TestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String list() {
        return "test!";
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/sub")
    String sub() {
        return "test/sub!";
    }

}