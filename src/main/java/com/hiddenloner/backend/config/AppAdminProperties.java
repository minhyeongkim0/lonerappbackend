package com.hiddenloner.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AppAdminProperties(
    List<String> usernames
) {
}
