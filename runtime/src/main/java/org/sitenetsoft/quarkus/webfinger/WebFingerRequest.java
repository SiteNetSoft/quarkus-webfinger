package org.sitenetsoft.quarkus.webfinger;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

/**
 * An incoming WebFinger query. {@code host} is the request's {@code Host} header, so a resolver can serve several
 * domains. {@code rels} may be ignored: the extension filters links after merging.
 */
public record WebFingerRequest(URI resource, Set<String> rels, String host) {

    public WebFingerRequest {
        rels = rels == null ? Set.of() : Set.copyOf(rels);
    }

    /**
     * The resource as an {@code acct:} URI, when it is one.
     */
    public Optional<AcctUri> account() {
        if (resource == null || !"acct".equalsIgnoreCase(resource.getScheme())) {
            return Optional.empty();
        }
        try {
            return Optional.of(AcctUri.parse(resource.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
