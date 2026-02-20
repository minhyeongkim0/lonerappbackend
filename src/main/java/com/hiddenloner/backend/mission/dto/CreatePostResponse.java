package com.hiddenloner.backend.mission.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePostResponse(
    UUID postId,
    String status,
    OffsetDateTime createdAt
) {
}
