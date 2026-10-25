package org.sitenetsoft.quarkus.webfinger.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class HostMetaTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResolvers.class, TestResolvers.AliceResolver.class))
            .overrideConfigKey("quarkus.webfinger.server.host-meta.enabled", "true");

    @Test
    void xrdHasLrddTemplate() {
        given().header("Host", "social.example")
                .when().get("/.well-known/host-meta")
                .then().statusCode(200)
                .header("Content-Type", startsWith("application/xrd+xml"))
                .body(containsString("<XRD xmlns=\"http://docs.oasis-open.org/ns/xri/xrd-1.0\">"))
                .body(containsString("rel=\"lrdd\""))
                .body(containsString("template=\"https://social.example/.well-known/webfinger?resource={uri}\""));
    }

    @Test
    void jsonHasLrddLink() {
        given().header("Host", "social.example")
                .when().get("/.well-known/host-meta.json")
                .then().statusCode(200)
                .body("links[0].rel", equalTo("lrdd"))
                .body("links[0].template", equalTo("https://social.example/.well-known/webfinger?resource={uri}"));
    }
}
