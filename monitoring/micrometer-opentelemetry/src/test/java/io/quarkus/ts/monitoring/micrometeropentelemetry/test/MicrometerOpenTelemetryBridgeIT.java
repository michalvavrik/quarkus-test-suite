package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;

import io.quarkus.test.bootstrap.GrafanaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GrafanaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.ValidatableResponse;

@QuarkusScenario
public class MicrometerOpenTelemetryBridgeIT extends AbstractMicrometerOpenTelemetryBridgeIT {

    @GrafanaContainer
    static final GrafanaService grafana = new GrafanaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("otlp-collector-url", grafana::getOtlpCollectorUrl);

    @Override
    protected ValidatableResponse queryLoki(Instant start) {
        String startTimestamp = String.valueOf(start.getEpochSecond());
        String endTimestamp = String.valueOf(Instant.now().plusSeconds(20).getEpochSecond());
        return given().when()
                .queryParam("query", "{service_name=\"app\"}")
                .queryParam("limit", 500)
                .queryParam("direction", "forward")
                .queryParam("start", startTimestamp)
                .queryParam("end", endTimestamp)
                .get(grafana.getRestUrl() + "/loki/api/v1/query_range")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"));
    }
}
