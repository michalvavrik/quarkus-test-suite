package io.quarkus.ts.openshift.security.basic;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("/user")
@RolesAllowed("user")
public class UserResource {
    @GET
    public String get(@Context SecurityContext security) {
        return "Hello, user " + security.getUserPrincipal().getName();
    }
}
