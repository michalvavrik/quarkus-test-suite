package io.quarkus.ts.http.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import io.quarkus.ts.grpc.GreeterGrpc;
import io.quarkus.ts.grpc.HelloReply;
import io.quarkus.ts.grpc.HelloRequest;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.CloseableManagedChannel;
import io.quarkus.ts.grpc.metadata.MetadataGrpc;
import io.quarkus.ts.grpc.metadata.MetadataReply;
import io.quarkus.ts.grpc.metadata.MetadataRequest;
import io.quarkus.ts.http.grpc.customizers.LegacyGrpcServerCustomizer;
import io.quarkus.ts.http.grpc.customizers.LegacyGrpcServerCustomizer2;
import io.quarkus.ts.http.grpc.customizers.LegacyGrpcServerCustomizer3;

public interface GrpcSameServerCustomizationIT {

    CloseableManagedChannel getChannel();

    @Test
    default void testCustomizations() throws ExecutionException, InterruptedException {
        try (var channel = getChannel()) {
            String name = "Black Lung";
            HelloRequest request = HelloRequest.newBuilder().setName(name).build();
            HelloReply response = GreeterGrpc.newFutureStub(channel).sayHello(request).get();
            assertEquals(name, response.getMessage());
            name = "Black Lung".repeat(10);
            request = HelloRequest.newBuilder().setName(name).build();
            response = GreeterGrpc.newFutureStub(channel).sayHello(request).get();
            assertEquals(name, response.getMessage());
        }
    }

}
