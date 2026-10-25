package org.sitenetsoft.quarkus.webfinger.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class WebFingerProcessor {

    static final String FEATURE = "webfinger";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
