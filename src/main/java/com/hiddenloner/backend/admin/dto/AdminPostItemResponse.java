package com.hiddenloner.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPostItemResponse(
    UUID postId,
    UUID missionId,
    String missionTitle,
    UUID userId,
    String authorMasked,
    String content,
    String status,
    OffsetDateTime createdAt
) {
}
