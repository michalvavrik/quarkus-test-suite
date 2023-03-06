package io.quarkus.ts.http.advanced.reactive;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.grpc.reflection.v1.MutinyServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.quarkus.example.Greeter;
import io.quarkus.example.HelloReply;
import io.quarkus.example.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/grpc")
public class GrpcResource {

    @Inject
    @GrpcClient("hello")
    Greeter client;

    @Inject
    @GrpcClient("reflection-service")
    MutinyServerReflectionGrpc.MutinyServerReflectionStub reflection;

    @GET
    @Path("reflection/service/count")
    @Produces(MediaType.TEXT_PLAIN)
    public int serviceCount() {
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder().setHost("localhost")
                .setListServices("").build();
        GrpcReflectionResponse response = new GrpcReflectionResponse(invoke(request));
        return response.getServiceCount();
    }

    @GET
    @Path("reflection/service/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> serviceList() {
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder().setHost("localhost")
                .setListServices("").build();
        GrpcReflectionResponse response = new GrpcReflectionResponse(invoke(request));
        return response.getServiceList();
    }

    @GET
    @Path("reflection/something")
    @Produces(MediaType.APPLICATION_JSON)
    public byte[] something() {
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder()
                .setHost("localhost").setFileByFilename("helloworld.proto").build();

        var desc = invoke(request);
        GrpcReflectionResponse response = new GrpcReflectionResponse(invoke(request));
        return desc.getFileDescriptorResponse().toByteArray();
    }

    @GET
    @Path("reflection/service/description")
    @Produces(MediaType.TEXT_PLAIN)
    public String serviceMethods() {
        ServerReflectionRequest request = ServerReflectionRequest.newBuilder()
                .setHost("localhost").setFileByFilename("helloworld.proto").build();

        GrpcReflectionResponse response = new GrpcReflectionResponse(invoke(request));
        return response.getFileDescriptor();
    }

    private ServerReflectionResponse invoke(ServerReflectionRequest request) {
        return reflection.serverReflectionInfo(Multi.createFrom().item(request))
                .collect().first()
                .await().indefinitely();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> hello(@PathParam("name") String name) {
        return client.sayHello(HelloRequest.newBuilder().setName(name).build()).onItem().transform(HelloReply::getMessage);
    }

}
