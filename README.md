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
