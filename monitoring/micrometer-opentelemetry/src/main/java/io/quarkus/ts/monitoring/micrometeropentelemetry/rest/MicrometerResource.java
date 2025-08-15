package io.quarkus.ts.monitoring.micrometeropentelemetry.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestQuery;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Path("/micrometer")
public class MicrometerResource {

    private final Counter counter;

    MicrometerResource(MeterRegistry meterRegistry) {
        counter = Counter.builder("count.me")
                .baseUnit("beans")
                .description("counter used for teasing")
                .tags("region", "test")
                .register(meterRegistry);
    }

    @Path("/song")
    @Produces(APPLICATION_JSON)
    @GET
    public Response song(@RestHeader boolean badRequest) {
        return badRequest ? Response.status(400).build() : Response.ok("Ho Hey").build();
    }

    @Path("/counter")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @GET
    public Response counterDouble(@RestQuery Double increase) {
        counter.increment(increase);
        return Response.ok("Counter increased by " + increase).build();
    }
}
