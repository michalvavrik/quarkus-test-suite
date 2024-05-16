package io.quarkus.ts.sqldb.sqlapp;

import static io.quarkus.test.services.containers.DockerContainerManagedResource.DOCKER_INNER_CONTAINER;

import java.io.IOException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

import com.ibm.db2.jcc.DB2BaseDataSource;

import io.quarkus.test.bootstrap.Db2Service;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.utils.FileUtils;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

@QuarkusScenario
@Tag("podman-incompatible") //TODO: https://github.com/containers/podman/issues/16432
public class DB2DatabaseIT extends AbstractSqlDatabaseIT {

    private static final int DB2_PORT = 30171;
    private static final String JWT_PRIVATE_KEY_FILE_NAME = "jwt_private_key.pem";
    private static final Path TOKEN_PRIVATE_KEY = Path.of("target").resolve(JWT_PRIVATE_KEY_FILE_NAME).toAbsolutePath();
    private static String jwtToken = null;

    @Container(image = "${db2.image}", port = DB2_PORT, expectedLog = "Setup has completed")
    static Db2Service db2 = new Db2Service()
            .withProperty("container.privileged-mode", "true")
            .onPostStart(svc -> {
                var self = (Db2Service) svc;
                var container = self.<GenericContainer<?>> getPropertyFromContext(DOCKER_INNER_CONTAINER);
                // configure token auth https://www.ibm.com/docs/en/db2/11.5?topic=authentication-token-configuration-file
                var user = self.getUser();
                executeCommand(container, user,
                        "gsk8capicmd_64 -keydb -create -db jwtkeys.p12 -pw passw0rd -type pkcs12 -stash");
                executeCommand(container, user, "echo myreallysecretkey12345678901234567890 > mykey.txt");
                executeCommand(container, user,
                        "gsk8capicmd_64 -secretkey -add -db jwtkeys.p12 -stashed -label mySecretKey -file mykey.txt");
                executeCommand(container, user, "ssh-keygen -t rsa -b 4096 -m PEM -f jwtRS256.key -N password");
                executeCommand(container, user,
                        "openssl req -x509 -new -nodes -key jwtRS256.key -sha256 -days 1825 -out myCert.pem -passin pass:password -subj '/C=CZ/ST=JMK/L=Brno/O=RedHat/OU=Quarkus-QE/CN=localhost'");
                executeCommand(container, user,
                        "gsk8capicmd_64 -cert -add -db jwtkeys.p12 -stashed -label myCert -file myCert.pem");
                container.copyFileToContainer(Transferable.of("""
                        VERSION=1
                        TOKEN_TYPES_SUPPORTED=JWT
                        JWT_KEYDB=/database/config/%s/jwtkeys.p12
                        JWT_IDP_ISSUER=data_henrik
                        JWT_IDP_AUTHID_CLAIM=name
                        JWT_IDP_SECRETKEY_LABEL=mySecretKey
                        JWT_IDP_RSA_CERTIFICATE_LABEL=myCert
                        """.formatted(user)), "/database/config/%s/sqllib/cfg/db2token.cfg".formatted(user));
                executeCommand(container, user, "db2 update dbm cfg using SRVCON_AUTH SERVER_ENCRYPT_TOKEN");
                executeCommand(container, user, "db2stop force");
                executeCommand(container, user, "db2start");

                executeCommand(container, user,
                        "openssl pkcs8 -topk8 -inform PEM -in jwtRS256.key -out %s -nocrypt -passin pass:password"
                                .formatted(JWT_PRIVATE_KEY_FILE_NAME));
                container.copyFileFromContainer("/database/config/%s/%s".formatted(user, JWT_PRIVATE_KEY_FILE_NAME),
                        TOKEN_PRIVATE_KEY.toString());
            });

    @QuarkusApplication
    static RestService app = new RestService().withProperties("db2_app.properties")
            .withProperty("quarkus.security.security-providers", "BCFIPSJSSE")
            //            .withProperty("quarkus.security.security-providers", "IBMJSSE2,IBMJCEFIPS,IBMJCE")
            //            .withProperty("com.ibm.jsse2.JSSEFIPS", "true")
            .withProperty("quarkus.datasource.username", db2.getUser())
            .withProperty("quarkus.datasource.password", db2.getPassword())
            .withProperty(DB2BaseDataSource.propertyKey_securityMechanism, DB2BaseDataSource.TOKEN_SECURITY + "")
            .withProperty(DB2BaseDataSource.propertyKey_accessTokenType, "JWT")
            .withProperty(DB2BaseDataSource.propertyKey_accessToken, DB2DatabaseIT::getJwtToken)
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyDefault_securityMechanism,
                    DB2BaseDataSource.TOKEN_SECURITY + "")
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyKey_accessTokenType,
                    "JWT")
            .withProperty(
                    "quarkus.datasource.jdbc.additional-jdbc-properties." + DB2BaseDataSource.propertyKey_accessToken,
                    DB2DatabaseIT::getJwtToken)
            .withProperty("quarkus.datasource.jdbc.url", db2::getJdbcUrl);

    private static void executeCommand(GenericContainer<? extends GenericContainer<?>> container, String user, String cmd) {
        try {
            var result = container.execInContainer("sh", "-c", "su - %s -c \"%s\"".formatted(user, cmd));
            if (result.getExitCode() != 0) {
                // sadly it's not a rule that for an error we get the STD ERR
                String message;
                if (!result.getStderr().isEmpty()) {
                    message = "with message: %s".formatted(result.getStderr());
                } else if (!result.getStdout().isEmpty()) {
                    message = "with message: %s".formatted(result.getStdout());
                } else {
                    message = "with no message";
                }
                throw new RuntimeException("DB2 setup failed " + message);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getJwtToken() {
        if (jwtToken == null) {
            // right now, SmallRye JWT Builder only load keys from resources, so let's load it ourselves
            String privateKeyContent = FileUtils.loadFile(TOKEN_PRIVATE_KEY.toFile());
            PrivateKey privateKey = KeyUtils.tryAsPemSigningPrivateKey(privateKeyContent, SignatureAlgorithm.RS256);
            jwtToken = Jwt.issuer("data_henrik").sign(privateKey);
        }
        try {
            Connection conn = DriverManager.getConnection( "jdbc:db2://host-name-or-IP-address:50001/BLUDB:" + "accessToken=access-token;accessTokenType=JWT;" + "securityMechanism=19;sslConnection=true");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return jwtToken;
    }
}
