package io.quarkus.ts.transactions;

import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;
import static io.quarkus.ts.transactions.TransactionLogService.ACCOUNT_NUMBER_EDUARDO;
import static io.quarkus.ts.transactions.TransactionLogService.ACCOUNT_NUMBER_FRANCISCO;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.bootstrap.AmqService;
import io.quarkus.test.bootstrap.JaegerService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.AmqContainer;
import io.quarkus.test.services.JaegerContainer;
import io.quarkus.test.services.containers.model.AmqProtocol;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // we keep order to ensure JDBC traces are ready
public abstract class TransactionCommons {

    static final String ACCOUNT_NUMBER_MIGUEL = "SK0389852379529966291984";
    static final String ACCOUNT_NUMBER_GARCILASO = "FR9317569000409377431694J37";
    static final String ACCOUNT_NUMBER_LUIS = "ES8521006742088984966816";
    static final String ACCOUNT_NUMBER_LOPE = "CZ9250512252717368964232";
    static final int ASSERT_SERVICE_TIMEOUT_MINUTES = 1;

    @JaegerContainer(expectedLog = "\"Health Check state change\",\"status\":\"ready\"")
    static final JaegerService jaeger = new JaegerService();

    @AmqContainer(protocol = AmqProtocol.TCP)
    static AmqService amq = new AmqService();

    protected abstract RestService getApp();

