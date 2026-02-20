package com.hiddenloner.backend.dev.dto;

import java.util.UUID;

public record ApprovalSeedResponse(
    UUID missionId,
    UUID postId,
    UUID userId,
    String postStatus
) {
}
