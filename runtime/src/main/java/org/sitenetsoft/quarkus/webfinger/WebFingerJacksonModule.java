package org.sitenetsoft.quarkus.webfinger;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson support for the JRD wire format. Null and empty members are omitted on output, except {@code links}
 * (always present) and property values, which RFC 7033 allows to be {@code null}. Unknown members are ignored on
 * input.
 */
public class WebFingerJacksonModule extends SimpleModule {

    public WebFingerJacksonModule() {
        super("webfinger");
        addSerializer(JsonResourceDescriptor.class, new JrdSerializer());
        addDeserializer(JsonResourceDescriptor.class, new JrdDeserializer());
        addSerializer(Link.class, new LinkSerializer());
        addDeserializer(Link.class, new LinkDeserializer());
    }

    static final class JrdSerializer extends StdSerializer<JsonResourceDescriptor> {
        JrdSerializer() {
            super(JsonResourceDescriptor.class);
        }

        @Override
        public void serialize(JsonResourceDescriptor jrd, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("subject", jrd.subject().toString());
            if (!jrd.aliases().isEmpty()) {
                gen.writeArrayFieldStart("aliases");
                for (URI alias : jrd.aliases()) {
                    gen.writeString(alias.toString());
                }
                gen.writeEndArray();
            }
            writeProperties(gen, jrd.properties());
            // "links" is always written, even when empty, so a rel filter that matches nothing yields "links": []
            gen.writeArrayFieldStart("links");
            for (Link link : jrd.links()) {
                provider.defaultSerializeValue(link, gen);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    static final class LinkSerializer extends StdSerializer<Link> {
        LinkSerializer() {
            super(Link.class);
        }

        @Override
        public void serialize(Link link, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("rel", link.rel());
            if (link.type() != null) {
                gen.writeStringField("type", link.type());
            }
            if (link.href() != null) {
                gen.writeStringField("href", link.href().toString());
            }
            if (link.template() != null) {
                gen.writeStringField("template", link.template());
            }
            if (!link.titles().isEmpty()) {
                gen.writeObjectFieldStart("titles");
                for (Map.Entry<String, String> e : link.titles().entrySet()) {
                    gen.writeStringField(e.getKey(), e.getValue());
                }
                gen.writeEndObject();
            }
            writeProperties(gen, link.properties());
            gen.writeEndObject();
        }
    }

    static void writeProperties(JsonGenerator gen, Map<URI, String> properties) throws IOException {
        if (properties.isEmpty()) {
            return;
        }
        gen.writeObjectFieldStart("properties");
        for (Map.Entry<URI, String> e : properties.entrySet()) {
            if (e.getValue() == null) {
                gen.writeNullField(e.getKey().toString());
            } else {
                gen.writeStringField(e.getKey().toString(), e.getValue());
            }
        }
        gen.writeEndObject();
    }

    static final class JrdDeserializer extends StdDeserializer<JsonResourceDescriptor> {
        JrdDeserializer() {
            super(JsonResourceDescriptor.class);
        }

        @Override
        public JsonResourceDescriptor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            JsonNode subject = node.get("subject");
            if (subject == null || !subject.isTextual()) {
                throw ctxt.weirdStringException(null, JsonResourceDescriptor.class, "JRD has no textual 'subject'");
            }
            List<URI> aliases = new ArrayList<>();
            JsonNode aliasNode = node.get("aliases");
            if (aliasNode != null && aliasNode.isArray()) {
                for (JsonNode a : aliasNode) {
                    if (a.isTextual()) {
                        aliases.add(uri(ctxt, a.asText()));
                    }
                }
            }
            List<Link> links = new ArrayList<>();
            JsonNode linkNode = node.get("links");
            if (linkNode != null && linkNode.isArray()) {
                for (JsonNode l : linkNode) {
                    Link link = LinkDeserializer.read(ctxt, l);
                    if (link != null) {
                        links.add(link);
                    }
                }
            }
            return new JsonResourceDescriptor(uri(ctxt, subject.asText()), aliases, readProperties(ctxt, node.get("properties")), links);
        }
    }

    static final class LinkDeserializer extends StdDeserializer<Link> {
        LinkDeserializer() {
            super(Link.class);
        }

        @Override
        public Link deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return read(ctxt, p.getCodec().readTree(p));
        }

        static Link read(DeserializationContext ctxt, JsonNode node) throws IOException {
            if (node == null || !node.isObject()) {
                return null;
            }
            JsonNode rel = node.get("rel");
            if (rel == null || !rel.isTextual() || rel.asText().isEmpty()) {
                return null; // RFC 7033: links without rel are not useful; skip rather than fail
            }
            Map<String, String> titles = new LinkedHashMap<>();
            JsonNode titleNode = node.get("titles");
            if (titleNode != null && titleNode.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = titleNode.fields(); it.hasNext();) {
                    Map.Entry<String, JsonNode> e = it.next();
                    titles.put(e.getKey(), e.getValue().isNull() ? null : e.getValue().asText());
                }
            }
            return new Link(rel.asText(), text(node, "type"),
                    node.hasNonNull("href") ? uri(ctxt, node.get("href").asText()) : null,
                    text(node, "template"), titles, readProperties(ctxt, node.get("properties")));
        }
    }

    static String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n == null || !n.isTextual() ? null : n.asText();
    }

    static Map<URI, String> readProperties(DeserializationContext ctxt, JsonNode node) throws IOException {
        Map<URI, String> props = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> e = it.next();
                props.put(uri(ctxt, e.getKey()), e.getValue().isNull() ? null : e.getValue().asText());
            }
        }
        return props;
    }

    static URI uri(DeserializationContext ctxt, String value) throws IOException {
        try {
            return new URI(value);
        } catch (java.net.URISyntaxException e) {
            throw ctxt.weirdStringException(value, URI.class, "not a valid URI");
        }
    }
}
