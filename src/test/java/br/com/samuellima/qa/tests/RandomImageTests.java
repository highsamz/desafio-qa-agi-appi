package br.com.samuellima.qa.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.samuellima.qa.model.RandomImageResponse;
import br.com.samuellima.qa.support.BaseTest;
import br.com.samuellima.qa.validator.ImageValidator;
import br.com.samuellima.qa.validator.ResponseValidator;
import io.restassured.response.Response;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("api")
@Tag("random")
@DisplayName("GET /breeds/image/random")
class RandomImageTests extends BaseTest {

  private static final String SCHEMA = "schemas/image-response-schema.json";

  @Nested
  @DisplayName("Contrato")
  class Contract {

    @Test
    @DisplayName("Retorna 200, JSON e status success")
    void returnsSuccessEnvelope() {
      Response response = imageFlow.randomImage();
      ResponseValidator.assertSuccess(response);
    }

    @Test
    @DisplayName("Corpo adere ao JSON Schema")
    void matchesSchema() {
      Response response = imageFlow.randomImage();
      ResponseValidator.assertMatchesSchema(response, SCHEMA);
    }
  }

  @Nested
  @DisplayName("Conteúdo")
  class Content {

    @Test
    @DisplayName("Message é uma URL de imagem válida")
    void returnsValidImageUrl() {
      RandomImageResponse body = imageFlow.randomImage().as(RandomImageResponse.class);
      ImageValidator.validateRandom(body);
      reportInfo("URL", body.imageUrl());
    }
  }

  @Nested
  @DisplayName("Aleatoriedade")
  class Randomness {

    @Test
    @Tag("unstable")
    @DisplayName("Chamadas repetidas retornam URLs majoritariamente distintas")
    void repeatedCallsReturnDistinctUrls() {
      Set<String> urls = imageFlow.collectRandomImageUrls(5);
      assertTrue(urls.size() >= 2,
          "Esperava ao menos 2 URLs distintas em 5 chamadas, veio: " + urls.size());
      reportInfo("Distintas em 5 chamadas", String.valueOf(urls.size()));
    }
  }

  @Nested
  @DisplayName("Acessibilidade (externo)")
  class Reachability {

    @Test
    @Tag("external")
    @DisplayName("A imagem retornada está acessível (HEAD 200)")
    void imageIsReachable() {
      RandomImageResponse body = imageFlow.randomImage().as(RandomImageResponse.class);
      ImageValidator.assertReachable(body.imageUrl());
    }
  }
}
