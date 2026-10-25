package org.sitenetsoft.quarkus.webfinger.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.webfinger.AcctUri;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.client.ReactiveWebFingerClient;
import org.sitenetsoft.quarkus.webfinger.client.WebFingerClient;
import org.sitenetsoft.quarkus.webfinger.client.WebFingerInvalidResponseException;
import org.sitenetsoft.quarkus.webfinger.client.WebFingerNotFoundException;
import org.sitenetsoft.quarkus.webfinger.client.WebFingerUnreachableException;

import io.quarkus.test.QuarkusUnitTest;

public class WebFingerClientTest {

    static final FakeWebFingerHosts hosts = new FakeWebFingerHosts();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(FakeWebFingerHosts.class))
            .overrideConfigKey("quarkus.safe-fetch.https-only", "false")
            .overrideConfigKey("quarkus.safe-fetch.allow-private-addresses", "true")
            .overrideConfigKey("quarkus.webfinger.client.base-url-override", "http://127.0.0.1:" + hosts.port());

    @AfterAll
    static void stop() {
        hosts.stop();
    }

    @Inject
    WebFingerClient client;

    @Inject
    ReactiveWebFingerClient reactive;

    @Test
    void looksUpDirect() {
        JsonResourceDescriptor jrd = client.lookup(URI.create("acct:alice@direct.test"), Set.of());
        assertEquals(URI.create("acct:alice@direct.test"), jrd.subject());
        assertEquals(2, jrd.links().size());
    }

    @Test
    void relsAreSentAndApplied() {
        JsonResourceDescriptor jrd = client.lookup(URI.create("acct:alice@direct.test"), Set.of("self"));
        assertEquals(1, jrd.links().size());
        assertEquals("self", jrd.links().get(0).rel());
    }

    @Test
    void cachesWithinMaxAge() {
        URI r = URI.create("acct:alice@direct.test");
        client.lookup(r, Set.of("http://webfinger.net/rel/profile-page"));
        int before = hosts.hits("direct.test/.well-known/webfinger");
        client.lookup(r, Set.of("http://webfinger.net/rel/profile-page"));
        assertEquals(before, hosts.hits("direct.test/.well-known/webfinger"));
    }

    @Test
    void cachedLookupStillFiltersRels() {
        URI r = URI.create("acct:alice@direct.test");
        assertEquals(2, client.lookup(r, Set.of()).links().size());
        JsonResourceDescriptor filtered = client.lookup(r, Set.of("self"));
        assertEquals(1, filtered.links().size());
        // same rels again: served from cache, still filtered
        assertEquals(1, client.lookup(r, Set.of("self")).links().size());
    }

    @Test
    void fallsBackToLrddAndRemembersTemplate() {
        JsonResourceDescriptor jrd = client.lookup(new AcctUri("bob", "lrdd.test"), Set.of());
        assertEquals(URI.create("https://lrdd.test/users/bob"), jrd.links().get(0).href());
        int hostMeta = hosts.hits("lrdd.test/.well-known/host-meta");
        int webfinger = hosts.hits("lrdd.test/.well-known/webfinger");
        assertThrows(WebFingerNotFoundException.class, () -> client.lookup(new AcctUri("nobody", "lrdd.test"), Set.of()));
        assertEquals(hostMeta, hosts.hits("lrdd.test/.well-known/host-meta"), "host-meta fetched again");
        assertEquals(webfinger, hosts.hits("lrdd.test/.well-known/webfinger"), "direct endpoint tried again");
    }

    @Test
    void notFoundIsTyped() {
        assertThrows(WebFingerNotFoundException.class, () -> client.lookup(URI.create("acct:nobody@direct.test"), Set.of()));
    }

    @Test
    void invalidJsonIsTyped() {
        assertThrows(WebFingerInvalidResponseException.class, () -> client.lookup(URI.create("acct:bad@direct.test"), Set.of()));
    }

    @Test
    void missingSubjectIsTyped() {
        assertThrows(WebFingerInvalidResponseException.class, () -> client.lookup(URI.create("acct:nosubject@direct.test"), Set.of()));
    }

    @Test
    void serverErrorIsTyped() {
        assertThrows(WebFingerInvalidResponseException.class, () -> client.lookup(URI.create("acct:x@error.test"), Set.of()));
    }

    @Test
    void unreachableIsTyped() {
        assertThrows(WebFingerUnreachableException.class, () -> client.lookup(URI.create("acct:x@down.test"), Set.of()));
    }

    @Test
    void resourceWithoutHostIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> client.lookup(URI.create("urn:isbn:1"), Set.of()));
    }

    @Test
    void reactiveForm() {
        JsonResourceDescriptor jrd = reactive.lookup(new AcctUri("alice", "direct.test"), Set.of())
                .await().atMost(Duration.ofSeconds(5));
        assertEquals(URI.create("acct:alice@direct.test"), jrd.subject());
    }
}
