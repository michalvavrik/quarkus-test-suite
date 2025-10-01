package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class StepUpAuthenticationIT {

    @KeycloakContainer(runKeycloakInProdMode = true)
    static KeycloakService keycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM, DEFAULT_REALM_BASE_PATH);

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("quarkus.http.auth.proactive", "false")
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl())
            .withProperty("quarkus.keycloak.admin-client.server-url",
                    () -> {
                        String realmUrl = keycloak.getRealmUrl();
                        return realmUrl.substring(0, realmUrl.indexOf("/realms"));
                    })
            .withProperty("quarkus.keycloak.admin-client.realm", "master")
            .withProperties(() -> keycloak.getTlsProperties())
            .withProperty("quarkus.keycloak.admin-client.tls-configuration-name", "keycloak");

    @BeforeAll
    public static void setupRealm() {
        app.given()
                .when()
                .get("/admin-client/configure-stepup")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Realm configured for Step-Up Authentication"));
    }

    @Test
    public void testSingleAcrWithValidToken() {
        String token = TokenUtils.createToken(keycloak);
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        System.out.println("TOKEN PAYLOAD: " + payload);
        app.given()
                .auth().oauth2(token)
                .when()
                .get("/step-up/single-acr")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Single ACR validated"));
    }

}
