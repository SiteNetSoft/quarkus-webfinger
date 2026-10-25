package org.sitenetsoft.quarkus.webfinger.server;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sitenetsoft.quarkus.webfinger.JsonResourceDescriptor;
import org.sitenetsoft.quarkus.webfinger.Link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Serves host-meta (RFC 6415) as XRD and JSON with one LRDD link pointing at the WebFinger endpoint.
 */
@ApplicationScoped
public class HostMetaHandler implements BeanHandler.VariantHandler {

    public static final String XRD_CONTENT_TYPE = "application/xrd+xml";
    public static final String LRDD_REL = "lrdd";

    private final ObjectMapper mapper;

    @Inject
    public HostMetaHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Handler<RoutingContext> variant(String variant) {
        return "json".equals(variant) ? this::json : this::xrd;
    }

    static String template(RoutingContext ctx) {
        return "https://" + ctx.request().host() + "/.well-known/webfinger?resource={uri}";
    }

    void xrd(RoutingContext ctx) {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<XRD xmlns=\"http://docs.oasis-open.org/ns/xri/xrd-1.0\">\n"
                + "  <Link rel=\"lrdd\" template=\"" + escape(template(ctx)) + "\" type=\"application/jrd+json\"/>\n"
                + "</XRD>\n";
        ctx.response().putHeader("Content-Type", XRD_CONTENT_TYPE + "; charset=utf-8").end(body);
    }

    void json(RoutingContext ctx) {
        JsonResourceDescriptor jrd = JsonResourceDescriptor.of(URI.create("https://" + ctx.request().host() + "/"))
                .withLink(Link.template(LRDD_REL, template(ctx)).withType(WebFingerHandler.JRD_CONTENT_TYPE));
        try {
            ctx.response().putHeader("Content-Type", "application/json").end(mapper.writeValueAsString(jrd));
        } catch (JsonProcessingException e) {
            ctx.response().setStatusCode(500).end();
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }
}
