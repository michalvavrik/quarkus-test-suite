package io.quarkus.ts.security.oidcclient.mtls;

import static io.quarkus.ts.security.oidcclient.mtls.MutualTlsKeycloakService.newKeycloakInstance;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@Tag("fips-incompatible")
@QuarkusScenario
public class BouncyCastleFipsJsseOidcMtlsIT extends KeycloakMtlsAuthN {

    @Container(image = "${keycloak.image}", expectedLog = EXPECTED_LOG, port = KEYCLOAK_PORT)
    static KeycloakService keycloak = newKeycloakInstance();

    private static final String KEY_STORE_FILE_EXTENSION = "bcfks";
    private static final String KEY_STORE_TYPE = "BCFKS";

    @BeforeAll
    static void beforeAll() {
        //        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        //        Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
    }

    @AfterAll
    static void afterAll() {

        //        Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
    }

    /**
     * Keystore file type is automatically detected by file extension by quarkus-oidc.
     */
    @QuarkusApplication
    static RestService app = createRestService(KEY_STORE_FILE_EXTENSION, KEY_STORE_TYPE, keycloak::getRealmUrl);

    @Override
    protected String getKeyStoreFileExtension() {
        return KEY_STORE_FILE_EXTENSION;
    }

    @Override
    protected KeycloakService getKeycloakService() {
        return keycloak;
    }

}
