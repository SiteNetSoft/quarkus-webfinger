package org.sitenetsoft.quarkus.webfinger;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.webfinger")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface WebFingerBuildTimeConfig {

    /**
     * Server settings.
     */
    Server server();

    /**
     * Built-in OpenID Connect resolver.
     */
    Oidc oidc();

    interface Server {
        /**
         * Serve {@code /.well-known/webfinger} when resolver beans exist.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * host-meta settings.
         */
        HostMeta hostMeta();

        interface HostMeta {
            /**
             * Also serve {@code /.well-known/host-meta} (XRD) and {@code /.well-known/host-meta.json}.
             */
            @WithDefault("false")
            boolean enabled();
        }
    }

    interface Oidc {
        /**
         * Enable the built-in OpenID Connect issuer resolver.
         */
        @WithDefault("false")
        boolean enabled();
    }
}
