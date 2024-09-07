package io.quarkus.qe.messaging.ssl.providers;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.config.SslConfigs;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.common.annotation.Identifier;

public class SslKafkaProvider extends KafkaProviders {

    @ConfigProperty(name = "kafka-client-ssl.bootstrap.servers", defaultValue = "localhost:9092")
    String sslKafkaBootStrap;

    @ConfigProperty(name = "kafka.ssl.truststore.location", defaultValue = "server.jks")
    String trustStoreFile;

    @ConfigProperty(name = "kafka.ssl.truststore.password", defaultValue = "top-secret")
    String trustStorePassword;

    @ConfigProperty(name = "kafka.ssl.truststore.type", defaultValue = "PKCS12")
    String trustStoreType;

    @Inject
    TlsConfigurationRegistry tlsConfigRegistry;

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;

    @Singleton
    @Produces
    @Named("kafka-consumer-ssl")
    KafkaConsumer<String, String> getSslConsumer() {
        Properties props = setupConsumerProperties(sslKafkaBootStrap);
        sslSetup(props);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-ssl-consumer"));
        return consumer;
    }

    @Singleton
    @Produces
    @Named("kafka-producer-ssl")
    KafkaProducer<String, String> getSslProducer() {
        Properties props = setupProducerProperties(sslKafkaBootStrap);
        sslSetup(props);
        return new KafkaProducer<>(props);
    }

    protected void sslSetup(Properties props) {
        if (isTlsConfigScenario()) {
            Map<String, Object> config = new HashMap<>();
            for (Map.Entry<String, Object> entry : kafkaConfig.entrySet()) {
                if (AdminClientConfig.configNames().contains(entry.getKey())) {
                    config.put(entry.getKey(), entry.getValue().toString());
                }
            }
            props.putAll(config);
        } else {
            File tsFile = new File(trustStoreFile);
            props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tsFile.getPath());
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, trustStoreType);
            props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        }
    }

    private boolean isTlsConfigScenario() {
        return tlsConfigRegistry.get("kafka-ssl").isPresent();
    }

}
