package io.quarkus.ts.hibernate.reactive;

import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Pool;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.MySqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.filter.log.ResponseLoggingFilter;

@QuarkusScenario
public class VertxMySqlFipsReroducer {

    private static final String MYSQL_USER = "quarkus_test";
    private static final String MYSQL_PASSWORD = "quarkus_test";
    private static final String MYSQL_DATABASE = "quarkus_test";
    private static final int MYSQL_PORT = 3306;

    @Container(image = "${mysql.80.image}", port = MYSQL_PORT, expectedLog = "Only MySQL server logs after this point")
    static MySqlService database = new MySqlService()
            .withUser(MYSQL_USER)
            .withPassword(MYSQL_PASSWORD)
            .withDatabase(MYSQL_DATABASE);

    @QuarkusApplication(classes = ReproResource.class)
    static RestService app = new RestService()
            .withProperty("quarkus.datasource.username", MYSQL_USER)
            .withProperty("quarkus.datasource.password", MYSQL_PASSWORD)
            .withProperty("quarkus.datasource.reactive.url", database::getReactiveUrl);

    @Test
    public void reproducer() {
        app.given()
                .log().all()
                .filter(new ResponseLoggingFilter())
                .get("repro")
                .then()
                .statusCode(200);
    }
}
