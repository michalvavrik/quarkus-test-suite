package io.quarkus.ts.opentelemetry;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.GrafanaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.GrafanaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@Tag("QUARKUS-5623")
@QuarkusScenario
public class OpenTelemetryLoggingIT {
    private static final String SERVICE_NAME = "logging-app";

    private static final String CUSTOM_ATTRIBUTE_VALUE = "Lorem-ipsum";
    private static final Integer BULK_SIZE = 10;

    @GrafanaContainer()
    static final GrafanaService grafana = new GrafanaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.otel.logs.enabled", "true")
            .withProperty("quarkus.log.level", "DEBUG")
            .withProperty("quarkus.application.name", SERVICE_NAME)
            .withProperty("quarkus.otel.resource.attributes", "custom_attribute=" + CUSTOM_ATTRIBUTE_VALUE)
            .withProperty("quarkus.otel.blrp.schedule.delay", "PT5S")
            .withProperty("quarkus.otel.blrp.max.export.batch.size", BULK_SIZE.toString())
            .withProperty("quarkus.otel.exporter.otlp.logs.endpoint", grafana::getOtlpCollectorUrl);

    @Test
    public void shouldExportLogs() {
        // accessing this endpoint triggers logging these lines
        app.given().get("/logging")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("This is logging resource"));

        // expect log entries that are hardcoded on app's side
        List<ExpectedEntry> expectedLogEntries = new ArrayList<>();
        expectedLogEntries.add(new ExpectedEntry("INFO", "This is info message"));
        expectedLogEntries.add(new ExpectedEntry("DEBUG", "This is a debug message"));
        expectedLogEntries.add(new ExpectedEntry("WARN", "This is a warning"));
        expectedLogEntries.add(new ExpectedEntry("ERROR", "This is an error"));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response logsResponse = retrieveAllLogs(SERVICE_NAME);
            assertEquals("success", logsResponse.jsonPath().getString("status"),
                    "Should succeed when getting the logs from server");
            List<LogEntry> parsedLogs = parseJsonLogs(logsResponse.jsonPath().getJsonObject("data.result"));

            // check that all expected log lines are present
            for (ExpectedEntry expectedEntry : expectedLogEntries) {
                assertEquals(1,
                        parsedLogs.stream()
                                .filter(p -> p.text.equals(expectedEntry.text) && p.severity.equals(expectedEntry.severity))
                                .count(),
                        "Logs on server should contains line: " + expectedEntry);
            }

            // check that custom attribute (set by properties) is present in the report
            assertEquals(CUSTOM_ATTRIBUTE_VALUE, parsedLogs.get(0).customAttribute, "Custom attribute should be in the report");

            // validate correct hostname in the report - should be the name of the machine app is running on (not the one it was compiled on)
            validateHostname(parsedLogs.get(0));

            // App produces trace message(s), but since there are not reported (too detailed), they should not be on the server
            assertEquals(0, parsedLogs.stream().filter(entry -> entry.severity.equals("TRACE")).count(),
                    "There should be no debug log messages on the server");
        });
    }

    @Test
    public void shouldSendReportInBulks() {
        // this test uses only warning level messages, to filter debug messages, generated by the framework, which may skew asserts
        // period how often so send log lines is configured by "quarkus.otel.blrp.schedule.delay" property

        // count log lines reported before test starts
        int initLogLines = getWarningLogCount(SERVICE_NAME);

        // generate just a few lines, less that size of bulk
        app.given().get("/logging/generate/two")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("Lines generated"));

        // add extra 30 seconds waiting period on Windows
        boolean isWinOs = OS.WINDOWS.isCurrentOs();

        await().atMost(isWinOs ? 37 : 7, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(initLogLines + 2, getWarningLogCount(SERVICE_NAME),
                    "Lines should arrive within sending interval");
        });

        // generate more lines that fit more than one bulk
        app.given().get("/logging/generate/twenty")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("Lines generated"));

        // messages are more than one bulk, so at least one bulk should be sent soon
        await().atMost(isWinOs ? 32 : 2, TimeUnit.SECONDS).untilAsserted(() -> {
            int expectedNumOfLogLines = initLogLines + 2 + BULK_SIZE;
            int actualNumOfLogLines = getWarningLogCount(SERVICE_NAME);
            assertTrue(actualNumOfLogLines >= expectedNumOfLogLines,
                    "Bulk of log lines should arrive sooner, actual number of lines "
                            + actualNumOfLogLines + " should be greater or equal then " + expectedNumOfLogLines);
        });

        // all messages should arrive in at most one sending period
        await().atMost(isWinOs ? 37 : 7, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(initLogLines + 22, getWarningLogCount(SERVICE_NAME),
                    "All logged lines should arrive withing sending interval");
        });
    }

    protected void validateHostname(LogEntry logEntry) throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        assertEquals(hostname, logEntry.hostname, "Hostname in the logs should be local machine hostname");
    }

    private int getWarningLogCount(String serviceName) {
        Response logsResponse = retrieveWarnings(serviceName);
        assertEquals("success", logsResponse.jsonPath().getString("status"),
                "Should succeed when getting the logs from server");
        List<LogEntry> parsedLogs = parseJsonLogs(logsResponse.jsonPath().getJsonObject("data.result"));
        return parsedLogs.size();
    }

    private Response retrieveAllLogs(String serviceName) {
        return given().when()
                .queryParam("query", "{service_name=\"" + serviceName + "\"}")
                .queryParam("limit", 500)
                .get(grafana.getRestUrl() + "/loki/api/v1/query_range");
    }

    private Response retrieveWarnings(String serviceName) {
        return given().when()
                .queryParam("query", "{service_name=\"" + serviceName + "\"} |= \"warning\"")
                .queryParam("limit", 500)
                .get(grafana.getRestUrl() + "/loki/api/v1/query_range");
    }

    private List<LogEntry> parseJsonLogs(List<Map> restAssuredResult) {
        List<LogEntry> result = new ArrayList<>();

        for (Map map : restAssuredResult) {
            result.add(new LogEntry(
                    ((Map) map.get("stream")).get("severity_text").toString(),
                    // element number 0 is timestamp, to get text we get element number 1 from values
                    ((List<List<String>>) map.get("values")).get(0).get(1),
                    ((Map<?, ?>) map.get("stream")).get("host_name").toString(),
                    ((Map<?, String>) map.get("stream")).getOrDefault("custom_attribute", "").toString()));
        }

        return result;
    }

    private record ExpectedEntry(String severity, String text) {

    }

    protected record LogEntry(String severity, String text, String hostname, String customAttribute) {
        @Override
        public String hostname() {
            return hostname;
        }
    }
}
