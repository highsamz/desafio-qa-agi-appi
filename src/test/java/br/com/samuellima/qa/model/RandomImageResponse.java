package br.com.samuellima.qa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record RandomImageResponse(
        @JsonProperty("message") String message,
        @JsonProperty("status") String status,
        @JsonProperty("code") Integer code) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public boolean isError() {
        return "error".equalsIgnoreCase(status);
    }

    public String imageUrl() {
        return message;
    }
}