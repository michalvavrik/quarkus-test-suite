package io.quarkus.qe.messaging.ssl.quickstart;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

@Path("/kafka/sasl/ssl")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class SaslSslKafkaEndpoint extends KafkaEndpoint {

    @Inject
    @Named("kafka-consumer-sasl-ssl")
    KafkaConsumer<String, String> saslSslConsumer;

    @Inject
    @Named("kafka-producer-sasl-ssl")
    KafkaProducer<String, String> saslSslProducer;

    @Inject
    @Named("kafka-admin-sasl-ssl")
    AdminClient saslSslAdmin;

    public void initialize(@Observes StartupEvent ev,
            @ConfigProperty(name = "kafka.security.protocol", defaultValue = "") String kafkaSecurityProtocol) {
        if ("SASL_SSL".equals(kafkaSecurityProtocol)) {
            super.initialize(saslSslConsumer);
        }
    }

    @Path("/topics")
    @GET
    public Set<String> getTopics() throws InterruptedException, ExecutionException, TimeoutException {
        return super.getTopics(saslSslAdmin);
    }

    @POST
    public long produceEvent(@QueryParam("key") String key, @QueryParam("value") String value)
            throws InterruptedException, ExecutionException, TimeoutException {
        return super.produceEvent(saslSslProducer, key, value);
    }

    @GET
    public String getLast() {
        return super.getLast();
    }
}
