package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.bootstrap.KeycloakService;
import io.restassured.response.Response;

final class TokenUtils {

    private static final Logger LOG = Logger.getLogger(TokenUtils.class);
    static final String USER = "test-user";

    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    static final String TOKEN_ENDPOINT = "/protocol/openid-connect/token";
    static final String CLIENT_ID = "test-application-client";
    static final String CLIENT_SECRET = "test-application-client-secret";

    private TokenUtils() {
    }

    static String createToken(KeycloakService keycloak) {
        // we retry and create AuthzClient as we experienced following exception in the past: 'Caused by:
        // org.apache.http.NoHttpResponseException: keycloak-ts-juwpkvyduk.apps.ocp4-15.dynamic.quarkus:80 failed to respond'
        return createToken(1, keycloak);
    }

    private static String createToken(int attemptCount, KeycloakService keycloak) {
        try {
            return keycloak.createAuthzClient(CLIENT_ID_DEFAULT, CLIENT_SECRET_DEFAULT).obtainAccessToken(USER, USER)
                    .getToken();
        } catch (RuntimeException e) {
            LOG.error("Attempt #%d to create token failed with exception:".formatted(attemptCount), e);
            if (e.getCause() instanceof org.apache.http.NoHttpResponseException && attemptCount < 3) {
                LOG.info("Retrying to create token.");
                return createToken(attemptCount + 1, keycloak);
            }
            throw e;
        }
    }

    public static String createTokenWithAcr(KeycloakService keycloak, String username, String acrLevel) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", CLIENT_ID + "-copper");
        params.put("client_secret", CLIENT_SECRET);
        params.put("username", username);
        params.put("password", username);
        params.put("scope", "acr");
        params.put("prompt", "login");
        params.put("claims", getAcrClaim(acrLevel));

        Response response = given()
                .relaxedHTTPSValidation()
                .formParams(params)
                .when()
                .post(keycloak.getRealmUrl() + TOKEN_ENDPOINT);

        return response.jsonPath().getString("access_token");
    }

    public static String createToken(KeycloakService keycloak, String username, String... acrValues) {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("username", username);
        params.put("password", username);
        params.put("scope", "acr");

        if (acrValues.length > 0) {
            var acrRepresentation = getAcrClaim(acrValues);
            params.put("claims", acrRepresentation);
        }

        Response response = given()
                .relaxedHTTPSValidation()
                .formParams(params)
                .when()
                .post(keycloak.getRealmUrl() + TOKEN_ENDPOINT);

        response.then().statusCode(200);
        return response.jsonPath().getString("access_token");
    }

    static String getAcrClaim(String... acrValues) {
        var claimsRepresentation = claims(true, acrValues);
        var acrRepresentation = encodeClaimRepresentation(claimsRepresentation);
        return acrRepresentation;
    }

    private static ClaimsRepresentation claims(boolean essential, String... acrValues) {
        //in order to test both values and value
        //setValue only for essential false and only one value
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        if (essential || acrValues.length > 1) {
            acrClaim.setValues(Arrays.asList(acrValues));
        } else {
            acrClaim.setValue(acrValues[0]);
        }

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));
        return claims;
    }

    private static String encodeClaimRepresentation(ClaimsRepresentation representation) {
        try {
            return URLEncoder.encode(JsonSerialization.writeValueAsString(representation), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
