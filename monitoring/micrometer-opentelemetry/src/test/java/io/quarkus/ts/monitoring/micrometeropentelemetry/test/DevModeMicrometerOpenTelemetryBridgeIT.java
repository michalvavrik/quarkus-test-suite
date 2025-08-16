package io.quarkus.ts.monitoring.micrometeropentelemetry.test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
public class DevModeMicrometerOpenTelemetryBridgeIT extends AbstractMicrometerOpenTelemetryBridgeIT {

    @DevModeQuarkusApplication
    static final RestService app = new RestService();

    @Override
    protected String getLokiUrl() {
        return "";
    }

    @Override
    protected String getTempoUrl() {
        return "";
    }

    @Override
    protected String getPrometheusUrl() {
        return "";
    }
}
