package org.sitenetsoft.quarkus.webfinger.deployment;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.webfinger.ReactiveWebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.WebFingerBuildTimeConfig;
import org.sitenetsoft.quarkus.webfinger.WebFingerObjectMapperCustomizer;
import org.sitenetsoft.quarkus.webfinger.WebFingerRecorder;
import org.sitenetsoft.quarkus.webfinger.WebFingerResolver;
import org.sitenetsoft.quarkus.webfinger.oidc.OidcIssuerResolver;
import org.sitenetsoft.quarkus.webfinger.server.HostMetaHandler;
import org.sitenetsoft.quarkus.webfinger.server.WebFingerHandler;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

class WebFingerProcessor {

    static final String FEATURE = "webfinger";
    static final String WEBFINGER_PATH = "/.well-known/webfinger";
    static final String HOST_META_PATH = "/.well-known/host-meta";
    static final String HOST_META_JSON_PATH = "/.well-known/host-meta.json";

    private static final Logger LOG = Logger.getLogger("org.sitenetsoft.quarkus.webfinger");
    private static final DotName RESOLVER = DotName.createSimple(WebFingerResolver.class);
    private static final DotName REACTIVE_RESOLVER = DotName.createSimple(ReactiveWebFingerResolver.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(WebFingerHandler.class, HostMetaHandler.class, WebFingerObjectMapperCustomizer.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    void oidcResolver(WebFingerBuildTimeConfig config, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (config.oidc().enabled()) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(OidcIssuerResolver.class));
        }
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableResolvers() {
        return UnremovableBeanBuildItem.beanTypes(WebFingerResolver.class, ReactiveWebFingerResolver.class);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerRoutes(WebFingerBuildTimeConfig config, CombinedIndexBuildItem index, HttpRootPathBuildItem rootPath,
            WebFingerRecorder recorder, BuildProducer<RouteBuildItem> routes) {
        if (!config.server().enabled()) {
            LOG.debug("WebFinger endpoint not registered: quarkus.webfinger.server.enabled=false");
            return;
        }
        Collection<ClassInfo> resolvers = index.getIndex().getAllKnownImplementors(RESOLVER);
        Collection<ClassInfo> reactive = index.getIndex().getAllKnownImplementors(REACTIVE_RESOLVER);
        boolean oidc = config.oidc().enabled();
        if (resolvers.isEmpty() && reactive.isEmpty() && !oidc) {
            LOG.info("WebFinger endpoint not registered: no WebFingerResolver or ReactiveWebFingerResolver bean found "
                    + "(and quarkus.webfinger.oidc.enabled=false)");
            return;
        }
        if (!"/".equals(rootPath.getRootPath())) {
            LOG.warnf("quarkus.http.root-path is '%s'; WebFinger still serves %s at the server root because "
                    + "well-known paths must sit at the domain root", rootPath.getRootPath(), WEBFINGER_PATH);
        }
        LOG.debugf("WebFinger resolvers: %s %s", resolvers, reactive);
        routes.produce(RouteBuildItem.builder().route(WEBFINGER_PATH).handler(recorder.webfingerHandler())
                .displayOnNotFoundPage("WebFinger").build());
        if (config.server().hostMeta().enabled()) {
            routes.produce(RouteBuildItem.builder().route(HOST_META_PATH).handler(recorder.hostMetaXrdHandler()).build());
            routes.produce(RouteBuildItem.builder().route(HOST_META_JSON_PATH).handler(recorder.hostMetaJsonHandler()).build());
        }
    }
}
