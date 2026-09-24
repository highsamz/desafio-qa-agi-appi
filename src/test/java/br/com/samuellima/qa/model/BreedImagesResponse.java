package br.com.samuellima.qa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record BreedImagesResponse(
        @JsonProperty("message") List<String> message,
        @JsonProperty("status") String status,
        @JsonProperty("code") Integer code) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public boolean isError() {
        return "error".equalsIgnoreCase(status);
    }

    public List<String> images() {
        return message != null ? message : List.of();
    }

    public int count() {
        return images().size();
    }

    public String first() {
        return images().isEmpty() ? null : images().get(0);
    }
}