package org.sitenetsoft.quarkus.webfinger.client;

import java.net.URI;
import java.util.Set;

import org.sitenetsoft.quarkus.webfinger.AcctUri;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;

/**
 * Blocking WebFinger lookups with host-meta (LRDD) fallback and caching. Safe on worker and virtual threads.
 *
 * @throws WebFingerNotFoundException when the host answers 404 and no LRDD template helps
 * @throws WebFingerInvalidResponseException on unexpected statuses or unparseable JRDs
 * @throws WebFingerUnreachableException when the host cannot be reached (or is refused by safe-fetch)
 */
public interface WebFingerClient {

    JsonResourceDescriptor lookup(URI resource, Set<String> rels);

    JsonResourceDescriptor lookup(AcctUri account, Set<String> rels);
}
