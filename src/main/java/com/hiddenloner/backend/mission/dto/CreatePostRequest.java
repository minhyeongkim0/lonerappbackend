package com.hiddenloner.backend.mission.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
    String content,

    @Size(min = 1, max = 3, message = "이미지는 1~3장이어야 합니다.")
    List<@NotBlank(message = "이미지 경로는 비어 있을 수 없습니다.") String> imagePaths
) {
}
