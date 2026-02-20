package com.hiddenloner.backend.recommendation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecommendationItemResponse(
    UUID id,
    UUID userId,
    String authorMasked,
    String title,
    String content,
    long upvoteCount,
    long downvoteCount,
    long score,
    String myVote,
    OffsetDateTime createdAt
) {
}
