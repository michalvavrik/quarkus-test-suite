package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import org.junit.jupiter.api.Disabled;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

@Disabled("https://github.com/quarkusio/quarkus/issues/49571")
@QuarkusScenario
public class DevModeMicrometerOpenTelemetryBridgeIT extends AbstractMicrometerOpenTelemetryBridgeIT {

    @DevModeQuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.observability.enabled", "true");

    @Override
    protected String getLokiUrl() {
        // FIXME: impl. me when this test class is enabled
        return "";
    }

    @Override
    protected String getTempoUrl() {
        // FIXME: impl. me when this test class is enabled
        return "";
    }

    @Override
    protected String getPrometheusUrl() {
        // FIXME: impl. me when this test class is enabled
        return "";
    }
}
