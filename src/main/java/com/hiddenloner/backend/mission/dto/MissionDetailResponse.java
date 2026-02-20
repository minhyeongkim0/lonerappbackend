package com.hiddenloner.backend.mission.dto;

import java.util.UUID;

public record MissionDetailResponse(
    UUID id,
    String category,
    String title,
    String description,
    String verifyCondition,
    int rewardPoint,
    long postCount,
    boolean completed
) {
}
