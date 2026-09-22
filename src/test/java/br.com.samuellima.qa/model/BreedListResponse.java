package br.com.samuellima.qa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;


@JsonIgnoreProperties(ignoreUnknown = true)
public record BreedListResponse(
        @JsonProperty("message") Map<String, List<String>> message,
        @JsonProperty("status") String status,
        @JsonProperty("code") Integer code) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public boolean isError() {
        return "error".equalsIgnoreCase(status);
    }

    public Set<String> breeds() {
        return message != null ? message.keySet() : Set.of();
    }

    public List<String> subBreeds(String breed) {
        return message != null ? message.getOrDefault(breed, List.of()) : List.of();
    }

    public int breedCount() {
        return breeds().size();
    }
}