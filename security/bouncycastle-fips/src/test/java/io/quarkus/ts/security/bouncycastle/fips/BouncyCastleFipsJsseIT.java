package io.quarkus.ts.security.bouncycastle.fips;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

@QuarkusScenario
public class BouncyCastleFipsJsseIT {

    private static final String PASSWORD = "password";
    private static final String BCFIPS = BouncyCastleFipsProvider.PROVIDER_NAME;
    private static final String BCJSSE = "BCJSSE";
    private static final String KS_TYPE = "BCFKS";

    @QuarkusApplication(ssl = true, dependencies = {
            @Dependency(groupId = "org.bouncycastle", artifactId = "bctls-fips", version = "${bouncycastle.bctls-fips.version}")
    })
    private static final RestService app = new RestService().withProperties("jsse.properties");

    @Test
    public void verifyBouncyCastleFipsAndJsseProviderAvailability() {
        BouncyCastleFipsProvider provider = new BouncyCastleFipsProvider();
        Security.insertProviderAt(provider, 1);
        String expectedResp = String.join(",", List.of(BCFIPS, BCJSSE));
        given()
                .config(RestAssured
                        .config()
                        .sslConfig(
                                new SSLConfig()
                                        .sslSocketFactory(sslSocketFactory(provider))))
                .when()
                .get("/api/listProviders")
                .then()
                .statusCode(200)
                .body(containsString(expectedResp));
    }

    private SSLSocketFactory sslSocketFactory(
            BouncyCastleFipsProvider provider) {

        SSLSocketFactory clientAuthFactory = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(KS_TYPE);
            keyStore.load(new FileInputStream(Paths.get("src", "test", "resources", "client-keystore.jks").toFile()),
                    PASSWORD.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, PASSWORD.toCharArray());
            KeyManager[] kms = kmf.getKeyManagers();

            KeyStore trustStore = KeyStore.getInstance(KS_TYPE);
            keyStore.load(new FileInputStream(Paths.get("src", "test", "resources", "client-truststore.jks").toFile()),
                    PASSWORD.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            //Adds the truststore to the factory
            tmf.init(trustStore);
            //This is passed to the SSLContext init method
            TrustManager[] tms = tmf.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kms, tms, SecureRandom.getInstance("DEFAULT", provider));

            clientAuthFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Exception e) {
            System.out.println("Error while loading keystore: " + e);
        }
        return clientAuthFactory;
    }

}
