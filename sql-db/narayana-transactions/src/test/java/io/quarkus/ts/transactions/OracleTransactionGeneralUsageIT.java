package io.quarkus.ts.transactions;

import static io.quarkus.test.services.containers.DockerContainerManagedResource.DOCKER_INNER_CONTAINER;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.OracleService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.Mount;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.transactions.recovery.TransactionExecutor;

@QuarkusScenario
@DisabledIfSystemProperty(named = "ts.arm.missing.services.excludes", matches = "true", disabledReason = "https://github.com/quarkus-qe/quarkus-test-suite/issues/2022")
public class OracleTransactionGeneralUsageIT extends TransactionCommons {

    private static final String GRANT_XA_TRANSACTIONS_SQL_PATH = "/tmp/grant_xa_transactions.sql";
    private static final int ORACLE_PORT = 1521;
    private static final String DATABASE = "mydb";
    private static final String SECOND_DATABASE = DATABASE + 2;

    @Container(image = "${oracle.image}", port = ORACLE_PORT, expectedLog = "DATABASE IS READY TO USE!", mounts = {
            @Mount(from = "oracle_grant_xa_transactions.sql", to = GRANT_XA_TRANSACTIONS_SQL_PATH)
    })
    static OracleService database = new OracleService()
            .withDatabase(DATABASE + "," + SECOND_DATABASE)
            .onPostStart(service -> {
                // enable XA transactions by executing the 'oracle_grant_xa_transactions.sql' file as sysdba
                var self = (OracleService) service;
                try {
                    var execResult = self
                            .<GenericContainer<?>> getPropertyFromContext(DOCKER_INNER_CONTAINER)
                            .execInContainer("sqlplus", "-s", "sys/user@localhost:1521/mydb as sysdba",
                                    "@$ORACLE_HOME/rdbms/admin/xaview.sql");
                    if (execResult.getStderr() != null && !execResult.getStderr().isBlank()) {
                        Assertions.fail("Failed to create views for XA transaction management: " + execResult.getStderr());
                    }
                    execResult = self
                            .<GenericContainer<?>> getPropertyFromContext(DOCKER_INNER_CONTAINER)
                            .execInContainer("sqlplus", "-s", "sys/user@localhost:1521/mydb as sysdba",
                                    "@" + GRANT_XA_TRANSACTIONS_SQL_PATH);
                    if (execResult.getStderr() != null && !execResult.getStderr().isBlank()) {
                        Assertions.fail(
                                "Failed to enable grant XA transactions to " + self.getUser() + ": " + execResult.getStderr());
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

    @QuarkusApplication
    static RestService app = new RestService().withProperties("oracle.properties")
            .withProperty("quarkus.otel.exporter.otlp.traces.endpoint", jaeger::getCollectorUrl)
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", () -> {
                String jdbcUrl = database.getJdbcUrl();
                return jdbcUrl.substring(0, jdbcUrl.length() - SECOND_DATABASE.length() - 1);
            });

    @Override
    protected RestService getApp() {
        return app;
    }

    @Override
    protected TransactionExecutor getTransactionExecutorUsedForRecovery() {
        return TransactionExecutor.QUARKUS_TRANSACTION_CALL;
    }

    @Override
    protected Operation[] getExpectedJdbcOperations() {
        return new Operation[] { new Operation("SELECT mydb.dual"), new Operation("INSERT mydb.journal"),
                new Operation("UPDATE mydb.account") };
    }

}
