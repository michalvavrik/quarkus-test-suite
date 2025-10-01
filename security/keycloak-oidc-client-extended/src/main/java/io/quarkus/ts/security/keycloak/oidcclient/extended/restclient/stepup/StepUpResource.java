package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.AuthenticationContext;

@Path("/step-up")
public class StepUpResource {

    @GET
    @Path("/single-acr")
    @AuthenticationContext("copper")
    public String singleAcr() {
        return "Single ACR validated";
    }

}
