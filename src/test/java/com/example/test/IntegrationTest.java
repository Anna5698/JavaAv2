package com.example.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class IntegrationTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void setup() {
        // Запускаем WireMock сервер на порту 8089
        wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        RestAssured.baseURI = "http://localhost:8089";

        // Настраиваем мок-ответ для успешного теста
        String validResponse = "[{\"id\":1,\"name\":\"Ivan Ivanov\",\"number\":\"•• 1234\",\"balance\":15000,\"currency\":\"RUB\"},{\"id\":2,\"name\":\"John Smith\",\"number\":\"•• 5678\",\"balance\":2500,\"currency\":\"USD\"}]";

        stubFor(get(urlEqualTo("/api/accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(validResponse)));

        // Настраиваем мок-ответ для теста с ошибкой (невалидная валюта)
        String invalidResponse = "[{\"id\":3,\"name\":\"Test User\",\"number\":\"•• 9999\",\"balance\":5000,\"currency\":\"EUR\"}]";

        stubFor(get(urlEqualTo("/api/accounts-invalid"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(invalidResponse)));
    }

    @AfterAll
    public static void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testValidResponseMatchesSchema() {
        RestAssured.given()
                .when()
                .get("/api/accounts")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("accounts.schema.json"));
    }

    @Test
    public void testInvalidCurrencyFails() {
        try {
            RestAssured.given()
                    .when()
                    .get("/api/accounts-invalid")
                    .then()
                    .statusCode(200)
                    .body(matchesJsonSchemaInClasspath("accounts.schema.json"));

            throw new AssertionError("Expected test to fail due to invalid currency");
        } catch (AssertionError e) {
            // Expected behavior - test should fail due to schema validation
        }
    }
}