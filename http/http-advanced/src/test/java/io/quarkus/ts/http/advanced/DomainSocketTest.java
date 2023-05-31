package io.quarkus.ts.http.advanced;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;

@QuarkusTest
public class DomainSocketTest {

    @Test
    public void ensureApplicationProvidesContent() {
        Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
        WebClient client = WebClient.create(vertx, new WebClientOptions().setFollowRedirects(false));
        SocketAddress serverAddress = SocketAddress.domainSocketAddress("/tmp/io.quarkus.app.socket");

        AtomicReference<String> response = new AtomicReference<>();
        client
                .request(
                        HttpMethod.GET,
                        serverAddress,
                        8080,
                        "localhost",
                        "/api/hello")
                .expect(ResponsePredicate.SC_OK)
                .as(BodyCodec.jsonObject())
                .send()
                .onSuccess(res -> response.set(res.body().toString()))
                .onFailure(err -> response.set(err.getMessage()));

        await().atMost(3, TimeUnit.SECONDS).until(() -> response.get() != null);
    }
}
