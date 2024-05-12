package io.quarkus.ts.sqldb.sqlapp;

import static io.quarkus.test.services.containers.DockerContainerManagedResource.DOCKER_INNER_CONTAINER;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.containers.GenericContainer;

import com.ibm.db2.jcc.DB2BaseDataSource;

import io.quarkus.test.bootstrap.Db2Service;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@Tag("podman-incompatible") //TODO: https://github.com/containers/podman/issues/16432
public class DB2DatabaseIT extends AbstractSqlDatabaseIT {

    private static final int DB2_PORT = 30171;

    @Container(image = "${db2.image}", port = DB2_PORT, expectedLog = "Setup has completed")
    static Db2Service db2 = new Db2Service()
            .withProperty("container.privileged-mode", "true")
            .onPostStart(svc -> {
                var self = (Db2Service) svc;
                var container = self.<GenericContainer<?>> getPropertyFromContext(DOCKER_INNER_CONTAINER);
                executeCommand(container, self.getUser(), "db2", "update", "dbm", "cfg", "using", "SSL_SVR_KEYDB",
                        "/mydb.kdb");
                executeCommand(container, self.getUser(), "db2", "update", "dbm", "cfg", "using", "SSL_SVR_STASH",
                        "/mydb.sth");
                executeCommand(container, self.getUser(), "db2", "update", "dbm", "cfg", "using", "SSL_SVR_LABEL",
                        "myselfsigned");
                executeCommand(container, self.getUser(), "db2", "update", "dbm", "cfg", "using", "SSL_SVCENAME",
                        DB2_PORT + "");
                executeCommand(container, self.getUser(), "db2set", "-i", self.getUser(), "DB2COMM=SSL");
                executeCommand(container, self.getUser(), "db2stop", "force");
                executeCommand(container, self.getUser(), "db2start");
            });

    @QuarkusApplication
    static RestService app = new RestService().withProperties("db2_app.properties")
            .withProperty("quarkus.security.security-providers", "BCFIPSJSSE")
            //            .withProperty("quarkus.security.security-providers", "IBMJSSE2,IBMJCEFIPS,IBMJCE")
            //            .withProperty("com.ibm.jsse2.JSSEFIPS", "true")
            .withProperty("quarkus.datasource.username", db2.getUser())
            .withProperty("quarkus.datasource.password", db2.getPassword())
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyKey_securityMechanism,
                    DB2BaseDataSource.CLEAR_TEXT_PASSWORD_SECURITY + "")
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyKey_sslTrustStoreLocation,
                    () -> Path.of("src").resolve("main").resolve("resources").resolve("server-truststore.p12").toAbsolutePath()
                            .toString())
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyKey_sslTrustStorePassword,
                    "changeit")
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyKey_sslConnection, "true")
            .withProperty("quarkus.datasource.jdbc.url", db2::getJdbcUrl);

    private static void executeCommand(GenericContainer<? extends GenericContainer<?>> container, String user, String... cmd) {
        try {
            var config = ExecConfig.builder()
                    .user(user)
                    // PATH value as received by command 'env' after 'docker exec -ti container-id bash -c "su - user"'
                    .envVars(Map.of("PATH",
                            "/database/config/" +
                                    user +
                                    "/.local/bin:/database/config/" +
                                    user +
                                    "/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/database/config/" +
                                    user +
                                    "/sqllib/bin:/database/config/" +
                                    user +
                                    "/sqllib/adm:/database/config/" +
                                    user +
                                    "/sqllib/misc:/database/config/" +
                                    user +
                                    "/sqllib/pd:/database/config/" +
                                    user +
                                    "/sqllib/gskit/bin:/database/config/" +
                                    user +
                                    "/sqllib/db2tss/bin"))
                    .command(cmd)
                    .build();
            var result = container.execInContainer(config);
            if (!result.getStderr().isEmpty()) {
                throw new RuntimeException("DB2 setup failed with error: " + result.getStderr());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
