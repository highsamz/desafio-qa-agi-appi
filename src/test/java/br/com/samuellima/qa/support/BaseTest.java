package br.com.samuellima.qa.support;

import br.com.samuellima.qa.flow.BreedFlow;
import br.com.samuellima.qa.flow.ImageFlow;
import br.com.samuellima.qa.report.ExecutionRegistry;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {

    protected final BreedFlow breedFlow = new BreedFlow();
    protected final ImageFlow imageFlow = new ImageFlow();

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
    }

    protected void reportInfo(String key, String value) {
        ExecutionRegistry.registerInfo(key, value);
    }
}