package com.hiddenloner.backend.mission.dto;

import java.util.UUID;

public record ToggleLikeResponse(
    UUID postId,
    boolean liked,
    long likeCount
) {
}
