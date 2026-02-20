package com.hiddenloner.backend.common.error;

import org.springframework.stereotype.Component;

import com.hiddenloner.backend.config.AppSecurityProperties;

@Component
public class ExternalErrorMessageResolver {

    private final AppSecurityProperties appSecurityProperties;

    public ExternalErrorMessageResolver(AppSecurityProperties appSecurityProperties) {
        this.appSecurityProperties = appSecurityProperties;
    }

    public String resolve(String defaultMessage, String externalBody) {
        if (appSecurityProperties.exposeDetailedErrors() && externalBody != null && !externalBody.isBlank()) {
            return externalBody;
        }
        return defaultMessage;
    }
}
