package com.hiddenloner.backend.mission.dto;

import java.util.UUID;

public record MissionSummaryResponse(
    UUID id,
    String category,
    String title,
    int rewardPoint,
    long postCount,
    boolean completed
) {
}
