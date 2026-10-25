package org.sitenetsoft.quarkus.webfinger.client;

import java.net.URI;
import java.util.Set;

import org.sitenetsoft.quarkus.webfinger.AcctUri;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;

import io.smallrye.mutiny.Uni;

/**
 * Mutiny form of {@link WebFingerClient}.
 */
public interface ReactiveWebFingerClient {

    Uni<JsonResourceDescriptor> lookup(URI resource, Set<String> rels);

    Uni<JsonResourceDescriptor> lookup(AcctUri account, Set<String> rels);
}
