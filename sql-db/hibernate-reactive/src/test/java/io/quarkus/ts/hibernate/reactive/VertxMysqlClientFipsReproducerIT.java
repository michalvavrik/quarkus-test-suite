package io.quarkus.ts.hibernate.reactive;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.filter.log.ResponseLoggingFilter;

@QuarkusScenario
public class VertxMysqlClientFipsReproducerIT {

    @QuarkusApplication(classes = ReproResource.class)
    static RestService app = new RestService();

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
