package com.hiddenloner.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dev")
public record AppDevProperties(
    boolean bootstrapEnabled
) {
}
