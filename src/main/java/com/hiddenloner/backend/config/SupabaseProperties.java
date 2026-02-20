package com.hiddenloner.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
    String url,
    String anonKey,
    String usernameEmailDomain,
    String storageBucket,
    String serviceRoleKey
) {
}
