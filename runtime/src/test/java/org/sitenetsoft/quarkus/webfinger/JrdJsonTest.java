package org.sitenetsoft.quarkus.webfinger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JrdJsonTest {

    static final String RFC7033_3_1 = """
            {
              "subject" : "acct:carol@example.com",
              "links" :
              [
                {
                  "rel" : "http://openid.net/specs/connect/1.0/issuer",
                  "href" : "https://openid.example.com"
                }
              ]
            }
            """;

    static final String RFC7033_3_2 = """
            {
              "subject" : "http://blog.example.com/article/id/314",
              "aliases" :
              [
                "http://blog.example.com/cool_new_thing",
                "http://blog.example.com/steve/article/7"
              ],
              "properties" :
              {
                "http://blgx.example.net/ns/version" : "1.3",
                "http://blgx.example.net/ns/ext" : null
              },
              "links" :
              [
                {
                  "rel" : "copyright",
                  "href" : "http://www.example.com/copyright"
                },
                {
                  "rel" : "author",
                  "href" : "http://blog.example.com/author/steve",
                  "titles" :
                  {
                    "en-us" : "The Magical World of Steve",
                    "fr" : "Le Monde Magique de Steve"
                  },
                  "properties" :
                  {
                    "http://example.com/role/editor" : null
                  }
                }
              ]
            }
            """;

    final ObjectMapper mapper = new ObjectMapper().registerModule(new WebFingerJacksonModule());

    @Test
    void rfc7033Section3Example1RoundTrips() throws Exception {
        JsonResourceDescriptor jrd = mapper.readValue(RFC7033_3_1, JsonResourceDescriptor.class);
        assertEquals(URI.create("acct:carol@example.com"), jrd.subject());
        assertEquals(1, jrd.links().size());
        assertEquals("http://openid.net/specs/connect/1.0/issuer", jrd.links().get(0).rel());
        assertEquals(URI.create("https://openid.example.com"), jrd.links().get(0).href());

        JsonNode written = mapper.readTree(mapper.writeValueAsString(jrd));
        assertEquals(mapper.readTree(RFC7033_3_1), written);
    }

    @Test
    void rfc7033Section3Example2RoundTrips() throws Exception {
        JsonResourceDescriptor jrd = mapper.readValue(RFC7033_3_2, JsonResourceDescriptor.class);
        assertEquals(2, jrd.aliases().size());
        assertEquals("1.3", jrd.properties().get(URI.create("http://blgx.example.net/ns/version")));
        assertTrue(jrd.properties().containsKey(URI.create("http://blgx.example.net/ns/ext")));
        assertNull(jrd.properties().get(URI.create("http://blgx.example.net/ns/ext")));
        Link author = jrd.links().get(1);
        assertEquals("Le Monde Magique de Steve", author.titles().get("fr"));
        assertTrue(author.properties().containsKey(URI.create("http://example.com/role/editor")));

        JsonNode written = mapper.readTree(mapper.writeValueAsString(jrd));
        assertEquals(mapper.readTree(RFC7033_3_2), written);
        assertEquals(jrd, mapper.readValue(mapper.writeValueAsString(jrd), JsonResourceDescriptor.class));
    }

    @Test
    void omitsEmptyFieldsExceptLinks() throws Exception {
        String json = mapper.writeValueAsString(JsonResourceDescriptor.of(URI.create("acct:a@b.example")));
        assertEquals("{\"subject\":\"acct:a@b.example\",\"links\":[]}", json);
    }

    @Test
    void ignoresUnknownFields() throws Exception {
        JsonResourceDescriptor jrd = mapper.readValue("{\"subject\":\"acct:a@b.example\",\"extra\":1,\"links\":[{\"rel\":\"x\",\"foo\":2}]}",
                JsonResourceDescriptor.class);
        assertEquals("x", jrd.links().get(0).rel());
    }

    @Test
    void keepsTemplateLinks() throws Exception {
        String json = "{\"subject\":\"acct:a@b.example\",\"links\":[{\"rel\":\"lrdd\",\"template\":\"https://b.example/wf?r={uri}\"}]}";
        JsonResourceDescriptor jrd = mapper.readValue(json, JsonResourceDescriptor.class);
        assertEquals("https://b.example/wf?r={uri}", jrd.links().get(0).template());
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(jrd)));
    }

    @Test
    void filterRelsKeepsMatching() {
        JsonResourceDescriptor jrd = JsonResourceDescriptor.of(URI.create("acct:a@b.example"))
                .withLink(Link.of("self", URI.create("https://b.example/a")))
                .withLink(Link.of("http://webfinger.net/rel/profile-page", URI.create("https://b.example/@a")));
        assertEquals(List.of("self"), jrd.filterRels(Set.of("self")).links().stream().map(Link::rel).toList());
        assertEquals(2, jrd.filterRels(Set.of()).links().size());
        assertEquals(0, jrd.filterRels(Set.of("nope")).links().size());
    }

    @Test
    void mergeConcatenatesAndPrefersHigherPriorityProperties() {
        URI p = URI.create("http://example.com/p");
        JsonResourceDescriptor high = new JsonResourceDescriptor(URI.create("acct:a@b.example"),
                List.of(URI.create("https://b.example/@a")), Map.of(p, "high"),
                List.of(Link.of("http://openid.net/specs/connect/1.0/issuer", URI.create("https://id.example"))));
        JsonResourceDescriptor low = new JsonResourceDescriptor(URI.create("acct:a@b.example"),
                List.of(URI.create("https://b.example/@a"), URI.create("https://b.example/users/a")), Map.of(p, "low"),
                List.of(Link.of("self", URI.create("https://b.example/users/a"))));
        JsonResourceDescriptor merged = high.merge(low);
        assertEquals(URI.create("acct:a@b.example"), merged.subject());
        assertEquals(List.of(URI.create("https://b.example/@a"), URI.create("https://b.example/users/a")), merged.aliases());
        assertEquals("high", merged.properties().get(p));
        assertEquals(List.of("http://openid.net/specs/connect/1.0/issuer", "self"), merged.links().stream().map(Link::rel).toList());
        assertFalse(merged.links().isEmpty());
    }
}
