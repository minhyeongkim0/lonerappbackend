package com.hiddenloner.backend.admin.dto;

import java.util.UUID;

public record ApprovePostResponse(
    UUID postId,
    UUID userId,
    int grantedPoint,
    int userPoint,
    String status
) {
}
