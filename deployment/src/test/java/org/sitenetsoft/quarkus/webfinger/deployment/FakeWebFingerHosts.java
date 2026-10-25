package org.sitenetsoft.quarkus.webfinger.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

/**
 * One loopback server playing several WebFinger hosts, selected by the {@code Host} header:
 * <ul>
 * <li>{@code direct.test}: serves WebFinger directly with {@code Cache-Control: max-age=60}</li>
 * <li>{@code lrdd.test}: no WebFinger, host-meta XRD with an LRDD template to {@code /finger?r={uri}}</li>
 * <li>{@code error.test}: 500 on everything</li>
 * <li>{@code down.test}: closes the connection</li>
 * </ul>
 */
public final class FakeWebFingerHosts {

    private final Vertx vertx = Vertx.vertx();
    private final HttpServer server;
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();

    public FakeWebFingerHosts() {
        server = vertx.createHttpServer().requestHandler(this::handle);
        server.listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture().join();
    }

    public int port() {
        return server.actualPort();
    }

    public int hits(String hostAndPath) {
        return hits.getOrDefault(hostAndPath, new AtomicInteger()).get();
    }

    public void stop() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    private void handle(HttpServerRequest req) {
        String host = req.getHeader("Host");
        host = host == null ? "" : host.split(":")[0];
        hits.computeIfAbsent(host + req.path(), k -> new AtomicInteger()).incrementAndGet();
        String resource = req.getParam("resource");
        switch (host) {
            case "direct.test" -> {
                if (req.path().equals("/.well-known/webfinger") && "acct:alice@direct.test".equals(resource)) {
                    req.response().putHeader("Content-Type", "application/jrd+json").putHeader("Cache-Control", "max-age=60")
                            .end("{\"subject\":\"acct:alice@direct.test\",\"links\":[{\"rel\":\"self\",\"href\":\"https://direct.test/users/alice\"},"
                                    + "{\"rel\":\"http://webfinger.net/rel/profile-page\",\"href\":\"https://direct.test/@alice\"}]}");
                } else if (req.path().equals("/.well-known/webfinger") && "acct:bad@direct.test".equals(resource)) {
                    req.response().putHeader("Content-Type", "application/jrd+json").end("{not json");
                } else if (req.path().equals("/.well-known/webfinger") && "acct:nosubject@direct.test".equals(resource)) {
                    req.response().putHeader("Content-Type", "application/jrd+json").end("{\"links\":[]}");
                } else {
                    req.response().setStatusCode(404).end();
                }
            }
            case "lrdd.test" -> {
                switch (req.path()) {
                    case "/.well-known/host-meta" -> req.response().putHeader("Content-Type", "application/xrd+xml")
                            .end("<?xml version=\"1.0\"?><XRD xmlns=\"http://docs.oasis-open.org/ns/xri/xrd-1.0\">"
                                    + "<Link rel=\"lrdd\" type=\"application/jrd+json\" template=\"https://lrdd.test/finger?r={uri}\"/></XRD>");
                    case "/finger" -> {
                        if ("acct:bob@lrdd.test".equals(req.getParam("r"))) {
                            req.response().putHeader("Content-Type", "application/jrd+json")
                                    .end("{\"subject\":\"acct:bob@lrdd.test\",\"links\":[{\"rel\":\"self\",\"href\":\"https://lrdd.test/users/bob\"}]}");
                        } else {
                            req.response().setStatusCode(404).end();
                        }
                    }
                    default -> req.response().setStatusCode(404).end();
                }
            }
            case "error.test" -> req.response().setStatusCode(500).end("boom");
            case "down.test" -> req.connection().close();
            default -> req.response().setStatusCode(404).end();
        }
    }
}
