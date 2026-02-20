package com.hiddenloner.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminRecommendationItemResponse(
    UUID id,
    UUID userId,
    String authorMasked,
    String title,
    String content,
    long upvoteCount,
    long downvoteCount,
    long score,
    OffsetDateTime createdAt
) {
}
