package io.quarkus.ts.http.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

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
            MetadataRequest request = MetadataRequest.newBuilder().setMessage("Hey").build();
            MetadataReply response = MetadataGrpc.newFutureStub(channel).getMetadata(request).get();
            assertEquals(request.getMessage(), response.getRequestMessage());
            // if the first customizer adds interceptor A and the second adds interceptor B
            // then the B is invoked first and the A is invoked second and so on
            assertEquals(LegacyGrpcServerCustomizer3.class.getName(), response.getInterceptedFirst());
            assertEquals(LegacyGrpcServerCustomizer2.class.getName(), response.getInterceptedSecond());
            assertEquals(LegacyGrpcServerCustomizer.class.getName(), response.getInterceptedThird());
        }
    }

}
