package com.hiddenloner.backend.mission.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PostSummaryResponse(
    UUID id,
    UUID missionId,
    String authorMasked,
    String content,
    String thumbnail,
    long likes,
    long commentCount,
    String status,
    OffsetDateTime createdAt
) {
}
