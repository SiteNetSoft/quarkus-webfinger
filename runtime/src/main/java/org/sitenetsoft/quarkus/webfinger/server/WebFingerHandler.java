package org.sitenetsoft.quarkus.webfinger.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.ReactiveWebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.WebFingerRequest;
import org.sitenetsoft.quarkus.webfinger.WebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.WebFingerRuntimeConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Serves {@code GET /.well-known/webfinger}: runs every resolver (highest priority first), merges the answers for
 * the subject, applies the {@code rel} filter and writes the JRD.
 */
@ApplicationScoped
public class WebFingerHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger("org.sitenetsoft.quarkus.webfinger");
    public static final String JRD_CONTENT_TYPE = "application/jrd+json";

    private final List<WebFingerResolver> resolvers;
    private final List<ReactiveWebFingerResolver> reactiveResolvers;
    private final ObjectMapper mapper;
    private final WebFingerRuntimeConfig config;
    private final Vertx vertx;

    @Inject
    public WebFingerHandler(@All List<WebFingerResolver> resolvers, @All List<ReactiveWebFingerResolver> reactiveResolvers,
            ObjectMapper mapper, WebFingerRuntimeConfig config, Vertx vertx) {
        this.resolvers = resolvers;
        this.reactiveResolvers = reactiveResolvers;
        this.mapper = mapper;
        this.config = config;
        this.vertx = vertx;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String resourceParam = ctx.request().getParam("resource");
        URI resource = parseResource(resourceParam);
        if (resource == null) {
            ctx.response().setStatusCode(400).putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"invalid_resource\"}");
            return;
        }
        Set<String> rels = new LinkedHashSet<>(ctx.request().params().getAll("rel"));
        WebFingerRequest request = new WebFingerRequest(resource, rels, ctx.request().host());
        resolveAll(request).subscribe().with(result -> write(ctx, result, rels), failure -> {
            LOG.errorf(failure, "WebFinger resolution failed for %s", resource);
            ctx.response().setStatusCode(500).end();
        });
    }

    public static URI parseResource(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value.strip());
            return uri.getScheme() == null ? null : uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Runs every resolver in priority order and merges non-empty answers.
     */
    public Uni<Optional<JsonResourceDescriptor>> resolveAll(WebFingerRequest request) {
        List<Uni<Optional<JsonResourceDescriptor>>> calls = new ArrayList<>();
        for (WebFingerResolver r : resolvers) {
            calls.add(Uni.createFrom().completionStage(() -> vertx.executeBlocking(() -> r.resolve(request), false).toCompletionStage()));
        }
        for (ReactiveWebFingerResolver r : reactiveResolvers) {
            calls.add(Uni.createFrom().deferred(() -> r.resolve(request)));
        }
        if (calls.isEmpty()) {
            return Uni.createFrom().item(Optional.empty());
        }
        return Uni.join().all(calls).andCollectFailures().map(WebFingerHandler::merge);
    }

    static Optional<JsonResourceDescriptor> merge(List<Optional<JsonResourceDescriptor>> answers) {
        JsonResourceDescriptor merged = null;
        for (Optional<JsonResourceDescriptor> a : answers) {
            if (a == null || a.isEmpty()) {
                continue;
            }
            merged = merged == null ? a.get() : merged.merge(a.get());
        }
        return Optional.ofNullable(merged);
    }

    private void write(RoutingContext ctx, Optional<JsonResourceDescriptor> result, Set<String> rels) {
        if (result.isEmpty()) {
            ctx.response().setStatusCode(404).end();
            return;
        }
        try {
            String body = mapper.writeValueAsString(result.get().filterRels(rels));
            ctx.response().putHeader("Content-Type", JRD_CONTENT_TYPE);
            if (config.server().cors()) {
                ctx.response().putHeader("Access-Control-Allow-Origin", "*");
            }
            ctx.response().end(body);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize JRD", e);
            ctx.response().setStatusCode(500).end();
        }
    }
}
