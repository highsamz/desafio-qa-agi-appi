package br.com.samuellima.qa.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.samuellima.qa.model.BreedListResponse;
import br.com.samuellima.qa.support.BaseTest;
import br.com.samuellima.qa.validator.BreedValidator;
import br.com.samuellima.qa.validator.ResponseValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("api")
@Tag("breeds-list")
@DisplayName("GET /breeds/list/all")
class BreedsListTests extends BaseTest {

  private static final String SCHEMA = "schemas/breeds-list-schema.json";

  @Nested
  @DisplayName("Contrato")
  class Contract {

    @Test
    @DisplayName("Retorna 200, JSON e status success")
    void returnsSuccessEnvelope() {
      Response response = breedFlow.listAllBreeds();
      ResponseValidator.assertSuccess(response);
    }

    @Test
    @DisplayName("Corpo adere ao JSON Schema")
    void matchesSchema() {
      Response response = breedFlow.listAllBreeds();
      ResponseValidator.assertMatchesSchema(response, SCHEMA);
    }
  }

  @Nested
  @DisplayName("Conteúdo")
  class Content {

    @Test
    @DisplayName("Lista de raças íntegra e não vazia")
    void breedListIsValid() {
      BreedListResponse body = breedFlow.listAllBreeds().as(BreedListResponse.class);
      BreedValidator.validateBreedList(body);
      assertTrue(body.breedCount() > 0, "Deveria retornar ao menos uma raça");
      reportInfo("Raças retornadas", String.valueOf(body.breedCount()));
    }

    @Test
    @DisplayName("Raça conhecida traz as sub-raças esperadas")
    void knownBreedHasSubBreeds() {
      BreedListResponse body = breedFlow.listAllBreeds().as(BreedListResponse.class);
      BreedValidator.assertContainsBreed(body, "hound");
      BreedValidator.assertHasSubBreeds(body, "hound", "afghan", "basset");
    }

    @Test
    @DisplayName("Raça sem sub-raça retorna lista vazia")
    void breedWithoutSubBreeds() {
      BreedListResponse body = breedFlow.listAllBreeds().as(BreedListResponse.class);
      BreedValidator.assertContainsBreed(body, "affenpinscher");
      BreedValidator.assertNoSubBreeds(body, "affenpinscher");
    }
  }
}
