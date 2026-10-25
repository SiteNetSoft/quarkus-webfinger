package org.sitenetsoft.quarkus.webfinger.client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.safefetch.FetchOptions;
import org.sitenetsoft.quarkus.safefetch.FetchResponse;
import org.sitenetsoft.quarkus.safefetch.ReactiveSafeFetchClient;
import org.sitenetsoft.quarkus.safefetch.SafeFetchException;
import org.sitenetsoft.quarkus.webfinger.AcctUri;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.WebFingerRuntimeConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import io.smallrye.mutiny.Uni;

/**
 * The single WebFinger client implementation (reactive); {@link BlockingWebFingerClient} wraps it.
 */
@ApplicationScoped
public class WebFingerClientImpl implements ReactiveWebFingerClient {

    private static final Logger LOG = Logger.getLogger("org.sitenetsoft.quarkus.webfinger");
    private static final Pattern MAX_AGE = Pattern.compile("(?:^|[,\\s])max-age\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    static final String ACCEPT = "application/jrd+json, application/json;q=0.9";

    private record Cached(JsonResourceDescriptor jrd, Duration ttl) {
    }

    private final ReactiveSafeFetchClient fetch;
    private final ObjectMapper mapper;
    private final WebFingerRuntimeConfig config;
    private final HostMetaTemplates templates;
    private final Cache<String, Cached> results;

    @Inject
    public WebFingerClientImpl(ReactiveSafeFetchClient fetch, ObjectMapper mapper, WebFingerRuntimeConfig config) {
        this.fetch = fetch;
        this.mapper = mapper;
        this.config = config;
        this.templates = new HostMetaTemplates(config.client().cache().maxEntries(), config.client().cache().maxTtl());
        this.results = Caffeine.newBuilder().maximumSize(config.client().cache().maxEntries())
                .expireAfter(new Expiry<String, Cached>() {
                    @Override
                    public long expireAfterCreate(String key, Cached value, long currentTime) {
                        return value.ttl().toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, Cached value, long currentTime, long currentDuration) {
                        return value.ttl().toNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, Cached value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                }).build();
    }

    @Override
    public Uni<JsonResourceDescriptor> lookup(AcctUri account, Set<String> rels) {
        return lookup(account.toUri(), rels);
    }

    @Override
    public Uni<JsonResourceDescriptor> lookup(URI resource, Set<String> rels) {
        String host = hostOf(resource);
        Set<String> relSet = rels == null ? Set.of() : Set.copyOf(rels);
        String key = resource + "|" + new TreeSet<>(relSet);
        Cached cached = results.getIfPresent(key);
        if (cached != null) {
            return Uni.createFrom().item(cached.jrd().filterRels(relSet));
        }
        return direct(host, resource, relSet)
                .onFailure(SafeFetchException.class).transform(t -> new WebFingerUnreachableException(
                        "WebFinger lookup of " + resource + " failed: " + t.getMessage(), t))
                .invoke(result -> {
                    if (result.ttl() != null && !result.ttl().isZero()) {
                        results.put(key, new Cached(result.jrd(), result.ttl()));
                    }
                })
                .map(r -> r.jrd().filterRels(relSet));
    }

    static String hostOf(URI resource) {
        String host;
        if ("acct".equalsIgnoreCase(resource.getScheme())) {
            host = AcctUri.parse(resource.toString()).host();
        } else {
            host = resource.getHost();
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("resource has no host: " + resource);
        }
        return host.toLowerCase(Locale.ROOT);
    }

    private record Fetched(JsonResourceDescriptor jrd, Duration ttl) {
    }

    private Uni<Fetched> direct(String host, URI resource, Set<String> rels) {
        StringBuilder url = new StringBuilder("https://").append(host).append("/.well-known/webfinger?resource=")
                .append(encode(resource.toString()));
        for (String rel : rels) {
            url.append("&rel=").append(encode(rel));
        }
        return get(host, URI.create(url.toString())).chain(response -> {
            if (response.status() == 200) {
                return Uni.createFrom().item(parse(resource, response));
            }
            if (response.status() == 404) {
                if (config.client().hostMetaFallback()) {
                    return viaTemplate(host, resource);
                }
                return Uni.createFrom().failure(new WebFingerNotFoundException("No WebFinger data for " + resource));
            }
            return Uni.createFrom().failure(new WebFingerInvalidResponseException(
                    "WebFinger endpoint on " + host + " answered " + response.status()));
        });
    }

    private Uni<Fetched> viaTemplate(String host, URI resource) {
        Optional<Optional<String>> known = templates.get(host);
        Uni<Optional<String>> template = known.map(t -> Uni.createFrom().item(t))
                .orElseGet(() -> discoverTemplate(host).invoke(t -> templates.put(host, t)));
        return template.chain(t -> {
            if (t.isEmpty()) {
                return Uni.createFrom().failure(new WebFingerNotFoundException("No WebFinger data for " + resource));
            }
            URI url;
            try {
                url = URI.create(t.get().replace("{uri}", encode(resource.toString())));
            } catch (IllegalArgumentException e) {
                return Uni.createFrom().failure(new WebFingerInvalidResponseException(
                        "LRDD template on " + host + " is not a valid URL", e));
            }
            return get(url.getHost() == null ? host : url.getHost(), url).chain(response -> {
                if (response.status() == 200) {
                    return Uni.createFrom().item(parse(resource, response));
                }
                if (response.status() == 404) {
                    return Uni.createFrom().failure(new WebFingerNotFoundException("No WebFinger data for " + resource));
                }
                return Uni.createFrom().failure(new WebFingerInvalidResponseException(
                        "LRDD endpoint on " + host + " answered " + response.status()));
            });
        });
    }

    private Uni<Optional<String>> discoverTemplate(String host) {
        return get(host, URI.create("https://" + host + "/.well-known/host-meta.json")).chain(json -> {
            if (json.status() == 200) {
                try {
                    return Uni.createFrom().item(HostMetaTemplates.fromJrd(
                            mapper.readValue(json.body(), JsonResourceDescriptor.class)));
                } catch (Exception e) {
                    LOG.debugf("Ignoring unparseable host-meta.json on %s", host);
                }
            }
            return get(host, URI.create("https://" + host + "/.well-known/host-meta")).map(xrd -> {
                if (xrd.status() != 200) {
                    return Optional.<String> empty();
                }
                return HostMetaTemplates.fromXrd(xrd.bodyAsString());
            });
        });
    }

    private Uni<FetchResponse> get(String host, URI url) {
        FetchOptions options = FetchOptions.defaults()
                .withTimeout(config.client().timeout())
                .withMaxResponseSize(config.client().maxResponseSize().asLongValue())
                .withHeader("Accept", ACCEPT);
        URI target = url;
        Optional<String> override = config.client().baseUrlOverride();
        if (override.isPresent()) {
            String path = url.getRawPath() + (url.getRawQuery() == null ? "" : "?" + url.getRawQuery());
            target = URI.create(override.get() + path);
            options = options.withHeader("Host", url.getHost());
        }
        return fetch.get(target, options);
    }

    private Fetched parse(URI resource, FetchResponse response) {
        JsonResourceDescriptor jrd;
        try {
            jrd = mapper.readValue(response.body(), JsonResourceDescriptor.class);
        } catch (Exception e) {
            throw new WebFingerInvalidResponseException("WebFinger response for " + resource + " is not a valid JRD", e);
        }
        return new Fetched(jrd, ttl(response));
    }

    private Duration ttl(FetchResponse response) {
        Optional<String> cc = response.header("Cache-Control");
        if (cc.isEmpty() || cc.get().toLowerCase(Locale.ROOT).contains("no-store")) {
            return Duration.ZERO;
        }
        Matcher m = MAX_AGE.matcher(cc.get());
        if (!m.find()) {
            return Duration.ZERO;
        }
        Duration maxAge = Duration.ofSeconds(Long.parseLong(m.group(1)));
        Duration cap = config.client().cache().maxTtl();
        return maxAge.compareTo(cap) > 0 ? cap : maxAge;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
