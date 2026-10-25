package org.sitenetsoft.quarkus.webfinger.oidc;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sitenetsoft.quarkus.webfinger.AcctUri;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.Link;
import org.sitenetsoft.quarkus.webfinger.WebFingerRequest;
import org.sitenetsoft.quarkus.webfinger.WebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.WebFingerRuntimeConfig;

/**
 * Built-in resolver for OpenID Connect Discovery (section 2): answers the
 * {@code http://openid.net/specs/connect/1.0/issuer} rel for {@code acct:} addresses on the configured domains.
 */
@ApplicationScoped
@Priority(0)
public class OidcIssuerResolver implements WebFingerResolver {

    public static final String ISSUER_REL = "http://openid.net/specs/connect/1.0/issuer";

    private final WebFingerRuntimeConfig config;

    @Inject
    public OidcIssuerResolver(WebFingerRuntimeConfig config) {
        this.config = config;
    }

    @Override
    public Optional<JsonResourceDescriptor> resolve(WebFingerRequest request) {
        Optional<AcctUri> account = request.account();
        if (account.isEmpty()) {
            return Optional.empty();
        }
        URI issuer = config.oidc().issuer().orElseThrow(() -> new IllegalStateException(
                "quarkus.webfinger.oidc.enabled=true requires quarkus.webfinger.oidc.issuer"));
        List<String> domains = config.oidc().domains().orElseThrow(() -> new IllegalStateException(
                "quarkus.webfinger.oidc.enabled=true requires quarkus.webfinger.oidc.domains"));
        String host = account.get().host();
        boolean match = domains.stream().map(d -> d.strip().toLowerCase(Locale.ROOT)).anyMatch(host::equals);
        if (!match) {
            return Optional.empty();
        }
        return Optional.of(JsonResourceDescriptor.of(request.resource()).withLink(Link.of(ISSUER_REL, issuer)));
    }
}
