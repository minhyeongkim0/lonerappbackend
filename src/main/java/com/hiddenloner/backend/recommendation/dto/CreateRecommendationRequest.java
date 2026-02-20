package com.hiddenloner.backend.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecommendationRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 120, message = "제목은 120자 이하여야 합니다.")
    String title,

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    String content
) {
}
