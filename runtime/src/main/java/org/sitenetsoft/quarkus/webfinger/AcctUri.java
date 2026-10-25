package org.sitenetsoft.quarkus.webfinger;

import java.net.IDN;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * An {@code acct:} URI (RFC 7565): {@code acct:user@host}.
 */
public record AcctUri(String user, String host) {

    public AcctUri {
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("acct URI needs a user part");
        }
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("acct URI needs a host part");
        }
    }

    /**
     * Parses {@code acct:user@host} or a bare {@code user@host}. The user part is percent-decoded and the host is
     * converted to its ASCII (IDNA) form and lowercased.
     */
    public static AcctUri parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("acct URI is null");
        }
        String s = value.strip();
        if (s.regionMatches(true, 0, "acct:", 0, 5)) {
            s = s.substring(5);
        }
        int at = s.lastIndexOf('@');
        if (at < 0) {
            throw new IllegalArgumentException("acct URI has no '@': " + value);
        }
        String user = URLDecoder.decode(s.substring(0, at).replace("+", "%2B"), StandardCharsets.UTF_8);
        String host;
        try {
            host = IDN.toASCII(s.substring(at + 1)).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("acct URI has an invalid host: " + value, e);
        }
        return new AcctUri(user, host);
    }

    public URI toUri() {
        String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8).replace("+", "%20");
        return URI.create("acct:" + encodedUser + "@" + host);
    }

    @Override
    public String toString() {
        return toUri().toString();
    }
}
