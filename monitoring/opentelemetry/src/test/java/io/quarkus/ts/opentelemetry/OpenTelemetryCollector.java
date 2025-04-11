package io.quarkus.ts.opentelemetry;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

/**
 * This simplistic collector allows us to test Vert.x-based traces exporter in Quarkus without starting a container.
 */
class OpenTelemetryCollector implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(OpenTelemetryCollector.class.getName());
    private static final int OTEL_COLLECTOR_PORT = 4317;

    private final Vertx vertx;
    private final Closeable backEnd;

    OpenTelemetryCollector() {
        vertx = Vertx.vertx();
        this.backEnd = new GRPCTraceHandler(vertx);
    }

    String url() {
        return "http://localhost:" + OTEL_COLLECTOR_PORT;
    }

    @Override
    public void close() throws IOException {
        backEnd.close();
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    class GRPCTraceHandler implements Closeable {
        private final HttpServer httpServer;

        public GRPCTraceHandler(Vertx vertx) {
            GrpcServer grpcHandler = GrpcServer.server(vertx);
            grpcHandler.callHandler(new Handler<GrpcServerRequest<Buffer, Buffer>>() {
                @Override
                public void handle(GrpcServerRequest<Buffer, Buffer> req) {
                    req.messageHandler(new Handler<GrpcMessage>() {
                        @Override
                        public void handle(GrpcMessage grpcMessage) {
                            String payload = grpcMessage.payload().toString(StandardCharsets.UTF_8);
                            if (payload.contains("twenty")) {
                                LOGGER.info("Received grpc message: bulk warning " + payload);
                            } else if (payload.contains("This is a warning")) {
                                LOGGER.info("Received grpc message: This is a warning");
                            }
                        }
                    });
                    req.endHandler(v -> {
                        // https://opentelemetry.io/docs/specs/otlp/#full-success
                        req.response().status(GrpcStatus.OK).end();
                    });
                }
            });
            httpServer = vertx
                    .createHttpServer()
                    .requestHandler(grpcHandler);
            httpServer.listen(OTEL_COLLECTOR_PORT);
            LOGGER.info("The listener started!");
        }

        @Override
        public void close() {
            LOGGER.info("Closing the listener");
            httpServer.close().toCompletionStage().toCompletableFuture().join();
            LOGGER.info("The listener was closed");
        }
    }
}
