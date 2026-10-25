package org.sitenetsoft.quarkus.webfinger.devui;

import java.net.URI;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.sitenetsoft.quarkus.webfinger.ReactiveWebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.WebFingerRequest;
import org.sitenetsoft.quarkus.webfinger.WebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.client.WebFingerClientImpl;
import org.sitenetsoft.quarkus.webfinger.server.WebFingerHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.arc.All;
import io.smallrye.mutiny.Uni;

/**
 * Dev UI backend: lists resolvers, previews the server's answer for a resource, and runs live client lookups.
 */
public class WebFingerJsonRpcService {

    @Inject
    @All
    java.util.List<WebFingerResolver> resolvers;

    @Inject
    @All
    java.util.List<ReactiveWebFingerResolver> reactiveResolvers;

    @Inject
    Instance<WebFingerHandler> handler;

    @Inject
    WebFingerClientImpl client;

    @Inject
    ObjectMapper mapper;

    public ArrayNode resolvers() {
        ArrayNode list = mapper.createArrayNode();
        resolvers.forEach(r -> list.add(beanName(r)));
        reactiveResolvers.forEach(r -> list.add(beanName(r) + " (reactive)"));
        return list;
    }

    public Uni<JsonNode> preview(String resource) {
        URI uri = WebFingerHandler.parseResource(resource);
        if (uri == null) {
            return Uni.createFrom().item(error("invalid resource"));
        }
        if (!handler.isResolvable()) {
            return Uni.createFrom().item(error("endpoint not registered (no resolver beans)"));
        }
        return handler.get().resolveAll(new WebFingerRequest(uri, Set.of(), "localhost"))
                .map(r -> r.map(jrd -> (JsonNode) mapper.valueToTree(jrd)).orElseGet(() -> error("404: no resolver answered")));
    }

    public Uni<JsonNode> lookup(String resource) {
        URI uri = WebFingerHandler.parseResource(resource);
        if (uri == null) {
            return Uni.createFrom().item(error("invalid resource"));
        }
        return client.lookup(uri, Set.of()).map(jrd -> (JsonNode) mapper.valueToTree(jrd))
                .onFailure().recoverWithItem(t -> error(t.getClass().getSimpleName() + ": " + t.getMessage()));
    }

    private JsonNode error(String message) {
        ObjectNode n = mapper.createObjectNode();
        n.put("error", message);
        return n;
    }

    private static String beanName(Object bean) {
        Class<?> c = bean.getClass();
        return c.getName().contains("_ClientProxy") || c.getName().contains("_Subclass") ? c.getSuperclass().getName() : c.getName();
    }
}
