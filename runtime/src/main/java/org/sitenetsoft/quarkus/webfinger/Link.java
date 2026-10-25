package org.sitenetsoft.quarkus.webfinger;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A JRD link (RFC 7033 section 4.4.4). {@code template} is not in RFC 7033 but appears in host-meta LRDD links and
 * remote-follow links, so it is kept. Property values may be {@code null}, as the RFC allows.
 */
public record Link(String rel, String type, URI href, String template, Map<String, String> titles,
        Map<URI, String> properties) {

    public Link {
        if (rel == null || rel.isEmpty()) {
            throw new IllegalArgumentException("link needs a rel");
        }
        titles = nullableCopy(titles);
        properties = nullableCopy(properties);
    }

    static <K, V> Map<K, V> nullableCopy(Map<K, V> map) {
        return map == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    public static Link of(String rel, URI href) {
        return new Link(rel, null, href, null, Map.of(), Map.of());
    }

    public static Link of(String rel, String type, URI href) {
        return new Link(rel, type, href, null, Map.of(), Map.of());
    }

    public static Link template(String rel, String template) {
        return new Link(rel, null, null, template, Map.of(), Map.of());
    }

    public Link withType(String type) {
        return new Link(rel, type, href, template, titles, properties);
    }

    public Link withTitle(String language, String title) {
        Map<String, String> t = new LinkedHashMap<>(titles);
        t.put(language, title);
        return new Link(rel, type, href, template, t, properties);
    }

    public Link withProperty(URI name, String value) {
        Map<URI, String> p = new LinkedHashMap<>(properties);
        p.put(name, value);
        return new Link(rel, type, href, template, titles, p);
    }
}
