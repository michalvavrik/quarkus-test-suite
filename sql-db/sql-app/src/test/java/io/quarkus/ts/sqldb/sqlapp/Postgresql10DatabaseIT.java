package io.quarkus.ts.sqldb.sqlapp;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class Postgresql10DatabaseIT extends AbstractSqlDatabaseIT {

    static final String POSTGRESQL_USER = "user";
    static final String POSTGRESQL_PASSWORD = "user";
    static final String POSTGRESQL_DATABASE = "mydb";
    static final int POSTGRESQL_PORT = 5432;

    @Container(image = "${postgresql.10.image}", port = POSTGRESQL_PORT, expectedLog = "listening on IPv4 address")
    static DefaultService database = new DefaultService()
            .withProperty("POSTGRESQL_USER", POSTGRESQL_USER)
            .withProperty("POSTGRESQL_PASSWORD", POSTGRESQL_PASSWORD)
            .withProperty("POSTGRESQL_DATABASE", POSTGRESQL_DATABASE);

    @QuarkusApplication
    static RestService app = new RestService().withProperties("postgresql.properties")
            .withProperty("quarkus.datasource.username", POSTGRESQL_USER)
            .withProperty("quarkus.datasource.password", POSTGRESQL_PASSWORD)
            .withProperty("quarkus.datasource.jdbc.url",
                    () -> database.getHost().replace("http", "jdbc:postgresql") + ":" + database.getPort() + "/"
                            + POSTGRESQL_DATABASE);
}