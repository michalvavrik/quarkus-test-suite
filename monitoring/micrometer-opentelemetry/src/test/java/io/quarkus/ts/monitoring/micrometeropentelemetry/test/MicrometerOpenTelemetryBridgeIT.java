package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import static io.restassured.RestAssured.given;

import io.quarkus.test.bootstrap.GrafanaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GrafanaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.ValidatableResponse;

@QuarkusScenario
public class MicrometerOpenTelemetryBridgeIT extends AbstractMicrometerOpenTelemetryBridgeIT {

    private static final Integer BULK_SIZE = 10;

    @GrafanaContainer
    static final GrafanaService grafana = new GrafanaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.otel.logs.enabled", "true")
            .withProperty("quarkus.log.level", "DEBUG")
            .withProperty("quarkus.application.name", "app")
            .withProperty("quarkus.otel.blrp.schedule.delay", "PT5S")
            .withProperty("quarkus.otel.blrp.max.export.batch.size", BULK_SIZE.toString())
            .withProperty("quarkus.otel.exporter.otlp.logs.endpoint", grafana::getOtlpCollectorUrl);

    @Override
    protected ValidatableResponse retrieveAllLogs() {
        return given().when()
                .queryParam("query", "{service_name=\"app\"}")
                .queryParam("limit", 500)
                .get(grafana.getRestUrl() + "/loki/api/v1/query_range")
                .then();
    }
}
