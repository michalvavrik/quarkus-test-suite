package io.quarkus.ts.http.grpc.customizers;

import jakarta.enterprise.context.ApplicationScoped;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.api.ServerBuilderCustomizer;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.ts.grpc.metadata.MetadataReply;
import io.quarkus.ts.grpc.metadata.MetadataRequest;
import io.quarkus.ts.grpc.metadata.MutinyMetadataGrpc;
import io.smallrye.mutiny.Uni;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.server.GrpcServerOptions;

@ApplicationScoped
public class GrpcServerCustomizer implements ServerBuilderCustomizer<VertxServerBuilder> {

    private final GrpcServerCustomizerHelper helper;

    GrpcServerCustomizer(GrpcServerCustomizerHelper helper) {
        this.helper = helper;
    }

    @Override
    public void customize(GrpcServerConfiguration config, VertxServerBuilder builder) {
        builder.addService(new MetadataGrpcService());
        builder.intercept(new MetadataPropagatingInterceptor());
    }

    @Override
    public void customize(GrpcServerConfiguration config, GrpcServerOptions options) {
        // FIXME: impl. me!
    }

    private final class MetadataPropagatingInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
                ServerCallHandler<ReqT, RespT> serverCallHandler) {
            helper.putMetadataInvocationsToContext(metadata);
            return serverCallHandler.startCall(serverCall, metadata);
        }
    }

    private final class MetadataGrpcService extends MutinyMetadataGrpc.MetadataImplBase {

        @Override
        public Uni<MetadataReply> getMetadata(MetadataRequest request) {
            InterceptorInvocations invocations = helper.getInvocationsFromContext();
            MetadataReply response = MetadataReply.newBuilder()
                    .setName(request.getName())
                    .setInterceptedFirst(invocations.interceptedFirst())
                    .setInterceptedSecond(invocations.interceptedSecond())
                    .setInterceptedThird(invocations.interceptedThird())
                    .build();
            return Uni.createFrom().item(response);
        }
    }
}
