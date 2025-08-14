package io.quarkus.ts.http.grpc;

import org.junit.jupiter.api.Test;

import io.vertx.mutiny.ext.web.client.WebClient;

public interface ServerCustomizationIT {

    WebClient getWebClient();

    @Test
    default void todo() {

    }

}
