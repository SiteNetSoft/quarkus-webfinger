# quarkus-webfinger — skill notes for coding agents

- To publish data, add a CDI bean implementing `WebFingerResolver` (blocking) or `ReactiveWebFingerResolver` (Mutiny).
  Return `Optional.empty()` for subjects you don't know. Several resolvers may answer the same subject; results merge,
  highest `@Priority` wins on property clashes.
- The endpoint `/.well-known/webfinger` is only registered when at least one resolver bean exists at build time
  (or `quarkus.webfinger.oidc.enabled=true`). Check the build log if it is missing.
- `rel` filtering is applied by the extension after merging; resolvers can ignore `WebFingerRequest.rels()`.
- Use `WebFingerClient` / `ReactiveWebFingerClient` for lookups. Failures are `WebFingerNotFoundException`,
  `WebFingerInvalidResponseException` or `WebFingerUnreachableException`.
- The client goes through `quarkus-safe-fetch`: in tests set `quarkus.safe-fetch.https-only=false` and
  `quarkus.safe-fetch.allow-private-addresses=true`.
