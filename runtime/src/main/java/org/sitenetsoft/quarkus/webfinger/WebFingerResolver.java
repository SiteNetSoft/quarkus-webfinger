package org.sitenetsoft.quarkus.webfinger;

import java.util.Optional;

/**
 * Supplies resource data for the WebFinger endpoint. Register any number of CDI beans; all run, highest
 * {@code @Priority} first, and results for the same subject are merged.
 */
public interface WebFingerResolver {

    Optional<JsonResourceDescriptor> resolve(WebFingerRequest request);
}
