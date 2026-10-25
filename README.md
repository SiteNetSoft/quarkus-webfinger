# quarkus-webfinger

Serve and query [WebFinger (RFC 7033)](https://www.rfc-editor.org/rfc/rfc7033) and
[host-meta (RFC 6415)](https://www.rfc-editor.org/rfc/rfc6415) from Quarkus. The app supplies resource data through
`WebFingerResolver` beans; the extension handles the endpoint, the wire format, merging, caching and safe fetching.
It knows resources and links, not actors: ActivityPub, OpenID Connect and others add meaning through resolvers.

Minimum supported Quarkus: **3.40 (LTS)**. Status: experimental, version 0.1.

```xml
<dependency>
  <groupId>org.sitenetsoft.quarkus</groupId>
  <artifactId>quarkus-webfinger</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `quarkus.webfinger.server.enabled` | `true` | Serve the endpoint when resolvers exist |
| `quarkus.webfinger.server.cors` | `true` | Send `Access-Control-Allow-Origin: *` |
| `quarkus.webfinger.server.host-meta.enabled` | `false` | Also serve host-meta (XRD and JSON) |
| `quarkus.webfinger.oidc.enabled` | `false` | Built-in OIDC issuer resolver |
| `quarkus.webfinger.oidc.issuer` | none | Issuer URL to advertise |
| `quarkus.webfinger.oidc.domains` | none | Domains whose `acct:` addresses it answers |
| `quarkus.webfinger.client.timeout` | `10s` | Per-request timeout |
| `quarkus.webfinger.client.max-response-size` | `64K` | Larger responses are rejected |
| `quarkus.webfinger.client.host-meta-fallback` | `true` | Try LRDD on `404` |
| `quarkus.webfinger.client.cache.max-entries` | `1000` | Result cache size |
| `quarkus.webfinger.client.cache.max-ttl` | `1h` | Upper bound on cached results |

## Dev UI

The card lists registered resolvers, previews the server's answer for any resource and runs live client lookups.

## Building

Depends on `quarkus-safe-fetch` (install it first). Then `JAVA_HOME=/path/to/jdk21 mvn -T 8 install`.
