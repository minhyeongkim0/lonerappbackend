package com.hiddenloner.backend.mission.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID postId,
    UUID userId,
    String authorMasked,
    String content,
    OffsetDateTime createdAt
) {
}
