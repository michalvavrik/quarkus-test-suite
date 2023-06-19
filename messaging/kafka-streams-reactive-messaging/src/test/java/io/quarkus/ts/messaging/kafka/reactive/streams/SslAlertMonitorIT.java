package io.quarkus.ts.messaging.kafka.reactive.streams;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;

@QuarkusScenario
public class SslAlertMonitorIT extends BaseKafkaStreamTest {
    /**
     * We can't rename this file to use the default SSL settings part of KafkaService.
     */
    private static final String TRUSTSTORE_FILE = "strimzi-server-ssl-truststore.p12";

    @KafkaContainer(vendor = KafkaVendor.STRIMZI, protocol = KafkaProtocol.SSL, kafkaConfigResources = TRUSTSTORE_FILE, version = "latest-kafka-3.5.0")
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl)
            .withProperty("kafka.security.protocol", "SSL")
            .withProperty("kafka.ssl.truststore.location", TRUSTSTORE_FILE)
            .withProperty("kafka.ssl.truststore.password", "top-secret")
            .withProperty("kafka.ssl-engine-factory-class", "org.apache.kafka.common.security.auth.SslEngineFactory")
            .withProperty("kafka.ssl.truststore.type", "PKCS12");

    @Override
    protected String getAppUrl() {
        return app.getHost() + ":" + app.getPort();
    }
}
