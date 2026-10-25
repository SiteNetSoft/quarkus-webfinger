package org.sitenetsoft.quarkus.webfinger.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Well-known paths must sit at the domain root even when the app lives under {@code quarkus.http.root-path}.
 */
public class RootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResolvers.class, TestResolvers.AliceResolver.class))
            .overrideConfigKey("quarkus.http.root-path", "/api")
            .overrideConfigKey("quarkus.webfinger.server.host-meta.enabled", "true");

    @Test
    void webfingerIsServedAtTheServerRoot() {
        given().baseUri("http://localhost:8081").basePath("/").queryParam("resource", "acct:alice@example.com")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200).body("subject", equalTo("acct:alice@example.com"));
    }

    @Test
    void webfingerIsNotUnderTheRootPath() {
        given().baseUri("http://localhost:8081").basePath("/").queryParam("resource", "acct:alice@example.com")
                .when().get("/api/.well-known/webfinger")
                .then().statusCode(404);
    }

    @Test
    void hostMetaIsServedAtTheServerRoot() {
        given().baseUri("http://localhost:8081").basePath("/").when().get("/.well-known/host-meta").then().statusCode(200);
    }
}