    @Order(1)
    @Tag("QUARKUS-2492")
    @Test
    public void verifyNarayanaProgrammaticApproachTransaction() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_MIGUEL);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_LOPE);
        transferDTO.setAmount(100);

        getApp().given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/transaction")
                .then().statusCode(HttpStatus.SC_CREATED);

        AccountEntity miguelAccount = getAccount(ACCOUNT_NUMBER_MIGUEL);
        Assertions.assertEquals(0, miguelAccount.getAmount(), "Unexpected amount on source account.");

        AccountEntity lopeAccount = getAccount(ACCOUNT_NUMBER_LOPE);
        Assertions.assertEquals(200, lopeAccount.getAmount(), "Unexpected amount on source account.");

        JournalEntity miguelJournal = getLatestJournalRecord(ACCOUNT_NUMBER_MIGUEL);
        Assertions.assertEquals(100, miguelJournal.getAmount(), "Unexpected journal amount.");
    }

    @Order(2)
    @Tag("QUARKUS-2492")
    @Test
    public void verifyLegacyNarayanaLambdaApproachTransaction() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_GARCILASO);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_GARCILASO);
        transferDTO.setAmount(100);

        getApp().given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/legacy/top-up")
                .then().statusCode(HttpStatus.SC_CREATED);

        AccountEntity garcilasoAccount = getAccount(ACCOUNT_NUMBER_GARCILASO);
        Assertions.assertEquals(200, garcilasoAccount.getAmount(),
                "Unexpected account amount. Expected 200 found " + garcilasoAccount.getAmount());

        JournalEntity garcilasoJournal = getLatestJournalRecord(ACCOUNT_NUMBER_GARCILASO);
        Assertions.assertEquals(100, garcilasoJournal.getAmount(), "Unexpected journal amount.");
    }

    @Order(3)
    @Tag("QUARKUS-2492")
    @Test
    public void verifyNarayanaLambdaApproachTransaction() {
        makeTopUpTransfer(getApp());

        AccountEntity garcilasoAccount = getAccount(ACCOUNT_NUMBER_EDUARDO);
        Assertions.assertEquals(200, garcilasoAccount.getAmount(),
                "Unexpected account amount. Expected 200 found " + garcilasoAccount.getAmount());

        JournalEntity garcilasoJournal = getLatestJournalRecord(ACCOUNT_NUMBER_EDUARDO);
        Assertions.assertEquals(100, garcilasoJournal.getAmount(), "Unexpected journal amount.");
    }

    static void makeTopUpTransfer(RestService app) {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_EDUARDO);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_EDUARDO);
        transferDTO.setAmount(100);

        app
                .given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/top-up")
                .then().statusCode(HttpStatus.SC_CREATED);
    }

    @Order(4)
    @Tag("QUARKUS-2492")
    @Test
    public void verifyRollbackForNarayanaProgrammaticApproach() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_LUIS);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_LUIS);
        transferDTO.setAmount(200);

        getApp().given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/withdrawal")
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);

        AccountEntity luisAccount = getAccount(ACCOUNT_NUMBER_LUIS);
        Assertions.assertEquals(100, luisAccount.getAmount(), "Unexpected account amount.");

        getApp().given()
                .get("/transfer/journal/latest/" + ACCOUNT_NUMBER_LUIS)
                .then()
                .statusCode(SC_OK);
    }

    // TODO figure why it not work, probably changes made for logs
    //    @Order(5)
    //    @Tag("QUARKUS-2492")
    //    @Test
    private void smokeTestNarayanaProgrammaticTransactionTrace() {
        String operationName = "GET /transfer/accounts/{account_id}";
        getApp().given()
                .get("/transfer/accounts/" + ACCOUNT_NUMBER_LUIS).then().statusCode(SC_OK);
        verifyRequestTraces(operationName);
    }

    // TODO figure why it not work, probably changes made for logs
    //    @Order(6)
    //    @Test
    private void verifyJdbcTraces() {
        for (String operationName : getExpectedJdbcOperationNames()) {
            verifyRequestTraces(operationName);
        }
    }

    protected String[] getExpectedJdbcOperationNames() {
        return new String[] { "SELECT mydb.account", "INSERT mydb.journal", "UPDATE mydb.account" };
    }

    //    @Order(5)
    //    @Tag("QUARKUS-2492")
    //    @Test
    private void smokeTestMetricsNarayanaProgrammaticTransaction() {
        String metricName = "transaction_withdrawal_amount";
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_FRANCISCO);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_FRANCISCO);
        transferDTO.setAmount(20);

        getApp().given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/withdrawal")
                .then().statusCode(HttpStatus.SC_CREATED);

        verifyMetrics(metricName, greater(0));

        // check rollback gauge
        transferDTO.setAmount(3000);
        double beforeRollback = getMetricsValue(metricName);
        getApp().given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/withdrawal")
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);
        double afterRollback = getMetricsValue(metricName);
        Assertions.assertEquals(beforeRollback, afterRollback, "Gauge should not be increased on a rollback transaction");
    }

    @Order(5)
    @Test
    public void todo() {
        //        getTransactionLog("not-crashed")
        //                .statusCode(200)
        //                .body(is("true"));
        //        getTransactionLogCrashed("crashed", true);
        //        getTransactionLog("not-crashed")
        //                .statusCode(200)
        //                .body(is("true"));
        //        getTransactionLogCrashed("crashed", true);
        //        getTransactionLog("not-crashed")
        //                .statusCode(200)
        //                .body(is("true"));
        getTransactionLogCrashed("crash-with-transaction", false);
    }

    private void getTransactionLogCrashed(String subPath, boolean restart) {
        try {
            getTransactionLog(subPath).statusCode(SC_OK);
        } catch (Throwable t) {
            if (restart) {
                getApp().restart();
                untilIsTrue(getApp()::isRunning);
            }
            return;
        }
        Assertions.fail("Illegal state - Application was supposed to crash");
    }

    private ValidatableResponse getTransactionLog(String subPath) {
        return getApp()
                .given()
                // FIXME: remove logging!
                .log().all().filter(new ResponseLoggingFilter())
                .get("/transaction-log/" + subPath)
                .then();
    }

    private AccountEntity getAccount(String accountNumber) {
        return getApp()
                .given()
                .get("/transfer/accounts/" + accountNumber)
                .then()
                .statusCode(SC_OK)
                .extract()
                .body().as(AccountEntity.class);
    }

    private void verifyRequestTraces(String operationName) {
        verifyRequestTraces(operationName, jaeger);
    }

    static void verifyRequestTraces(String operationName, JaegerService jaeger) {
        await().atMost(1, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            var operations = getTracedOperationsForName(operationName, jaeger);
            Assertions.assertNotNull(operations);
            Assertions.assertTrue(operations.stream().anyMatch(operationName::equals));
        });
    }

    static List<String> getTracedOperationsForName(String operationName, JaegerService jaeger) {
        var jaegerResponse = retrieveTraces(20, "1h", "narayanaTransactions",
                operationName, jaeger);
        return jaegerResponse.jsonPath().getList("data[0].spans.operationName", String.class);
    }

    private JournalEntity getLatestJournalRecord(String accountNumber) {
        return getApp()
                .given()
                .get("/transfer/journal/latest/" + accountNumber)
                .then()
                .statusCode(SC_OK)
                .extract()
                .body().as(JournalEntity.class);
    }

    static Response retrieveTraces(int pageLimit, String lookBack, String serviceName, String operationName,
            JaegerService jaeger) {
        return given()
                .when()
                .log().uri()
                .queryParam("operation", operationName)
                .queryParam("lookback", lookBack)
                .queryParam("limit", pageLimit)
                .queryParam("service", serviceName)
                .get(jaeger.getTraceUrl());
    }

    private void verifyMetrics(String name, Predicate<Double> valueMatcher) {
        await().ignoreExceptions().atMost(ASSERT_SERVICE_TIMEOUT_MINUTES, TimeUnit.MINUTES).untilAsserted(() -> {
            String response = getApp()
                    .given()
                    .get("/q/metrics")
                    .then()
                    .statusCode(SC_OK)
                    .extract().asString();

            boolean matches = false;
            for (String line : response.split("[\r\n]+")) {
                if (line.startsWith(name)) {
                    Double value = extractValueFromMetric(line);
                    Assertions.assertTrue(valueMatcher.test(value), "Metric " + name + " has unexpected value " + value);
                    matches = true;
                    break;
                }
            }

            Assertions.assertTrue(matches, "Metric " + name + " not found in " + response);
        });
    }

    private Double getMetricsValue(String name) {
        String response = getApp()
                .given()
                .get("/q/metrics")
                .then()
                .statusCode(SC_OK)
                .extract()
                .asString();
        for (String line : response.split("[\r\n]+")) {
            if (line.startsWith(name)) {
                return extractValueFromMetric(line);
            }
        }

        Assertions.fail("Metrics property " + name + " not found.");
        return 0d;
    }

    private Double extractValueFromMetric(String line) {
        return Double.parseDouble(line.substring(line.lastIndexOf(" ")));
    }

    private Predicate<Double> greater(double expected) {
        return actual -> actual > expected;
    }

}
