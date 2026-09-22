package br.com.samuellima.qa.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.samuellima.qa.model.BreedImagesResponse;
import br.com.samuellima.qa.model.BreedListResponse;
import java.util.List;


public final class BreedValidator {

    private BreedValidator() {
    }

    public static void validateBreedList(BreedListResponse body) {
        assertNotNull(body, "Corpo da lista de raças não pode ser nulo");
        assertTrue(body.isSuccess(), "status deveria ser 'success'");
        assertNotNull(body.message(), "'message' não pode ser nulo");
        assertFalse(body.message().isEmpty(), "A lista de raças não pode ser vazia");

        body.message().forEach((breed, subs) -> {
            assertNotNull(breed, "Nome de raça não pode ser nulo");
            assertFalse(breed.isBlank(), "Nome de raça não pode ser vazio");
            assertNotNull(subs, "Lista de sub-raças de '" + breed + "' não pode ser nula");
            subs.forEach(sub -> {
                assertNotNull(sub, "Sub-raça de '" + breed + "' não pode ser nula");
                assertFalse(sub.isBlank(), "Sub-raça de '" + breed + "' não pode ser vazia");
            });
        });
    }

    public static void assertContainsBreed(BreedListResponse body, String breed) {
        assertTrue(body.breeds().contains(breed), "Raça esperada ausente: " + breed);
    }

    public static void assertHasSubBreeds(BreedListResponse body, String breed, String... subs) {
        List<String> actual = body.subBreeds(breed);
        for (String sub : subs) {
            assertTrue(actual.contains(sub),
                    "Sub-raça '" + sub + "' ausente em '" + breed + "'. Veio: " + actual);
        }
    }

    public static void assertNoSubBreeds(BreedListResponse body, String breed) {
        assertTrue(body.subBreeds(breed).isEmpty(),
                "Esperava nenhuma sub-raça em '" + breed + "', veio: " + body.subBreeds(breed));
    }

    public static void validateBreedImages(BreedImagesResponse body, String breed) {
        assertNotNull(body, "Corpo de imagens não pode ser nulo");
        assertTrue(body.isSuccess(), "status deveria ser 'success'");
        assertNotNull(body.message(), "'message' não pode ser nulo");
        assertFalse(body.images().isEmpty(),
                "A raça '" + breed + "' deveria retornar ao menos uma imagem");
        body.images().forEach(ImageValidator::validateImageUrl);
    }


    public static void assertMultipleSubBreedImages(BreedImagesResponse body, String breed,
                                                    int minDistinctPrefixes) {
        long distinct = body.images().stream()
                .map(url -> subBreedPrefix(url, breed))
                .filter(prefix -> prefix != null)
                .distinct()
                .count();
        assertTrue(distinct >= minDistinctPrefixes,
                "Esperado >= " + minDistinctPrefixes + " sub-raças distintas em '" + breed
                        + "', encontrado: " + distinct);
    }

    private static String subBreedPrefix(String url, String breed) {
        String marker = "breeds/" + breed;
        int idx = url.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        String tail = url.substring(idx + marker.length());
        if (tail.startsWith("-")) {
            int slash = tail.indexOf('/');
            return slash > 1 ? tail.substring(1, slash) : null;
        }
        return null;
    }
}