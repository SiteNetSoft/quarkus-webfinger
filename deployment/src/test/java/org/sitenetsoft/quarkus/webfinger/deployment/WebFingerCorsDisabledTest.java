package org.sitenetsoft.quarkus.webfinger.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WebFingerCorsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResolvers.class, TestResolvers.AliceResolver.class))
            .overrideConfigKey("quarkus.webfinger.server.cors", "false");

    @Test
    void corsCanBeDisabled() {
        given().queryParam("resource", "acct:alice@example.com")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", nullValue());
    }
}
