package br.com.samuellima.qa.flow;

import br.com.samuellima.qa.client.DogApiClient;
import br.com.samuellima.qa.model.RandomImageResponse;
import io.restassured.response.Response;
import java.util.LinkedHashSet;
import java.util.Set;


public class ImageFlow {

    private final DogApiClient client;

    public ImageFlow() {
        this(new DogApiClient());
    }

    public ImageFlow(DogApiClient client) {
        this.client = client;
    }

    public Response randomImage() {
        return client.getRandomImage();
    }


    public Set<String> collectRandomImageUrls(int times) {
        Set<String> urls = new LinkedHashSet<>();
        for (int i = 0; i < times; i++) {
            urls.add(randomImage().as(RandomImageResponse.class).imageUrl());
        }
        return urls;
    }
}