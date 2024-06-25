package io.quarkus.ts.http.restclient.reactive;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.junit5.VertxExtension;

@ExtendWith(VertxExtension.class)
@QuarkusScenario
public class HttpSimpleEncodeModeIT {

    private static final File FILE;
    private static final String EXPECTED_CONTENT_DISPOSITION_SAMPLE = "\"Content-Disposition\":[\"form-data; name=\\\"file\\\"; filename=\\\"";
    private static final int VERTX_SERVER_PORT = 8081;
    private static HttpServer server;

    static {
        FILE = Paths.get("src", "test", "resources", "sample.txt").toFile();
    }

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperty("vertx-server-port", String.valueOf(VERTX_SERVER_PORT));

    @BeforeAll
    public static void beforeAll(Vertx vertx) {
        final var mapper = new ObjectMapper();
        vertx.createHttpServer(new HttpServerOptions().setPort(VERTX_SERVER_PORT))
                .requestHandler(httpServerRequest -> {
                    if (!httpServerRequest.path().contains("/encoder-mode")) {
                        httpServerRequest.response().setStatusCode(500).end();
                        return;
                    }

                    System.out.println("/f/////////// in here!!!"); // FIXME: remove me!
                    httpServerRequest.setExpectMultipart(true);
                    AtomicInteger fileCount = new AtomicInteger(0);
                    httpServerRequest.uploadHandler(upload -> {
                        // FIXME: using multiple files as described in https://github.com/quarkusio/quarkus/discussions/39751
                        //   but it's not enough to fail test when mode is not HTML5
                        System.out.println("Got a file upload " + upload.name()); // FIXME: remove me
                        if (fileCount.incrementAndGet() == 2) {
                            httpServerRequest.endHandler(v -> {
                                // The body has now been fully read, so retrieve the form attributes
                                var formAttributes = httpServerRequest.formAttributes();
                                var otherField = formAttributes.get("otherField");
                                try {
                                    httpServerRequest
                                            .response()
                                            .setStatusCode(200)
                                            .putHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                            // FIXME: set sensible response
                                            .end(mapper.writeValueAsString(new MyMultipartDTO(List.of())));
                                } catch (JsonProcessingException e) {
                                    // FIXME: exception handling
                                    httpServerRequest.response().setStatusCode(500).end("Ouch!");
                                }
                            });
                        }
                    });
                })
                .listen()
                .onSuccess(server -> HttpSimpleEncodeModeIT.server = server);
        // FIXME: ^^ needs to be blocking so that we know that server is ready when 'testMultipartEncodeMode' is called
    }

    @AfterAll
    public static void afterAll() {
        if (server != null) {
            // FIXME what if Vertx JUnit closes Vert.x instance first? this should be silent
            server.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "HTML5", "RFC1738", "RFC3986" }) // FIXME: use enum source!
    public void testMultipartEncodeMode(String encoderMode) {
        System.out.println("Encode mode " + encoderMode); // FIXME: remove me!

        Response response = app.given()
                .multiPart("file", FILE, "text/plain")
                .multiPart("otherField", "other field")
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .pathParam("encoder-mode", encoderMode)
                .when()
                .post("/encode/{encoder-mode}")
                .then()
                .statusCode(200)
                .extract().response();

        String capturedRequestBody = response.asString();
        // FIXME: verification
    }

}
