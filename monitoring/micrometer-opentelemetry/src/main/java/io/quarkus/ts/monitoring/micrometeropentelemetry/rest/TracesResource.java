package io.quarkus.ts.monitoring.micrometeropentelemetry.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/traces")
public class TracesResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloPathParam(@PathParam("name") String name) {
        return "Traced Hello to " + name;
    }

}
