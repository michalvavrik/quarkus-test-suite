package io.quarkus.ts.monitoring.micrometeropentelemetry.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestHeader;

@Path("/micrometer")
public class MicrometerResource {

    @Path("/song")
    @Produces(APPLICATION_JSON)
    @GET
    public Response song(@RestHeader boolean badRequest) {
        return badRequest ? Response.status(400).build() : Response.ok("Ho Hey").build();
    }

}
