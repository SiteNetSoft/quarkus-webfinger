package org.sitenetsoft.quarkus.webfinger.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WebFingerServerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestResolvers.class, TestResolvers.AliceResolver.class, TestResolvers.OidcLikeResolver.class,
                            TestResolvers.ReactiveBobResolver.class, TestResolvers.ArticleResolver.class));

    @Test
    void mergesTwoResolvers() {
        given().queryParam("resource", "acct:alice@example.com")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .contentType("application/jrd+json")
                .header("Access-Control-Allow-Origin", "*")
                .body("subject", equalTo("acct:alice@example.com"))
                .body("aliases", contains("https://example.com/@alice"))
                .body("properties.'http://example.com/p'", equalTo("high"))
                .body("links.rel", contains("http://openid.net/specs/connect/1.0/issuer", "self"))
                .body("links[1].type", equalTo("application/activity+json"));
    }

    @Test
    void reactiveResolverAnswers() {
        given().queryParam("resource", "acct:bob@example.com")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("links[0].href", equalTo("https://example.com/users/bob"));
    }

    @Test
    void unknownSubjectIs404() {
        given().queryParam("resource", "acct:nobody@example.com")
                .when().get("/.well-known/webfinger")
                .then().statusCode(404);
    }

    @Test
    void missingResourceIs400() {
        given().when().get("/.well-known/webfinger").then().statusCode(400);
    }

    @Test
    void malformedResourceIs400() {
        given().queryParam("resource", "::not a uri")
                .when().get("/.well-known/webfinger")
                .then().statusCode(400);
    }

    @Test
    void relFilterKeepsOnlyMatching() {
        given().queryParam("resource", "acct:alice@example.com").queryParam("rel", "self")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("links", hasSize(1))
                .body("links[0].rel", equalTo("self"));
    }

    @Test
    void repeatedRelParamsAreUnioned() {
        given().queryParam("resource", "acct:alice@example.com").queryParam("rel", "self", "http://openid.net/specs/connect/1.0/issuer")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("links.rel", containsInAnyOrder("self", "http://openid.net/specs/connect/1.0/issuer"));
    }

    @Test
    void relFilterNoMatchIs200WithEmptyLinks() {
        given().queryParam("resource", "acct:alice@example.com").queryParam("rel", "nope")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("subject", equalTo("acct:alice@example.com"))
                .body("links", empty());
    }

    @Test
    void bareAccountIsNormalisedToAcct() {
        given().queryParam("resource", "alice@example.com")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("subject", equalTo("acct:alice@example.com"));
    }

    @Test
    void httpsResourceIsPassedThrough() {
        given().queryParam("resource", "https://example.com/article/1")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("links[0].rel", equalTo("author"));
    }
}
