package br.com.samuellima.qa.validator;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.response.Response;

public final class ResponseValidator {

    private ResponseValidator() {
    }

    public static void assertHttpStatus(Response response, int expected) {
        assertNotNull(response, "Response não pode ser nula");
        assertEquals(expected, response.statusCode(),
                "HTTP status inesperado. Body: " + response.asString());
    }

    public static void assertJsonContentType(Response response) {
        String contentType = response.getContentType();
        assertNotNull(contentType, "Content-Type ausente");
        assertTrue(contentType.toLowerCase().contains("application/json"),
                "Content-Type deveria ser JSON, veio: " + contentType);
    }

    public static void assertStatusField(Response response, String expected) {
        String status = response.jsonPath().getString("status");
        assertEquals(expected, status,
                "Campo 'status' inesperado. Body: " + response.asString());
    }

    public static void assertSuccess(Response response) {
        assertHttpStatus(response, 200);
        assertJsonContentType(response);
        assertStatusField(response, "success");
    }

    public static void assertError(Response response, int expectedCode) {
        assertHttpStatus(response, expectedCode);
        assertStatusField(response, "error");

        Integer code = response.jsonPath().getObject("code", Integer.class);
        assertNotNull(code, "Resposta de erro deveria conter o campo 'code'");
        assertEquals(expectedCode, code.intValue(), "Campo 'code' divergente do HTTP status");

        String message = response.jsonPath().getString("message");
        assertNotNull(message, "Resposta de erro deveria conter 'message'");
        assertFalse(message.isBlank(), "'message' de erro não pode ser vazia");
    }

    public static void assertErrorMessageContains(Response response, String fragment) {
        String message = response.jsonPath().getString("message");
        assertNotNull(message, "'message' de erro ausente");
        assertTrue(message.toLowerCase().contains(fragment.toLowerCase()),
                "'message' deveria conter \"" + fragment + "\", veio: " + message);
    }

    public static void assertMatchesSchema(Response response, String classpathSchema) {
        response.then().assertThat().body(matchesJsonSchemaInClasspath(classpathSchema));
    }
}