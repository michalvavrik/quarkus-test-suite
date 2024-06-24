package io.quarkus.ts.http.restclient.reactive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.response.Response;

@QuarkusScenario
public class HttpSimpleEncodeModeIT {

    private String filename;
    static final String EXPACTED_CONTENT_DISPOSITION_SAMPLE = "\"Content-Disposition\":[\"form-data; name=\\\"file\\\"; filename=\\\"";

    @QuarkusApplication(properties = "test.properties")
    static RestService app = new RestService()
            .withProperty("quarkus.rest-client.multipart-post-encoder-mode", "HTML5");

    @ParameterizedTest
    @ValueSource(strings = { "HTML5", "RFC1738", "RFC3986" })
    public void testMultipartEncodeMode(String encoderMode) {
        System.out.println("Encode mode " + encoderMode);
        File file = Paths.get("src", "test", "resources", "sample.txt").toFile();
        String otherField = "other field";
        filename = file.getName();

        Response response = app.given()
                .multiPart("file", file, "text/plain")
                .multiPart("otherField", otherField)
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .when()
                .post("/simple/encode")
                .then()
                .statusCode(200)
                .extract().response();

        String capturedRequestBody = response.asString();

        String expectedContentDisposition = "";

        if (encoderMode.equals("HTML5")) {
            expectedContentDisposition = EXPACTED_CONTENT_DISPOSITION_SAMPLE + filename + "\\\"\"]";
        }
        if (encoderMode.equals("RFC1738")) {
            expectedContentDisposition = EXPACTED_CONTENT_DISPOSITION_SAMPLE + filename + "\\\"\"]";
        }
        if (encoderMode.equals("RFC3986")) {
            expectedContentDisposition = EXPACTED_CONTENT_DISPOSITION_SAMPLE + filename + "\\\"\"]";
        }

        assertTrue(capturedRequestBody.contains(expectedContentDisposition),
                "Content-Disposition header match expected format for encoder mode: " + encoderMode);
    }

}
