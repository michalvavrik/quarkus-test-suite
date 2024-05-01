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
    static MySqlService database = new MySqlService();

    @QuarkusApplication
    static RestService app = new RestService().withProperties("mysql.properties")
            // set https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-authentication.html#cj-conn-prop_defaultAuthenticationPlugin
            // to https://dev.mysql.com/doc/refman/8.0/en/caching-sha2-pluggable-authentication.html
            // as ATM default authentication mechanism is not supported in FIPS-enabled environment
            // TODO: re-check whether it is necessary when MySQL version changes from 8.0.x
            .withProperty("defaultAuthenticationPlugin", "caching_sha2_password")
            .withProperty("quarkus.datasource.username", database::getUser)
            .withProperty("quarkus.datasource.password", database::getPassword)
            .withProperty("quarkus.datasource.reactive.url", database::getReactiveUrl);

    @Override
    protected RestService getApp() {
        return app;
    }

}
