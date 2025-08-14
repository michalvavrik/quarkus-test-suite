package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.utils.AwaitilityUtils;
import io.restassured.response.ValidatableResponse;

public abstract class AbstractMicrometerOpenTelemetryBridgeIT {

    @LookupService
    static RestService app;

    protected abstract ValidatableResponse queryLoki(Instant start);

    @Test
    void testOpenTelemetryTracing() {
        Instant now = Instant.now();
        app.given().pathParam("name", "Jakub").get("/traces/{name}").then().statusCode(200).body(is("Traced Hello to Jakub"));
        System.out.println("pretty string is " + queryLoki(now).extract().asPrettyString()); // FIXME: remove me!
    }

    @Test
    void testOpenTelemetryLogging() {
        Instant now = Instant.now();
        app.given().get("/logging").then().statusCode(200).body(is("This is logging resource"));
        AwaitilityUtils.untilAsserted(() -> queryLoki(now)
                .body("data.result.values.flatten().flatten()", hasItems(
                        "This is an error 2",
                        "This is an error 1",
                        "This is a warning 2",
                        "This is a warning 1",
                        "This is a debug message 2",
                        "This is a debug message 1",
                        "This is info message 2",
                        "This is info message 1")));
    }

    @Test
    void testOpenTelemetryMetrics() {

    }

    @Test
    void testCustomMicrometerMetrics() {

    }

    @Test
    void testBuiltinMicrometerMetrics() {

    }
}
