package org.sitenetsoft.quarkus.webfinger.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * The built-in OpenID Connect issuer resolver (RFC 7033 section 3.1 shape), enabled by config alone.
 */
public class OidcIssuerResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.webfinger.oidc.enabled", "true")
            .overrideConfigKey("quarkus.webfinger.oidc.issuer", "https://openid.example.com")
            .overrideConfigKey("quarkus.webfinger.oidc.domains", "example.com,example.org");

    @Test
    void answersConfiguredDomain() {
        given().queryParam("resource", "acct:carol@example.com")
                .queryParam("rel", "http://openid.net/specs/connect/1.0/issuer")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200)
                .body("subject", equalTo("acct:carol@example.com"))
                .body("links", hasSize(1))
                .body("links[0].rel", equalTo("http://openid.net/specs/connect/1.0/issuer"))
                .body("links[0].href", equalTo("https://openid.example.com"));
    }

    @Test
    void answersSecondDomainCaseInsensitively() {
        given().queryParam("resource", "acct:carol@Example.ORG")
                .when().get("/.well-known/webfinger")
                .then().statusCode(200);
    }

    @Test
    void ignoresOtherDomain() {
        given().queryParam("resource", "acct:carol@elsewhere.example")
                .when().get("/.well-known/webfinger")
                .then().statusCode(404);
    }

    @Test
    void ignoresNonAcct() {
        given().queryParam("resource", "https://example.com/carol")
                .when().get("/.well-known/webfinger")
                .then().statusCode(404);
    }
}
