package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;

@Path("/admin-client")
public class AdminClientResource {

    private static final Logger LOG = Logger.getLogger(AdminClientResource.class);

    public static final String ACR_LOA_MAP = "acr.loa.map";
    private static final String REALM_NAME = "test-realm";
    private static final String CLIENT_ID = "test-application-client";

    @Inject
    Keycloak keycloak;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("configure-stepup")
    public Response configureStepUpRealm() {
        try {
            return Response.ok("Realm configured for Step-Up Authentication").build();

        } catch (Exception e) {
            LOG.error("Error configuring realm: ", e);
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

}
