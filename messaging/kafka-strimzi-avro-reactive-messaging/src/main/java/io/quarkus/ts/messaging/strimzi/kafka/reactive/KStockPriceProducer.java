package io.quarkus.ts.messaging.strimzi.kafka.reactive;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.ts.messaging.kafka.StockPrice;
import io.quarkus.ts.messaging.kafka.status;
import io.smallrye.common.constraint.NotNull;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

@ApplicationScoped
public class KStockPriceProducer {

    private static final Logger LOG = Logger.getLogger(KStockPriceProducer.class);
    private static final int BATCH_SIZE = 1000;

    @Inject
    @Channel("source-stock-price")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    Emitter<StockPrice> emitter;

    @ConfigProperty(name = "cron.expr.skip", defaultValue = "false")
    String skipCronJob;

    @Inject
    Vertx vertx;

    private Random random = new Random();

    public void generate(@Observes StartupEvent startupEvent) {
        vertx.executeBlocking(new Handler<Promise<Void>>() {
            @Override
            public void handle(Promise<Void> promise) {
                while (true) {
                    IntStream.range(0, BATCH_SIZE).forEach(next -> {
                        StockPrice event = StockPrice.newBuilder()
                                .setId("IBM")
                                .setPrice(random.nextDouble())
                                .setStatus(status.PENDING)
                                .build();
                        emitter.send(event).whenComplete(handlerEmitterResponse(KStockPriceProducer.class.getName()));
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void generate2(@Observes StartupEvent startupEvent) {
        try {
            Thread.sleep(258);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        vertx.executeBlocking(new Handler<Promise<Void>>() {
            @Override
            public void handle(Promise<Void> promise) {
                while (true) {
                    IntStream.range(0, BATCH_SIZE).forEach(next -> {
                        StockPrice event = StockPrice.newBuilder()
                                .setId("IBM")
                                .setPrice(random.nextDouble())
                                .setStatus(status.PENDING)
                                .build();
                        emitter.send(event).whenComplete(handlerEmitterResponse(KStockPriceProducer.class.getName()));
                    });
                    try {
                        Thread.sleep(59);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void generate3(@Observes StartupEvent startupEvent) {
        try {
            Thread.sleep(185);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        vertx.executeBlocking(new Handler<Promise<Void>>() {
            @Override
            public void handle(Promise<Void> promise) {
                while (true) {
                    IntStream.range(0, BATCH_SIZE).forEach(next -> {
                        StockPrice event = StockPrice.newBuilder()
                                .setId("IBM")
                                .setPrice(random.nextDouble())
                                .setStatus(status.PENDING)
                                .build();
                        emitter.send(event).whenComplete(handlerEmitterResponse(KStockPriceProducer.class.getName()));
                    });
                    try {
                        Thread.sleep(75);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void generate4(@Observes StartupEvent startupEvent) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        vertx.executeBlocking(new Handler<Promise<Void>>() {
            @Override
            public void handle(Promise<Void> promise) {
                while (true) {
                    IntStream.range(0, BATCH_SIZE).forEach(next -> {
                        StockPrice event = StockPrice.newBuilder()
                                .setId("IBM")
                                .setPrice(random.nextDouble())
                                .setStatus(status.PENDING)
                                .build();
                        emitter.send(event).whenComplete(handlerEmitterResponse(KStockPriceProducer.class.getName()));
                    });
                    try {
                        Thread.sleep(358);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    @NotNull
    private BiConsumer<Void, Throwable> handlerEmitterResponse(final String owner) {
        return (success, failure) -> {
            if (failure != null) {
                LOG.error(String.format("D'oh! %s", failure.getMessage()));
            }
        };
    }

}
