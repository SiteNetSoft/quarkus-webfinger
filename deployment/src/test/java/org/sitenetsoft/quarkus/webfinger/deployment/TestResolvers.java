package org.sitenetsoft.quarkus.webfinger.deployment;

import java.net.URI;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.Link;
import org.sitenetsoft.quarkus.webfinger.ReactiveWebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.WebFingerRequest;
import org.sitenetsoft.quarkus.webfinger.WebFingerResolver;

import io.smallrye.mutiny.Uni;

public final class TestResolvers {

    static final URI ALICE = URI.create("acct:alice@example.com");
    static final URI PROP = URI.create("http://example.com/p");

    @ApplicationScoped
    @Priority(10)
    public static class AliceResolver implements WebFingerResolver {
        @Override
        public Optional<JsonResourceDescriptor> resolve(WebFingerRequest request) {
            if (!ALICE.equals(request.resource())) {
                return Optional.empty();
            }
            return Optional.of(JsonResourceDescriptor.of(ALICE)
                    .withLink(Link.of("self", "application/activity+json", URI.create("https://example.com/users/alice")))
                    .withProperty(PROP, "low"));
        }
    }

    @ApplicationScoped
    @Priority(100)
    public static class OidcLikeResolver implements WebFingerResolver {
        @Override
        public Optional<JsonResourceDescriptor> resolve(WebFingerRequest request) {
            if (!ALICE.equals(request.resource())) {
                return Optional.empty();
            }
            return Optional.of(JsonResourceDescriptor.of(ALICE)
                    .withAlias(URI.create("https://example.com/@alice"))
                    .withLink(Link.of("http://openid.net/specs/connect/1.0/issuer", URI.create("https://openid.example.com")))
                    .withProperty(PROP, "high"));
        }
    }

    @ApplicationScoped
    public static class ReactiveBobResolver implements ReactiveWebFingerResolver {
        @Override
        public Uni<Optional<JsonResourceDescriptor>> resolve(WebFingerRequest request) {
            if (!"acct:bob@example.com".equals(request.resource().toString())) {
                return Uni.createFrom().item(Optional.empty());
            }
            return Uni.createFrom().item(Optional.of(JsonResourceDescriptor.of(request.resource())
                    .withLink(Link.of("self", URI.create("https://example.com/users/bob")))));
        }
    }

    @ApplicationScoped
    public static class ArticleResolver implements WebFingerResolver {
        @Override
        public Optional<JsonResourceDescriptor> resolve(WebFingerRequest request) {
            if (!"https://example.com/article/1".equals(request.resource().toString())) {
                return Optional.empty();
            }
            return Optional.of(JsonResourceDescriptor.of(request.resource())
                    .withLink(Link.of("author", URI.create("https://example.com/users/alice"))));
        }
    }
}
