package br.com.samuellima.qa.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.samuellima.qa.model.RandomImageResponse;
import io.restassured.RestAssured;
import java.net.URI;
import java.util.List;
import java.util.Locale;


public final class ImageValidator {

    private static final String EXPECTED_HOST = "images.dog.ceo";
    private static final List<String> IMAGE_EXTENSIONS =
            List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private ImageValidator() {
    }

    public static void validateImageUrl(String url) {
        assertNotNull(url, "URL de imagem não pode ser nula");
        assertFalse(url.isBlank(), "URL de imagem não pode ser vazia");

        URI uri = URI.create(url);
        assertEquals("https", uri.getScheme(), "URL deveria usar https: " + url);
        assertEquals(EXPECTED_HOST, uri.getHost(), "Host inesperado: " + url);

        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        assertTrue(IMAGE_EXTENSIONS.stream().anyMatch(path::endsWith),
                "URL não termina com extensão de imagem: " + url);
    }

    public static void validateAll(List<String> urls) {
        assertNotNull(urls, "Lista de URLs não pode ser nula");
        urls.forEach(ImageValidator::validateImageUrl);
    }

    public static void validateRandom(RandomImageResponse body) {
        assertNotNull(body, "Corpo do random não pode ser nulo");
        assertTrue(body.isSuccess(), "status deveria ser 'success'");
        validateImageUrl(body.imageUrl());
    }

    public static void assertReachable(String url) {
        int status = RestAssured.given()
                .redirects().follow(true)
                .when().head(url)
                .then().extract().statusCode();
        assertEquals(200, status, "Imagem não acessível (HEAD != 200): " + url);
    }
}