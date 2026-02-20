package com.hiddenloner.backend.config;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class AppStartupValidator {

    private final AppSecurityProperties appSecurityProperties;
    private final SupabaseProperties supabaseProperties;

    public AppStartupValidator(AppSecurityProperties appSecurityProperties, SupabaseProperties supabaseProperties) {
        this.appSecurityProperties = appSecurityProperties;
        this.supabaseProperties = supabaseProperties;
    }

    @PostConstruct
    void validate() {
        if (!appSecurityProperties.productionMode()) {
            return;
        }

        if (appSecurityProperties.corsAllowedOrigins() == null || appSecurityProperties.corsAllowedOrigins().isEmpty()) {
            throw new IllegalStateException("Production mode requires APP_CORS_ALLOWED_ORIGINS.");
        }

        if (supabaseProperties.serviceRoleKey() == null || supabaseProperties.serviceRoleKey().isBlank()) {
            throw new IllegalStateException("Production mode requires SUPABASE_SERVICE_ROLE_KEY.");
        }
    }
}
