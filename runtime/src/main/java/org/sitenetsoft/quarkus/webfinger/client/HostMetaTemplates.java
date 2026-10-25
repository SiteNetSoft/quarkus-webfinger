package org.sitenetsoft.quarkus.webfinger.client;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.Link;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Remembers, per host, the LRDD template from host-meta, or the fact that the host has none.
 */
final class HostMetaTemplates {

    private static final Pattern LINK = Pattern.compile("<Link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern REL_LRDD = Pattern.compile("\\brel\\s*=\\s*\"lrdd\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPLATE = Pattern.compile("\\btemplate\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private final Cache<String, Optional<String>> cache;

    HostMetaTemplates(int maxEntries, java.time.Duration ttl) {
        this.cache = Caffeine.newBuilder().maximumSize(maxEntries).expireAfterWrite(ttl).build();
    }

    Optional<Optional<String>> get(String host) {
        return Optional.ofNullable(cache.getIfPresent(host));
    }

    void put(String host, Optional<String> template) {
        cache.put(host, template);
    }

    /**
     * Extracts the {@code lrdd} template from an XRD document.
     */
    static Optional<String> fromXrd(String xml) {
        Matcher links = LINK.matcher(xml);
        while (links.find()) {
            String link = links.group();
            if (REL_LRDD.matcher(link).find()) {
                Matcher t = TEMPLATE.matcher(link);
                if (t.find()) {
                    return Optional.of(t.group(1).replace("&amp;", "&").replace("&quot;", "\"").replace("&lt;", "<"));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts the {@code lrdd} template from a JSON host-meta document.
     */
    static Optional<String> fromJrd(JsonResourceDescriptor jrd) {
        return jrd.links().stream().filter(l -> "lrdd".equalsIgnoreCase(l.rel())).map(Link::template)
                .filter(t -> t != null && !t.isBlank()).findFirst();
    }
}
