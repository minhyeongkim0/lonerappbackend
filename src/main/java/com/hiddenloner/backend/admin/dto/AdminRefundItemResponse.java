package com.hiddenloner.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminRefundItemResponse(
    UUID refundId,
    UUID userId,
    String authorMasked,
    int amount,
    String bankName,
    String accountNumber,
    String status,
    OffsetDateTime createdAt
) {
}
