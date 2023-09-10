package io.quarkus.ts.transactions;

import io.quarkus.test.bootstrap.OracleService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.transactions.recovery.TransactionExecutor;

@QuarkusScenario
public class OracleTransactionGeneralUsageIT extends TransactionCommons {

    static final int ORACLE_PORT = 1521;

    @Container(image = "${oracle.image}", port = ORACLE_PORT, expectedLog = "DATABASE IS READY TO USE!")
    static OracleService database = new OracleService();

    @QuarkusApplication
    static RestService app = new RestService().withProperties("oracle.properties")
            .withProperty("quarkus.otel.exporter.otlp.traces.endpoint", jaeger::getCollectorUrl)
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl)
            //            .withProperty("quarkus.datasource.xa-ds-1.jdbc.driver",
            //                    "io.quarkus.ts.transactions.recovery.driver.CrashingOracleXADataSource")
            .withProperty("quarkus.datasource.xa-ds-1.jdbc.driver",
                    "oracle.jdbc.xa.client.OracleXADataSource")
            .withProperty("quarkus.datasource.oracle-configure-xa.username", "sys as sysdba")
            .withProperty("quarkus.datasource.oracle-configure-xa.password", database.getPassword())
            .withProperty("quarkus.datasource.oracle-configure-xa.db-kind", "oracle")
            .withProperty("quarkus.datasource.oracle-configure-xa.jdbc.url", database::getJdbcUrl);

    @Override
    protected RestService getApp() {
        return app;
    }

    @Override
    protected TransactionExecutor getTransactionExecutorUsedForRecovery() {
        return TransactionExecutor.STATIC_USER_TRANSACTION;
    }

    @Override
    protected String[] getExpectedJdbcOperationNames() {
        return new String[] { "SELECT mydb", "INSERT mydb.journal", "UPDATE mydb.account" };
    }

}
