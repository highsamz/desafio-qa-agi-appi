package br.com.samuellima.qa.client;

import static io.restassured.RestAssured.given;

import br.com.samuellima.qa.support.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


public class DogApiClient {

    private static final String LIST_ALL_BREEDS = "/breeds/list/all";
    private static final String BREED_IMAGES = "/breed/{breed}/images";
    private static final String SUB_BREED_IMAGES = "/breed/{breed}/{subBreed}/images";
    private static final String RANDOM_IMAGE = "/breeds/image/random";

    private final RequestSpecification spec;

    public DogApiClient() {
        this(RestAssuredConfig.requestSpec());
    }

    public DogApiClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response getAllBreeds() {
        return given()
                .spec(spec)
                .when()
                .get(LIST_ALL_BREEDS)
                .andReturn();
    }

    public Response getBreedImages(String breed) {
        return given()
                .spec(spec)
                .pathParam("breed", breed)
                .when()
                .get(BREED_IMAGES)
                .andReturn();
    }

    public Response getSubBreedImages(String breed, String subBreed) {
        return given()
                .spec(spec)
                .pathParam("breed", breed)
                .pathParam("subBreed", subBreed)
                .when()
                .get(SUB_BREED_IMAGES)
                .andReturn();
    }

    public Response getRandomImage() {
        return given()
                .spec(spec)
                .when()
                .get(RANDOM_IMAGE)
                .andReturn();
    }
}