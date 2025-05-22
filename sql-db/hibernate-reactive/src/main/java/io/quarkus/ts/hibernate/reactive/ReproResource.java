package io.quarkus.ts.hibernate.reactive;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.mutiny.Uni;
import io.vertx.sqlclient.Pool;

@Path("repro")
public class ReproResource {

    @Inject
    Pool pool;

    @GET
    public Uni<String> queryUsingMysqlClient() {
        return Uni.createFrom().completionStage(
                pool
                        .query("SELECT * FROM unknown")
                        .execute()
                        .toCompletionStage())
                .map(Object::toString)
                .invoke(s -> System.out.println("////// row set is " + s))
                .onFailure().invoke(t -> System.out.println("///////// failure is " + t));
    }

}
