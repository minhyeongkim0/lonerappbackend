package com.hiddenloner.backend.upload.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.common.error.ExternalErrorMessageResolver;
import com.hiddenloner.backend.config.SupabaseProperties;

@Component
public class SupabaseStorageClient {

    private final RestClient restClient;
    private final SupabaseProperties properties;
    private final ExternalErrorMessageResolver errorMessageResolver;

    public SupabaseStorageClient(SupabaseProperties properties, ExternalErrorMessageResolver errorMessageResolver) {
        this.properties = properties;
        this.errorMessageResolver = errorMessageResolver;
        this.restClient = RestClient.builder().baseUrl(properties.url()).build();
    }

    public void upload(String objectPath, byte[] content, String contentType, String userAccessToken) {
        String key = hasText(properties.serviceRoleKey()) ? properties.serviceRoleKey() : properties.anonKey();
        String bearer = hasText(properties.serviceRoleKey()) ? properties.serviceRoleKey() : userAccessToken;

        try {
            restClient.post()
                .uri("/storage/v1/object/" + encodeSegment(properties.storageBucket()) + "/" + encodePath(objectPath))
                .header("apikey", key)
                .header("Authorization", "Bearer " + bearer)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content)
                .retrieve()
                .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_GATEWAY;
            }
            String body = ex.getResponseBodyAsString();
            String message = errorMessageResolver.resolve("이미지 업로드에 실패했습니다.", body);
            throw new ApiException(status, "STORAGE_UPLOAD_FAILED", message);
        }
    }

    public String createSignedUrl(String objectPath, int expiresInSeconds) {
        String key = hasText(properties.serviceRoleKey()) ? properties.serviceRoleKey() : properties.anonKey();
        String bearer = hasText(properties.serviceRoleKey()) ? properties.serviceRoleKey() : properties.anonKey();

        try {
            Map<?, ?> response = restClient.post()
                .uri("/storage/v1/object/sign/" + encodeSegment(properties.storageBucket()) + "/" + encodePath(objectPath))
                .header("apikey", key)
                .header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("expiresIn", expiresInSeconds))
                .retrieve()
                .body(Map.class);

            Object signedUrlValue = response == null ? null : response.get("signedURL");
            if (signedUrlValue instanceof String signedUrl && !signedUrl.isBlank()) {
                if (signedUrl.startsWith("http://") || signedUrl.startsWith("https://")) {
                    return signedUrl;
                }
                return properties.url() + "/storage/v1" + signedUrl;
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SIGNED_URL_FAILED", "이미지 URL 생성에 실패했습니다.");
        } catch (HttpStatusCodeException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_GATEWAY;
            }
            String body = ex.getResponseBodyAsString();
            String message = errorMessageResolver.resolve("이미지 URL 생성에 실패했습니다.", body);
            throw new ApiException(status, "SIGNED_URL_FAILED", message);
        }
    }

    private String encodePath(String path) {
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(encodeSegment(parts[i]));
        }
        return sb.toString();
    }

    private String encodeSegment(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
