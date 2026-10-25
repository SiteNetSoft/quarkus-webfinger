package org.sitenetsoft.quarkus.webfinger;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * Registers {@link WebFingerJacksonModule} on the Quarkus-managed {@link ObjectMapper}.
 */
@Singleton
public class WebFingerObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.registerModule(new WebFingerJacksonModule());
    }
}
