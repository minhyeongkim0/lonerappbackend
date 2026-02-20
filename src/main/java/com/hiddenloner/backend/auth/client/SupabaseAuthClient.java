package com.hiddenloner.backend.auth.client;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.common.error.ExternalErrorMessageResolver;
import com.hiddenloner.backend.config.SupabaseProperties;

@Component
public class SupabaseAuthClient {

    private final RestClient restClient;
    private final SupabaseProperties properties;
    private final ExternalErrorMessageResolver errorMessageResolver;

    public SupabaseAuthClient(SupabaseProperties properties, ExternalErrorMessageResolver errorMessageResolver) {
        this.properties = properties;
        this.errorMessageResolver = errorMessageResolver;
        this.restClient = RestClient.builder()
            .baseUrl(properties.url())
            .defaultHeader("apikey", properties.anonKey())
            .build();
    }

    public UUID signUp(String email, String password) {
        try {
            Map<String, Object> body = restClient.post()
                .uri("/auth/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(Map.class);

            return extractUserId(body);
        } catch (HttpStatusCodeException ex) {
            throw mapSupabaseException(ex, "회원가입 처리 중 오류가 발생했습니다.");
        }
    }

    public TokenResult login(String email, String password) {
        try {
            Map<String, Object> body = restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/auth/v1/token").queryParam("grant_type", "password").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(Map.class);

            if (body == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }

            String accessToken = (String) body.get("access_token");
            String refreshToken = (String) body.get("refresh_token");
            Number expiresIn = (Number) body.get("expires_in");
            UUID userId = extractUserId(body);

            if (accessToken == null || refreshToken == null || userId == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }

            return new TokenResult(userId, accessToken, refreshToken, expiresIn == null ? 0L : expiresIn.longValue());
        } catch (HttpStatusCodeException ex) {
            throw mapSupabaseException(ex, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public UUID getUserIdFromAccessToken(String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                .uri("/auth/v1/user")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

            if (body == null || body.get("id") == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
            }

            return UUID.fromString(body.get("id").toString());
        } catch (HttpStatusCodeException ex) {
            throw mapSupabaseException(ex, "유효하지 않은 토큰입니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId(Map<String, Object> body) {
        if (body == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SUPABASE_RESPONSE_ERROR", "Supabase 응답이 비어있습니다.");
        }

        Object directId = body.get("id");
        if (directId != null) {
            return UUID.fromString(directId.toString());
        }

        Object userObj = body.get("user");
        if (userObj instanceof Map<?, ?> userMap) {
            Object idObj = ((Map<String, Object>) userMap).get("id");
            if (idObj != null) {
                return UUID.fromString(idObj.toString());
            }
        }

        throw new ApiException(HttpStatus.BAD_GATEWAY, "SUPABASE_RESPONSE_ERROR", "Supabase 사용자 ID를 확인할 수 없습니다.");
    }

    private ApiException mapSupabaseException(HttpStatusCodeException ex, String defaultMessage) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String responseBody = ex.getResponseBodyAsString();
        String message = errorMessageResolver.resolve(defaultMessage, responseBody);

        return new ApiException(status, "SUPABASE_AUTH_ERROR", message);
    }

    public record TokenResult(UUID userId, String accessToken, String refreshToken, long expiresIn) {
    }
}
