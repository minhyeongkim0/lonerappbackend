package com.hiddenloner.backend.recommendation.dto;

import jakarta.validation.constraints.NotBlank;

public record VoteRecommendationRequest(
    @NotBlank(message = "voteType은 필수입니다.")
    String voteType
) {
}
