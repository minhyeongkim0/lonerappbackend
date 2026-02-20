package com.hiddenloner.backend.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMissionRequest(
    @NotBlank(message = "카테고리는 필수입니다.")
    @Size(max = 60, message = "카테고리는 60자 이하여야 합니다.")
    String category,

    @NotBlank(message = "미션명은 필수입니다.")
    @Size(max = 120, message = "미션명은 120자 이하여야 합니다.")
    String title,

    @NotBlank(message = "설명은 필수입니다.")
    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.")
    String description,

    @NotBlank(message = "인증 조건은 필수입니다.")
    @Size(max = 500, message = "인증 조건은 500자 이하여야 합니다.")
    String verifyCondition,

    @Min(value = 1, message = "포인트는 1 이상이어야 합니다.")
    int rewardPoint
) {
}
