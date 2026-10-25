package org.sitenetsoft.quarkus.webfinger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A JSON Resource Descriptor (RFC 7033 section 4.4).
 */
public record JsonResourceDescriptor(URI subject, List<URI> aliases, Map<URI, String> properties, List<Link> links) {

    public JsonResourceDescriptor {
        if (subject == null) {
            throw new IllegalArgumentException("JRD needs a subject");
        }
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        properties = Link.nullableCopy(properties);
        links = links == null ? List.of() : List.copyOf(links);
    }

    public static JsonResourceDescriptor of(URI subject) {
        return new JsonResourceDescriptor(subject, List.of(), Map.of(), List.of());
    }

    public JsonResourceDescriptor withLink(Link link) {
        List<Link> l = new ArrayList<>(links);
        l.add(link);
        return new JsonResourceDescriptor(subject, aliases, properties, l);
    }

    public JsonResourceDescriptor withAlias(URI alias) {
        List<URI> a = new ArrayList<>(aliases);
        a.add(alias);
        return new JsonResourceDescriptor(subject, a, properties, links);
    }

    public JsonResourceDescriptor withProperty(URI name, String value) {
        Map<URI, String> p = new LinkedHashMap<>(properties);
        p.put(name, value);
        return new JsonResourceDescriptor(subject, aliases, p, links);
    }

    /**
     * Keeps only links whose {@code rel} is in {@code rels}. An empty set keeps every link.
     */
    public JsonResourceDescriptor filterRels(Set<String> rels) {
        if (rels == null || rels.isEmpty()) {
            return this;
        }
        return new JsonResourceDescriptor(subject, aliases, properties,
                links.stream().filter(l -> rels.contains(l.rel())).toList());
    }

    /**
     * Merges a lower-priority descriptor for the same subject into this one: aliases are concatenated without
     * duplicates, links are concatenated, and on a property clash this descriptor's value wins.
     */
    public JsonResourceDescriptor merge(JsonResourceDescriptor lowerPriority) {
        Set<URI> a = new LinkedHashSet<>(aliases);
        a.addAll(lowerPriority.aliases());
        Map<URI, String> p = new LinkedHashMap<>(lowerPriority.properties());
        p.putAll(properties);
        List<Link> l = new ArrayList<>(links);
        l.addAll(lowerPriority.links());
        return new JsonResourceDescriptor(subject, new ArrayList<>(a), p, l);
    }
}
