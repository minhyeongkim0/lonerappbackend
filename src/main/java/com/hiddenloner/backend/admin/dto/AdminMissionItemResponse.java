package com.hiddenloner.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminMissionItemResponse(
    UUID id,
    String category,
    String title,
    String description,
    String verifyCondition,
    int rewardPoint,
    long postCount,
    boolean active,
    OffsetDateTime createdAt
) {
}
