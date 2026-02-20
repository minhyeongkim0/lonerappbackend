package com.hiddenloner.backend.auth.dto;

import java.util.UUID;

public record MeResponse(
    UUID userId,
    String username,
    int point,
    String bankName,
    String accountNumber
) {
}
