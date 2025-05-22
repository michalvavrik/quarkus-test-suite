package io.quarkus.ts.hibernate.reactive;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

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
}
