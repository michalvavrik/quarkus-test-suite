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

    //TODO Remove workaround after Keycloak is fixed https://github.com/keycloak/keycloak/issues/9916
    @KeycloakContainer(command = { "start-dev", "--import-realm", "--hostname-strict=false",
            "--hostname-strict-https=false", "--features=token-exchange",
            "--https-client-auth=required", "--https-certificate-file=/etc/tls-crt/tls.crt",
            "--https-certificate-key-file=/etc/tls-key/tls.key" }, port = KEYCLOAK_PORT, image = "${rhbk.image}")
    static KeycloakService keycloak = newKeycloakInstance(DEFAULT_REALM_FILE, REALM_DEFAULT, "realms")
            .withRedHatFipsDisabled()
            .withProperty("KC_HTTPS_CERTIFICATE_FILE", "secret_with_destination::/etc/tls-crt|tls.crt")
            .withProperty("KC_HTTPS_CERTIFICATE_KEY_FILE", "secret_with_destination::/etc/tls-key|tls.key");

    /**
     * Keystore file type is automatically detected by file extension by quarkus-oidc.
     */
    @QuarkusApplication
    static RestService app = createRestService(JKS_KEYSTORE_FILE_TYPE, JKS_KEYSTORE_FILE_EXTENSION, keycloak::getRealmUrl)
            .withProperty("quarkus.oidc.tls.trust-store-file", "rhsso-client-truststore.jks")
            .withProperty("quarkus.oidc.tls.key-store-file", "rhsso-client-keystore.jks");

    @Override
    protected KeycloakService getKeycloakService() {
        return keycloak;
    }

    @Override
    protected String getKeystoreFileExtension() {
        return JKS_KEYSTORE_FILE_EXTENSION;
    }

}
