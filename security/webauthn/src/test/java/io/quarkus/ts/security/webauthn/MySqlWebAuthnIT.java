package io.quarkus.ts.security.webauthn;

import io.quarkus.test.bootstrap.MySqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class MySqlWebAuthnIT extends AbstractWebAuthnTest {

    private static final int MYSQL_PORT = 3306;

    @Container(image = "${mysql.80.image}", port = MYSQL_PORT, expectedLog = "Only MySQL server logs after this point")
    static MySqlService database = new MySqlService()
            .withProperty("MYSQL_DEFAULT_AUTHENTICATION_PLUGIN", "caching_sha2_password");

    @QuarkusApplication
    static RestService app = new RestService().withProperties("mysql.properties")
            .withProperty("quarkus.datasource.username", database::getUser)
            .withProperty("quarkus.datasource.password", database::getPassword)
            .withProperty("quarkus.datasource.reactive.url",
                    () -> database.getReactiveUrl() + "?defaultAuthenticationPlugin=caching_sha2_password");

    @Override
    protected RestService getApp() {
        return app;
    }

}
