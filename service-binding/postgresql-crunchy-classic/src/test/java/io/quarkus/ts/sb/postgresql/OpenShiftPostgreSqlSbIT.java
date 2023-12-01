package io.quarkus.ts.sb.postgresql;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static io.quarkus.test.utils.AwaitilityUtils.AwaitilitySettings.using;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.utils.Command;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy)
public class OpenShiftPostgreSqlSbIT {

    @Inject
    static OpenShiftClient ocClient;

    @QuarkusApplication
    static RestService app = new RestService()
            .onPreStart(s -> createPostgresCluster());

    @AfterAll
    public static void tearDown() {
        deleteCustomResourceDefinition("pg-cluster.yml");
    }

    /**
     * This test verifies the application deploys successfully. If the binding fails, the application will not be
     * available.
     */
    @Test
    public void verifyPostgresServiceBoundToApplication() {
        app.given()
                .get("/todo/1/")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    private static void assertRequiredOperatorsInstalled() {
        untilAsserted(() -> {
            List<String> output = new ArrayList<>();
            try {
                // TODO: figure out a better way to wait for this - this wait is necessary as it takes some
                //  time for API to populate new namespace with objects
                new Command("oc", "get", "csv").outputToLines(output).runAndWait();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            assertTrue(output.stream().anyMatch(str -> str.contains("postgresoperator")));
            assertTrue(output.stream().anyMatch(str -> str.contains("service-binding-operator")));
        }, using(ofSeconds(2), ofSeconds(60)));
    }

    private static void createPostgresCluster() {
        assertRequiredOperatorsInstalled();
        applyCustomResourceDefinition("pg-cluster.yml");
        try {
            // TODO: figure out a better way to wait for this - sometimes operator takes a while to create object
            //       maybe condition of the generated postgres object would help, but this is not a documented API
            Thread.sleep(2000);
            new Command("oc", "-n", ocClient.project(), "wait", "--for", "condition=Ready", "--timeout=300s",
                    "pods", "--all").runAndWait();
        } catch (Exception e) {
            deleteCustomResourceDefinition("pg-cluster.yml");
            Assertions.fail("PostgresCluster did not form correctly. Caused by: " + e.getMessage());
        }
    }

    private static void applyCustomResourceDefinition(String yamlFile) {
        try {
            new Command("oc", "apply", "-f", Paths.get("target/test-classes/" + yamlFile).toString()).runAndWait();
        } catch (Exception e) {
            Assertions.fail("Failed to apply YAML file. Caused by: " + e.getMessage());
        }
    }

    private static void deleteCustomResourceDefinition(String yamlFile) {
        try {
            new Command("oc", "delete", "-f", Paths.get("target/test-classes/" + yamlFile).toString()).runAndWait();
        } catch (Exception e) {
            Assertions.fail("Failed to delete YAML file. Caused by: " + e.getMessage());
        }
    }

}
