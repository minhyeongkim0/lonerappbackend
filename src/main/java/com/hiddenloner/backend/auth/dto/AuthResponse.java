package com.hiddenloner.backend.auth.dto;

import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String username,
    String accessToken,
    String refreshToken,
    long expiresIn
) {
}
