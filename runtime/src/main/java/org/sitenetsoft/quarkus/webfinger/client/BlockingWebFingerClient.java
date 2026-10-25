package org.sitenetsoft.quarkus.webfinger.client;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sitenetsoft.quarkus.webfinger.AcctUri;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.WebFingerRuntimeConfig;

/**
 * Blocking facade over {@link WebFingerClientImpl}.
 */
@ApplicationScoped
public class BlockingWebFingerClient implements WebFingerClient {

    private final WebFingerClientImpl delegate;
    private final Duration limit;

    @Inject
    public BlockingWebFingerClient(WebFingerClientImpl delegate, WebFingerRuntimeConfig config) {
        this.delegate = delegate;
        // direct + host-meta.json + host-meta + templated lookup, each bounded by the per-request timeout
        this.limit = config.client().timeout().multipliedBy(4).plusSeconds(2);
    }

    @Override
    public JsonResourceDescriptor lookup(URI resource, Set<String> rels) {
        return delegate.lookup(resource, rels).await().atMost(limit);
    }

    @Override
    public JsonResourceDescriptor lookup(AcctUri account, Set<String> rels) {
        return delegate.lookup(account, rels).await().atMost(limit);
    }
}
