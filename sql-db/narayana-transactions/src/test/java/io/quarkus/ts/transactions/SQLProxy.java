package io.quarkus.ts.transactions;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.logging.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class SQLProxy extends AbstractVerticle {

    private final String backendHost;
    private final int backendPort;
    private final int proxyPort;
    private final AtomicLong validationQueryDelayMs = new AtomicLong(0);
    private final ArrayDeque<Buffer> databaseBuffers = new ArrayDeque<>();
    private NetServer proxyServer;
    private NetClient backendClient;
    private Vertx vertxInstance;
    private boolean delayFirst = false;

    public SQLProxy(String backendHost, int backendPort, int proxyPort) {
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.proxyPort = proxyPort;
    }

    @Override
    public void start() {
        proxyServer = vertx.createNetServer();
        backendClient = vertx.createNetClient();

        AtomicReference<NetSocket> backendSocketRef = new AtomicReference<>();
        AtomicReference<NetSocket> clientSocketRef = new AtomicReference<>();
        connectToDatabase(clientSocketRef, backendSocketRef);
        proxyServer.connectHandler(clientSocket -> {
            clientSocketRef.set(clientSocket);
            handleClientConnection(clientSocket, backendSocketRef);
        });

        proxyServer.listen(proxyPort, "localhost")
                .onSuccess(server -> System.out.println("Database proxy listening on port " + proxyPort))
                .onFailure(throwable -> System.err.println("Failed to start proxy: " + throwable.getMessage()));
    }

    private void handleClientConnection(NetSocket clientSocket, AtomicReference<NetSocket> backendSocketRef) {
        System.out.println("Client connected to database proxy");
        setupProxying(clientSocket, backendSocketRef);
    }

    private void connectToDatabase(AtomicReference<NetSocket> clientSocketRef, AtomicReference<NetSocket> backendSocketRef) {
        backendClient.connect(backendPort, backendHost)
                .onSuccess(backendSocket -> {
                    backendSocketRef.set(backendSocket);

                    backendSocket.exceptionHandler(err -> {
                        System.err.println("Backend socket error: " + err.getMessage());
                        var clientSocket = clientSocketRef.get();
                        if (clientSocket != null) {
                            clientSocket.close();
                        }
                    });

                    backendSocket.closeHandler(v -> {
                        System.out.println("Database disconnected");
                        var clientSocket = clientSocketRef.get();
                        if (clientSocket != null) {
                            clientSocket.close();
                        }
                    });

                    // Forward data from backend to client (responses)
                    backendSocket.handler(buffer -> {
                        System.out.println("Backend -> Client: " + buffer.length() + " bytes");
                        var clientSocket = clientSocketRef.get();
                        if (clientSocket != null) {
                            clientSocket.write(buffer);
                        } else if (!buffer.toString().contains("timeout")) {
                            databaseBuffers.offer(buffer);
                            Log.info("Adding database message to a queue: " + buffer);
                        }
                    });
                })
                .onFailure(err -> {
                    System.err.println("Failed to connect to backend database " + backendHost + ":" + backendPort + ": "
                            + err.getMessage());
                    var clientSocket = clientSocketRef.get();
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                });
    }

    private void setupProxying(NetSocket clientSocket, AtomicReference<NetSocket> backendSocketRef) {
        System.out.println("Connected to database, setting up transparent tunnel");

        // Forward data from client to backend
        clientSocket.handler(buffer -> {
            String utf8 = buffer.toString(StandardCharsets.UTF_8);
            System.out.println("UTF 8" + utf8);
            System.out.println("default charset" + buffer.toString(Charset.defaultCharset()));
            System.out.println("ISO_8859_1 charset" + buffer.toString(StandardCharsets.ISO_8859_1));
            System.out.println("ASCII charset" + buffer.toString(StandardCharsets.US_ASCII));
            System.out.println("Client -> Backend: " + buffer.length() + " bytes and content: " + buffer);

            if (isValidationQuery(buffer) || delayFirst) {
                delayFirst = false;
                System.out.println("*** INTERCEPTING VALIDATION QUERY ***");
                handleValidationQuery(buffer, backendSocketRef.get());
            } else {
                // Forward all traffic transparently (especially during handshake)
                backendSocketRef.get().write(buffer);
            }
        });

        // Handle socket closures
        clientSocket.closeHandler(v -> {
            System.out.println("Client disconnected from proxy");
            backendSocketRef.get().close();
        });

        // Handle errors
        clientSocket.exceptionHandler(err -> {
            System.err.println("Client socket error: " + err.getMessage());
            backendSocketRef.get().close();
        });

        for (Buffer databaseBuffer : databaseBuffers) {
            System.out.println("Removing database message from a queue: " + databaseBuffer);
            clientSocket.write(databaseBuffer);
        }
    }

    private static class ConnectionState {
        boolean isEstablished = false;
        int messageCount = 0;
    }

    private boolean isValidationQuery(Buffer buffer) {
        return buffer.toString().contains("SELECT 1");
    }

    private void handleValidationQuery(Buffer buffer, NetSocket backendSocket) {
        long delay = validationQueryDelayMs.get();
        if (delay > 0) {
            System.out.println("DELAYING validation query by " + delay + "ms: " + buffer);
            vertx.setTimer(delay, id -> {
                System.out.println("Forwarding delayed validation query");
                backendSocket.write(buffer);
            });
        } else {
            System.out.println("Forwarding validation query normally");
            backendSocket.write(buffer);
        }
    }

    @Override
    public void stop() {
        if (proxyServer != null) {
            proxyServer.close();
        }
        if (backendClient != null) {
            backendClient.close();
        }
        System.out.println("Database proxy stopped");
    }

    // Control methods for testing
    public void setValidationQueryDelay(long delayMs) {
        this.validationQueryDelayMs.set(delayMs);
        System.out.println("Validation query delay set to: " + delayMs + "ms");
    }

    // Static helper methods for easy usage in tests
    public static SQLProxy start(String backendHost, int backendPort, int proxyPort) {
        Vertx vertx = Vertx.vertx();
        SQLProxy proxy = new SQLProxy(backendHost, backendPort, proxyPort);
        proxy.vertxInstance = vertx;

        vertx.deployVerticle(proxy).onComplete(result -> {
            if (result.succeeded()) {
                System.out.println("Database proxy deployed successfully");
            } else {
                System.err.println("Failed to deploy proxy: " + result.cause().getMessage());
            }
        });

        // Give it a moment to start up
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return proxy;
    }

    public void shutdown() {
        if (vertxInstance != null) {
            vertxInstance.close();
        }
    }

    public void delayFirst(boolean delayFirst) {
        this.delayFirst = delayFirst;
    }
}
