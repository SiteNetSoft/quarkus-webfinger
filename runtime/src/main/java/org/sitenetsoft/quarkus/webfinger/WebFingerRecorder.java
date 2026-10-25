package org.sitenetsoft.quarkus.webfinger;

import org.sitenetsoft.quarkus.webfinger.server.BeanHandler;
import org.sitenetsoft.quarkus.webfinger.server.HostMetaHandler;
import org.sitenetsoft.quarkus.webfinger.server.WebFingerHandler;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebFingerRecorder {

    public Handler<RoutingContext> webfingerHandler() {
        return new BeanHandler(WebFingerHandler.class.getName(), null);
    }

    public Handler<RoutingContext> hostMetaXrdHandler() {
        return new BeanHandler(HostMetaHandler.class.getName(), "xrd");
    }

    public Handler<RoutingContext> hostMetaJsonHandler() {
        return new BeanHandler(HostMetaHandler.class.getName(), "json");
    }
}
