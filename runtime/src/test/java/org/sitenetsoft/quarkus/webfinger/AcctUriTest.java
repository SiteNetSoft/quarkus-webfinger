package org.sitenetsoft.quarkus.webfinger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;

class AcctUriTest {

    @Test
    void parsesAcctScheme() {
        AcctUri a = AcctUri.parse("acct:alice@example.com");
        assertEquals("alice", a.user());
        assertEquals("example.com", a.host());
        assertEquals(URI.create("acct:alice@example.com"), a.toUri());
    }

    @Test
    void parsesBare() {
        assertEquals(new AcctUri("alice", "example.com"), AcctUri.parse("alice@example.com"));
    }

    @Test
    void decodesPercent() {
        assertEquals("al@ice", AcctUri.parse("acct:al%40ice@example.com").user());
    }

    @Test
    void idnHostToAscii() {
        assertEquals("xn--bcher-kva.example", AcctUri.parse("acct:bob@bücher.example").host());
    }

    @Test
    void hostIsLowercased() {
        assertEquals("example.com", AcctUri.parse("acct:bob@Example.COM").host());
    }

    @Test
    void rejectsMissingAt() {
        assertThrows(IllegalArgumentException.class, () -> AcctUri.parse("acct:alice"));
    }

    @Test
    void rejectsEmptyParts() {
        assertThrows(IllegalArgumentException.class, () -> AcctUri.parse("acct:@example.com"));
        assertThrows(IllegalArgumentException.class, () -> AcctUri.parse("acct:alice@"));
    }

    @Test
    void toUriPercentEncodesUser() {
        assertEquals("acct:al%40ice@example.com", new AcctUri("al@ice", "example.com").toUri().toString());
    }
}
