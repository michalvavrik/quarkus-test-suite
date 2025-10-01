package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.stepup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

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

            LOG.info("Configuring realm: " + REALM_NAME);

            RealmResource realmResource = keycloak.realm(REALM_NAME);
            RealmRepresentation realm = realmResource.toRepresentation();

            Map<String, Integer> acrLoaMap = new HashMap<>();
            acrLoaMap.put("copper", 0);
            acrLoaMap.put("silver", 1);
            acrLoaMap.put("gold", 2);

            if (realm.getAttributes() == null) {
                realm.setAttributes(new HashMap<>());
            }
            realm.getAttributes().put(ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
            realm.setOtpPolicyCodeReusable(true);

            realmResource.update(realm);

            createTestUsers(realmResource);

            ClientsResource clientsResource = realmResource.clients();
            ClientRepresentation client = clientsResource.findByClientId(CLIENT_ID)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (client != null) {
                if (client.getAttributes() == null) {
                    client.setAttributes(new HashMap<>());
                }
                client.getAttributes().put(ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
                ProtocolMapperRepresentation acrMapper = new ProtocolMapperRepresentation();
                acrMapper.setName("acr-mapper");
                acrMapper.setProtocol("openid-connect");
                acrMapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
                Map<String, String> config = new HashMap<>();
                config.put("user.attribute", "acr_level");
                config.put("claim.name", "acr");
                config.put("jsonType.label", "String");
                config.put("access.token.claim", "true");
                config.put("id.token.claim", "true");
                acrMapper.setConfig(config);

                client.setProtocolMappers(Arrays.asList(acrMapper));

                client.setProtocolMappers(Arrays.asList(acrMapper));

                clientsResource.get(client.getId()).update(client);
                LOG.info("Client updated with ACR mapping and ACR claim mapper");
            }

            return Response.ok("Realm configured for Step-Up Authentication").build();

        } catch (Exception e) {
            LOG.error("Error configuring realm: ", e);
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }

    private void createTestUsers(RealmResource realmResource) {
        UsersResource usersResource = realmResource.users();

        createUser(usersResource, "test-user-copper", "test-user-copper",
                Arrays.asList("user"), "copper");
        createUser(usersResource, "test-user-silver", "test-user-silver",
                Arrays.asList("user"), "silver");
        createUser(usersResource, "test-user-gold", "test-user-gold",
                Arrays.asList("user", "admin"), "gold");
    }

    private void createUser(UsersResource usersResource, String username, String password,
            java.util.List<String> roles, String acrLevel) {
        if (!usersResource.search(username).isEmpty()) {
            LOG.info("User " + username + " already exists, skipping creation");
            return;
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setEmail(username + "@test.com");
        user.setFirstName(username);
        user.setLastName("Test");

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("acr_level", Arrays.asList(acrLevel));
        user.setAttributes(attributes);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Arrays.asList(credential));

        Response response = usersResource.create(user);
        if (response.getStatus() == 201) {
            LOG.info("Created user: " + username + " with ACR level: " + acrLevel);
            response.close();
        } else {
            LOG.error("Failed to create user " + username + ": " + response.getStatus());
            response.close();
        }
    }
}
