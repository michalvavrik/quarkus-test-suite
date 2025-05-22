package io.quarkus.ts.hibernate.reactive;

import java.util.Map;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

@TestProfile(VertxMysqlClientFipsReproducerTest.MyTestProfile.class)
@QuarkusTest
public class VertxMysqlClientFipsReproducerTest {

    @Test
    public void reproducer() {
        RestAssured
                .given()
                .log().all()
                .filter(new ResponseLoggingFilter())
                .get("repro")
                .then()
                .statusCode(200);
    }

    public static class ReproResource {

        void observe(@Observes StartupEvent event, Pool pool, Router router) {
            router.route("/repro").handler(context -> pool
                    .query("CREATE TABLE authors (id INT NOT NULL AUTO_INCREMENT, name VARCHAR(31) NOT NULL, PRIMARY KEY(id))")
                    .execute()
                    .onComplete(result -> {
                        if (result.failed()) {
                            System.out.println("failed with: " + result.cause());
                            result.cause().printStackTrace();
                            context.fail(result.cause());
                        } else {
                            context.response().setStatusCode(200).end("Repro result: " + result.result());
                        }
                    }));
        }

    }

    public static class MyTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.datasource.db-kind", "mysql",
                    "quarkus.datasource.username", "quarkus_test",
                    "quarkus.datasource.password", "quarkus_test",
                    "quarkus.datasource.reactive.url", "mysql://localhost:3306/quarkus_test");
        }
    }
}
