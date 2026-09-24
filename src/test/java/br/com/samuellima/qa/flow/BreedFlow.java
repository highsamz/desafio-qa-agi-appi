package br.com.samuellima.qa.flow;

import br.com.samuellima.qa.client.DogApiClient;
import br.com.samuellima.qa.model.BreedListResponse;
import io.restassured.response.Response;


public class BreedFlow {

    private final DogApiClient client;

    public BreedFlow() {
        this(new DogApiClient());
    }

    public BreedFlow(DogApiClient client) {
        this.client = client;
    }

    public Response listAllBreeds() {
        return client.getAllBreeds();
    }

    public Response breedImages(String breed) {
        return client.getBreedImages(breed);
    }

    public Response subBreedImages(String breed, String subBreed) {
        return client.getSubBreedImages(breed, subBreed);
    }


    public Response imagesForFirstBreed() {
        return breedImages(firstBreed());
    }

    public String firstBreed() {
        BreedListResponse list = listAllBreeds().as(BreedListResponse.class);
        return list.breeds().stream()
                .sorted()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Nenhuma raça retornada por /breeds/list/all"));
    }
}