package br.com.samuellima.qa.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.samuellima.qa.model.BreedImagesResponse;
import br.com.samuellima.qa.support.BaseTest;
import br.com.samuellima.qa.validator.BreedValidator;
import br.com.samuellima.qa.validator.ResponseValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("api")
@Tag("breed-images")
@DisplayName("GET /breed/{breed}/images")
class BreedImagesTests extends BaseTest {

  private static final String SCHEMA = "schemas/breed-images-schema.json";
  private static final String BREED = "hound";
  private static final String BREED_NO_SUB = "affenpinscher";

  @Nested
  @DisplayName("Caminho feliz")
  class HappyPath {

    @Test
    @DisplayName("Raça válida retorna lista não vazia de imagens")
    void validBreedReturnsImages() {
      Response response = breedFlow.breedImages(BREED);
      ResponseValidator.assertSuccess(response);

      BreedImagesResponse body = response.as(BreedImagesResponse.class);
      BreedValidator.validateBreedImages(body, BREED);
      reportInfo("Imagens de " + BREED, String.valueOf(body.count()));
    }

    @Test
    @DisplayName("Corpo adere ao JSON Schema")
    void matchesSchema() {
      Response response = breedFlow.breedImages(BREED);
      ResponseValidator.assertMatchesSchema(response, SCHEMA);
    }

    @Test
    @DisplayName("URLs contêm o nome da raça")
    void urlsContainBreedName() {
      BreedImagesResponse body = breedFlow.breedImages(BREED).as(BreedImagesResponse.class);
      body.images().forEach(url ->
          assertTrue(url.contains(BREED), "URL não contém a raça '" + BREED + "': " + url));
    }

    @Test
    @DisplayName("Raça sem sub-raça também retorna imagens")
    void breedWithoutSubBreedReturnsImages() {
      Response response = breedFlow.breedImages(BREED_NO_SUB);
      ResponseValidator.assertSuccess(response);
      BreedValidator.validateBreedImages(response.as(BreedImagesResponse.class), BREED_NO_SUB);
    }

    @Test
    @DisplayName("Sub-raça retorna lista não vazia de imagens")
    void subBreedReturnsImages() {
      Response response = breedFlow.subBreedImages(BREED, "afghan");
      ResponseValidator.assertSuccess(response);
      BreedValidator.validateBreedImages(response.as(BreedImagesResponse.class), BREED);
    }

    @Test
    @Tag("unstable")
    @DisplayName("Raça principal combina imagens de múltiplas sub-raças")
    void mainBreedCombinesSubBreeds() {
      BreedImagesResponse body = breedFlow.breedImages(BREED).as(BreedImagesResponse.class);
      BreedValidator.assertMultipleSubBreedImages(body, BREED, 2);
    }
  }

  @Nested
  @DisplayName("Negativos")
  class Negative {

    @Test
    @DisplayName("Raça inexistente retorna 404 e status error")
    void unknownBreedReturns404() {
      Response response = breedFlow.breedImages("notabreed");
      ResponseValidator.assertError(response, 404);
      ResponseValidator.assertErrorMessageContains(response, "not found");
    }

    @Test
    @DisplayName("Nome de raça é case-sensitive (maiúscula retorna 404)")
    void breedNameIsCaseSensitive() {
      Response response = breedFlow.breedImages("Hound");
      ResponseValidator.assertError(response, 404);
    }
  }
}
