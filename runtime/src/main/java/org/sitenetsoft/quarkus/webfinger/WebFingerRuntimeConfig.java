package org.sitenetsoft.quarkus.webfinger;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.webfinger")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebFingerRuntimeConfig {

    /**
     * Server settings.
     */
    Server server();

    /**
     * Built-in OpenID Connect resolver.
     */
    Oidc oidc();

    /**
     * Client settings.
     */
    Client client();

    interface Server {
        /**
         * Send {@code Access-Control-Allow-Origin: *} on WebFinger responses.
         */
        @WithDefault("true")
        boolean cors();
    }

    interface Oidc {
        /**
         * Issuer URL advertised by the built-in OIDC resolver.
         */
        Optional<URI> issuer();

        /**
         * Domains whose {@code acct:} addresses the built-in OIDC resolver answers.
         */
        Optional<List<String>> domains();
    }

    interface Client {
        /**
         * Per-request timeout.
         */
        @WithDefault("10s")
        Duration timeout();

        /**
         * Larger responses are rejected.
         */
        @WithDefault("64K")
        MemorySize maxResponseSize();

        /**
         * On 404, fetch host-meta and follow its LRDD template once.
         */
        @WithDefault("true")
        boolean hostMetaFallback();

        /**
         * Client result cache.
         */
        Cache cache();

        /**
         * Internal: replaces scheme and authority of every client request (tests only).
         */
        Optional<String> baseUrlOverride();

        interface Cache {
            /**
             * Result cache size.
             */
            @WithDefault("1000")
            int maxEntries();

            /**
             * Upper bound on how long a result is cached, whatever {@code Cache-Control} says.
             */
            @WithDefault("1h")
            Duration maxTtl();
        }
    }
}
