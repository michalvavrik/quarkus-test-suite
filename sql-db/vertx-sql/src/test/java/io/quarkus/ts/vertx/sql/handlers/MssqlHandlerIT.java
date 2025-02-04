package io.quarkus.ts.vertx.sql.handlers;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.bootstrap.SqlServerService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.SqlServerContainer;

@QuarkusScenario
public class MssqlHandlerIT {

    @SqlServerContainer
    static SqlServerService database = new SqlServerService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.datasource.mssql.username", database.getUser())
            .withProperty("quarkus.datasource.mssql.password", database.getPassword())
            .withProperty("quarkus.datasource.mssql.jdbc.url", database::getJdbcUrl)
            .withProperty("quarkus.datasource.mssql.reactive.url", database::getReactiveUrl)
            .withProperty("quarkus.datasource.mssql.jdbc.additional-jdbc-properties.trustServerCertificate", "true")
            .withProperty("quarkus.flyway.mssql.migrate-at-start", "true");

    @Test
    public void testMssqlHandler() {
        // must be here to run the test
    }
}
