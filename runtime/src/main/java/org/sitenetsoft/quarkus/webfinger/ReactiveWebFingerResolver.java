package org.sitenetsoft.quarkus.webfinger;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Mutiny form of {@link WebFingerResolver}.
 */
public interface ReactiveWebFingerResolver {

    Uni<Optional<JsonResourceDescriptor>> resolve(WebFingerRequest request);
}
