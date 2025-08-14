package io.quarkus.ts.http.grpc.customizers;

import jakarta.enterprise.context.Dependent;

import io.quarkus.grpc.api.ServerBuilderCustomizer;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.server.GrpcServerOptions;

@Dependent
public class LegacyGrpcServerCustomizer2 implements ServerBuilderCustomizer<VertxServerBuilder> {

    @Override
    public void customize(GrpcServerConfiguration config, GrpcServerOptions options) {
        // FIXME: impl. me!
        ServerBuilderCustomizer.super.customize(config, options);
    }
}
