package io.quarkus.ts.http.advanced.reactive;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@Path("/client")
public class ClientResource {
    @Inject
    @RestClient
    HealthClientService client;

    @GET
    public Uni<Response> get() {
        return client.health();
    }
}
