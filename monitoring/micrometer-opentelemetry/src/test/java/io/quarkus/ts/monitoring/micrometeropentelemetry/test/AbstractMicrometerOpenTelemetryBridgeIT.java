package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.LookupService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.utils.AwaitilityUtils;
import io.restassured.response.ValidatableResponse;

public abstract class AbstractMicrometerOpenTelemetryBridgeIT {

    @LookupService
    static RestService app;

    protected abstract String getLokiUrl();

    protected abstract String getTempoUrl();

    protected abstract String getPrometheusUrl();

    @Test
    void testOpenTelemetryTracing() {
        String traceId = app.given().get("/traces/trace-id").then().statusCode(200).body(notNullValue()).extract().asString();
        AwaitilityUtils.untilAsserted(() -> queryTempoByTraceId(traceId)
                .body("trace.resourceSpans.size()", greaterThanOrEqualTo(1))
                .body("trace.resourceSpans.flatten().resource.attributes.flatten().find { it.key == 'service.name' }.value.stringValue",
                        equalTo("app"))
                .body("trace.resourceSpans.flatten().scopeSpans.flatten().spans.flatten().attributes?.flatten().find { it.key == 'url.path' }.value.stringValue",
                        equalTo("/traces/trace-id")));
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
        callMetricsEndpoint();
        callMetricsEndpoint();
        callMetricsEndpoint();
        // FIXME: alter next 2 lines
        var body = queryPrometheusAndWaitForMetrics("hello_metrics_invocations_total").extract().body().asPrettyString();
        System.out.printf("body iiiiiiiiiiisssssssssssssss: %s", body);
    }

    @Test
    void testCustomMicrometerMetrics() {

    }

    @Test
    void testBuiltinMicrometerMetrics() {

    }

    private ValidatableResponse queryLoki(Instant start) {
        String startTimestamp = String.valueOf(start.getEpochSecond());
        String endTimestamp = String.valueOf(Instant.now().plusSeconds(20).getEpochSecond());
        return given().when()
                .queryParam("query", "{service_name=\"app\"}")
                .queryParam("limit", 500)
                .queryParam("direction", "forward")
                .queryParam("start", startTimestamp)
                .queryParam("end", endTimestamp)
                .get(getLokiUrl() + "/loki/api/v1/query_range")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"));
    }

    private ValidatableResponse queryTempoByTraceId(String traceId) {
        return given().when()
                .get(getTempoUrl() + "/api/v2/traces/" + traceId)
                .then()
                .statusCode(200);
    }

    private ValidatableResponse queryPrometheusAndWaitForMetrics(String metric) {
        AwaitilityUtils.untilAsserted(() -> queryPrometheus(metric));
        return queryPrometheus(metric);
    }

    private ValidatableResponse queryPrometheus(String metric) {
        return given()
                .queryParam("query", metric)
                .get(getPrometheusUrl() + "/api/v1/query")
                .then().statusCode(200)
                .body("status", is("success"))
                .body("data?.result?.flatten().size()", Matchers.greaterThanOrEqualTo(1));
    }

    private static void callMetricsEndpoint() {
        app.given()
                .get("/metrics")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("hello-metrics"));
    }
}
