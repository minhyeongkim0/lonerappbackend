package com.hiddenloner.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
    boolean productionMode,
    boolean exposeDetailedErrors,
    List<String> corsAllowedOrigins
) {
}
