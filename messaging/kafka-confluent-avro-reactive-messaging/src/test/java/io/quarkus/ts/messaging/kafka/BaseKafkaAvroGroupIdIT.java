package io.quarkus.ts.messaging.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.restassured.http.ContentType;

abstract class BaseKafkaAvroGroupIdIT {
    private static final int TIMEOUT_SEC = 10;
    private static final int EVENTS_AMOUNT = 5;

    private String endpoint;
    private Client client = ClientBuilder.newClient();
    private List<String> receive = new CopyOnWriteArrayList<>();
    private boolean completed;
    private Random rand = new Random();

    @Test
    public void testAlertMonitorEventStream() throws InterruptedException {
        GivenSomeStockPrices(getAppA(), EVENTS_AMOUNT);
        AndApplicationEndpoint(getEndpoint(getAppA()) + "/stock-price/stream");
        whenRequestSomeEvents(EVENTS_AMOUNT);
        thenVerifyAllEventsArrived();
        // Application B should have a different auto-generated group ID so,
        // double the number of events
        GivenSomeStockPrices(getAppB(), EVENTS_AMOUNT);
        AndApplicationEndpoint(getEndpoint(getAppB()) + "/stock-price/stream");
        whenRequestSomeEvents(EVENTS_AMOUNT + EVENTS_AMOUNT);
        thenVerifyAllEventsArrived();
    }

    protected abstract RestService getAppA();

    protected abstract RestService getAppB();

    private void GivenSomeStockPrices(RestService app, int amount) {
        IntStream.range(0, amount).forEach(i -> app.given()
                .contentType(ContentType.JSON)
                .body(randomStockPrice())
                .post("/stock-price")
                .then()
                .statusCode(202));
    }

    private void AndApplicationEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    private void whenRequestSomeEvents(int expectedAmount) {
        AtomicInteger totalAmountReceived = new AtomicInteger(0);
        try {
            WebTarget target = client.target(endpoint);
            CountDownLatch latch = new CountDownLatch(expectedAmount);
            SseEventSource source = SseEventSource.target(target).build();
            source.register(inboundSseEvent -> {
                // FIXME: remove logging once flaky tests are fixed
                final var data = inboundSseEvent.readData(String.class, MediaType.APPLICATION_JSON_TYPE);
                receive.add(data);
                totalAmountReceived.incrementAndGet();
                System.out.printf("Received inboundSseEvent '%s' %n total amount is '%d' %n 'receive' is %s.%n%n%n", data,
                        totalAmountReceived.get(), receive);
            });

            source.open();
            latch.await(TIMEOUT_SEC, TimeUnit.SECONDS);
            source.close();
        } catch (InterruptedException ex) {
            // Force a timeout in order to double-check if we receive more events than the expected ones
        } finally {
            int received = totalAmountReceived.get();
            assertEquals(expectedAmount, received, "You should not process more msg than the expected ones");
            completed = expectedAmount == received;
        }
    }

    private void thenVerifyAllEventsArrived() {
        assertTrue(completed, "Not all expected kafka events has been consumed.");
    }

    private String getEndpoint(RestService app) {
        return app.getHost() + ":" + app.getPort();
    }

    private StockPriceDto randomStockPrice() {
        StockPriceDto stockPriceDto = new StockPriceDto();
        stockPriceDto.setId(UUID.randomUUID().toString());
        stockPriceDto.setValue(rand.nextInt());
        return stockPriceDto;
    }
}
