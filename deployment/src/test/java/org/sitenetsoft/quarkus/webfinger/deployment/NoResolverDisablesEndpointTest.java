package org.sitenetsoft.quarkus.webfinger.deployment;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Without any resolver bean the endpoint is not registered and the build says why.
 */
public class NoResolverDisablesEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setLogRecordPredicate(r -> r.getLevel().intValue() >= Level.INFO.intValue()
                    && r.getLoggerName().startsWith("org.sitenetsoft.quarkus.webfinger"))
            .assertLogRecords(records -> assertTrue(records.stream()
                    .anyMatch(r -> r.getMessage().contains("not registered")), "expected a 'not registered' log line"));

    @Test
    void endpointIsAbsent() {
        given().when().get("/.well-known/webfinger?resource=acct:a@b.example").then().statusCode(404);
    }
}
