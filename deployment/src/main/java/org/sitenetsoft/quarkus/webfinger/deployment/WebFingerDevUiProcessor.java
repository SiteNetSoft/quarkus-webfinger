package org.sitenetsoft.quarkus.webfinger.deployment;

import org.sitenetsoft.quarkus.webfinger.devui.WebFingerJsonRpcService;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class WebFingerDevUiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem card() {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.webComponentPageBuilder()
                .title("Lookup")
                .icon("font-awesome-solid:magnifying-glass")
                .componentLink("qwc-webfinger-lookup.js"));
        card.addPage(Page.externalPageBuilder("RFC 7033")
                .url("https://www.rfc-editor.org/rfc/rfc7033")
                .icon("font-awesome-solid:book")
                .doNotEmbed());
        return card;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem jsonRpc() {
        return new JsonRPCProvidersBuildItem(WebFingerJsonRpcService.class);
    }
}
