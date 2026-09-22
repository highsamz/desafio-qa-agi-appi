package br.com.samuellima.qa.support;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;


public final class RestAssuredConfig {

    public static final String DEFAULT_BASE_URI = "https://dog.ceo";
    public static final String BASE_PATH = "/api";

    private static final String BASE_URI_PROPERTY = "dogapi.baseUri";

    private RestAssuredConfig() {
    }

    public static String baseUri() {
        return System.getProperty(BASE_URI_PROPERTY, DEFAULT_BASE_URI);
    }


    public static RequestSpecification requestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri())
                .setBasePath(BASE_PATH)
                .setAccept(ContentType.JSON)
                .log(LogDetail.URI)
                .addFilter(new ErrorLoggingFilter())
                .build();
    }
}