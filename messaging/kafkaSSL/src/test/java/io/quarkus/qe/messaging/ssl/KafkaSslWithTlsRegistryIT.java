package io.quarkus.qe.messaging.ssl;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;

@QuarkusScenario
public class KafkaSslWithTlsRegistryIT {

    @KafkaContainer(vendor = KafkaVendor.STRIMZI, protocol = KafkaProtocol.SSL, tlsRegistryEnabled = true, tlsConfigName = "kafka-ssl")
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl)
            .withProperties(kafka::getSslProperties)
            .withProperty("kafka-streams.state.dir", "target")
            .withProperty("kafka-client-ssl.bootstrap.servers", kafka::getBootstrapUrl);

    @Test
    void testKafkaClientSSL() {
        await().untilAsserted(() -> {
            pushEvent("my-key", "my-value");
            verifyEventWasProcessed("my-key-my-value");
            pushEvent("my-key", "my-value-two");
            verifyEventWasProcessed("my-key-my-value-two");
        });

        get("/kafka/ssl/topics")
                .then()
                .statusCode(200)
                .body(StringContains.containsString("hello"));
    }

    private void pushEvent(String key, String value) {
        given()
                .queryParam("key", key)
                .queryParam("value", value)
                .when()
                .post("/kafka/ssl")
                .then()
                .statusCode(200);
    }

    private void verifyEventWasProcessed(String expectedEvent) {
        get("/kafka/ssl")
                .then()
                .statusCode(200)
                .body(StringContains.containsString(expectedEvent));
    }
}
