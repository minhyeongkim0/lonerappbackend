package com.hiddenloner.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcessRefundResponse(
    UUID refundId,
    String status,
    int restoredPoint,
    int userPoint,
    OffsetDateTime processedAt,
    String rejectedReason
) {
}
