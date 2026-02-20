package com.hiddenloner.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppSecurityProperties appSecurityProperties;

    public WebConfig(AppSecurityProperties appSecurityProperties) {
        this.appSecurityProperties = appSecurityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = appSecurityProperties.corsAllowedOrigins() == null
            ? new String[] { "http://localhost:3000", "http://127.0.0.1:3000" }
            : appSecurityProperties.corsAllowedOrigins().stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);

        registry.addMapping("/api/**")
            .allowedOrigins(origins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }
}
