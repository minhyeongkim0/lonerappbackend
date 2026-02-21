package com.hiddenloner.backend.refund.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundRequestResponse(
    UUID refundId,
    UUID userId,
    int amount,
    String status,
    String bankName,
    String accountNumber,
    OffsetDateTime createdAt,
    OffsetDateTime processedAt,
    String rejectedReason
) {
}
