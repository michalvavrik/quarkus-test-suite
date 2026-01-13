package io.quarkus.ts.messaging.kafka.reactive.streams.shutdown;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.jboss.logging.Logger;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.reactive.messaging.kafka.KafkaConsumerRebalanceListener;

@ApplicationScoped
@Identifier("slow.rebalancer")
public class SlowTopicRebalanceListener implements KafkaConsumerRebalanceListener {

    private static final Logger LOG = Logger.getLogger(SlowTopicRebalanceListener.class);

    private final CountDownLatch partitionsAssigned = new CountDownLatch(1);

    @Override
    public void onPartitionsRevoked(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        LOG.infof("Partitions revoked: %s", partitions);
    }

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        LOG.infof("Partitions assigned: %s", partitions);
        if (!partitions.isEmpty()) {
            partitionsAssigned.countDown();
        }
    }

    /**
     * Wait for partitions to be assigned to the consumer.
     * This ensures the consumer is ready to receive messages.
     *
     * @throws InterruptedException if the wait is interrupted
     */
    void awaitPartitionsAssigned() throws InterruptedException {
        partitionsAssigned.await();
        LOG.info("Partition assignment completed, consumer is ready");
    }
}
