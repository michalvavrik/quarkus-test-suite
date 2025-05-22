package io.quarkus.ts.hibernate.reactive;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.sqlclient.Pool;

@TestProfile(VertxMysqlClientFipsReproducerTest.MyTestProfile.class)
@QuarkusTest
public class VertxMysqlClientFipsReproducerTest {

    @Inject
    Pool pool;

    @Test
    public void reproducer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        pool
                .query("CREATE TABLE authors (id INT NOT NULL AUTO_INCREMENT, name VARCHAR(31) NOT NULL, PRIMARY KEY(id))")
                .execute()
                .onComplete(result -> {
                    if (result.failed()) {
                        Assertions.fail(result.cause());
                    } else {
                        latch.countDown();
                    }
                });
        latch.await(1, TimeUnit.MINUTES);
    }

    public static class MyTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.datasource.db-kind", "mysql",
                    "quarkus.datasource.username", "quarkus_test",
                    "quarkus.datasource.password", "quarkus_test",
                    "quarkus.datasource.reactive.url", "mysql://localhost:3306/quarkus_test");
        }
    }
}
