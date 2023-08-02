package io.quarkus.ts.transactions;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows does not support Linux Containers / Testcontainers (Jaeger)")
public class PostgresqlTransactionGeneralUsageIT extends TransactionCommons {

    static final int POSTGRESQL_PORT = 5432;

    @Container(image = "${postgresql.latest.image}", port = POSTGRESQL_PORT, expectedLog = "listening on IPv4 address", command = "--max_prepared_transactions=100")
    static final PostgresqlService database2 = new PostgresqlService().withProperty("PGDATA", "/tmp/psql");

    @Container(image = "${postgresql.latest.image}", port = POSTGRESQL_PORT, expectedLog = "listening on IPv4 address", command = "--max_prepared_transactions=100")
    static final PostgresqlService database = new PostgresqlService().withProperty("PGDATA", "/tmp/psql");

    @QuarkusApplication
    public static final RestService app = new RestService().withProperties("postgresql.properties")
            .withProperty("quarkus.otel.exporter.otlp.traces.endpoint", jaeger::getCollectorUrl)
            .withProperty("quarkus.datasource.jdbc.telemetry", "true")
            .withProperty("quarkus.artemis.username", amq.getAmqUser())
            .withProperty("quarkus.artemis.password", amq.getAmqPassword())
            .withProperty("quarkus.artemis.url", amq::getTcpUrl)
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl)
            .withProperty("quarkus.datasource.second-pg.jdbc.url", database2::getJdbcUrl);

    @Override
    protected RestService getApp() {
        return app;
    }
}
