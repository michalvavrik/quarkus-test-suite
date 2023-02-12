package io.quarkus.ts.monitoring.opentracing.reactive.grpc.ping;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.ts.monitoring.opentracing.reactive.grpc.ping.clients.PongClient;
import io.quarkus.ts.monitoring.opentracing.reactive.grpc.traceable.TraceableResource;

@Path("/rest-ping")
public class PingResource extends TraceableResource {

    @Inject
    @RestClient
    PongClient pongClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getPing() {
        recordTraceId();

        return "ping " + pongClient.getPong();
    }
}