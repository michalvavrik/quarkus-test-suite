package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.RestService;
import io.restassured.response.ValidatableResponse;

public abstract class AbstractMicrometerOpenTelemetryBridgeIT {

    @LookupService
    static RestService app;

    protected abstract ValidatableResponse retrieveAllLogs();

    @Test
    void testOpenTelemetryTracing() {

    }

    @Test
    void testOpenTelemetryLogging() {
        app.given().get("/logging").then().statusCode(200).body(is("This is logging resource"));
        retrieveAllLogs()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("data.result.values.flatten().flatten()", hasItems(
                        "This is an error 2",
                        "This is an error 1",
                        "This is a warning 2",
                        "This is a warning 1",
                        "This is a debug message 2",
                        "This is a debug message 1",
                        "This is info message 2",
                        "This is info message 1"));
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
