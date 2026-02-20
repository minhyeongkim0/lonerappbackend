package com.hiddenloner.backend.mission.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PostDetailResponse(
    UUID id,
    UUID missionId,
    UUID userId,
    String authorMasked,
    String content,
    List<String> images,
    long likes,
    long commentCount,
    String status,
    OffsetDateTime createdAt
) {
}
