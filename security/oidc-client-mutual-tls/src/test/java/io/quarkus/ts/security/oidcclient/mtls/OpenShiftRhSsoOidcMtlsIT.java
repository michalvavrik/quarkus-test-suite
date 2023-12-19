package io.quarkus.ts.security.oidcclient.mtls;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;
import static io.quarkus.ts.security.oidcclient.mtls.MutualTlsKeycloakService.newKeycloakInstance;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
@DisabledIfSystemProperty(named = "ts.arm.missing.services.excludes", matches = "true", disabledReason = "https://github.com/quarkus-qe/quarkus-test-suite/issues/1145")
@EnabledIfSystemProperty(named = "ts.redhat.registry.enabled", matches = "true")
public class OpenShiftRhSsoOidcMtlsIT extends KeycloakMtlsAuthN {

    @KeycloakContainer(command = { "start-dev", "--import-realm", "--hostname-strict=false",
            "--hostname-strict-https=false", "--features=token-exchange", "--hostname=localhost",
            "--https-client-auth=required", "--https-key-store-file=/etc/keystore/server-keystore.p12",
            "--https-trust-store-file=/etc/truststore/server-truststore.p12",
            "--https-trust-store-password=password" }, port = KEYCLOAK_PORT, image = "${rhbk.image}")
    static MutualTlsKeycloakService keycloak = (MutualTlsKeycloakService) newKeycloakInstance(DEFAULT_REALM_FILE, REALM_DEFAULT,
            "realms")
            .withProperty("HTTPS_KEYSTORE",
                    "secret_with_destination::/etc/keystore|server-keystore." + P12_KEYSTORE_FILE_EXTENSION)
            .withProperty("HTTPS_TRUSTSTORE",
                    "secret_with_destination::/etc/truststore|server-truststore." + P12_KEYSTORE_FILE_EXTENSION);

    /**
     * Keystore file type is automatically detected by file extension by quarkus-oidc.
     */
    @QuarkusApplication
    static RestService app = createRestService(P12_KEYSTORE_FILE_TYPE, P12_KEYSTORE_FILE_EXTENSION, keycloak::getRealmUrl)
            .withProperty("quarkus.oidc.tls.trust-store-file", "client-truststore." + P12_KEYSTORE_FILE_EXTENSION)
            .withProperty("quarkus.oidc.tls.key-store-file", "client-keystore." + P12_KEYSTORE_FILE_EXTENSION);

    @Override
    protected KeycloakService getKeycloakService() {
        return keycloak;
    }

    @Override
    protected String getKeystoreFileExtension() {
        return P12_KEYSTORE_FILE_EXTENSION;
    }

}
