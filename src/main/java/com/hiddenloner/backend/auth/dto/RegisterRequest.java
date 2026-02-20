package com.hiddenloner.backend.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 30, message = "아이디는 4~30자여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문, 숫자, 언더스코어만 사용할 수 있습니다.")
    String username,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 72, message = "비밀번호는 4~72자여야 합니다.")
    String password,

    @Min(value = 1900, message = "출생연도 범위를 확인해주세요.")
    @Max(value = 2100, message = "출생연도 범위를 확인해주세요.")
    int birthYear,

    @Min(value = 1, message = "출생월 범위를 확인해주세요.")
    @Max(value = 12, message = "출생월 범위를 확인해주세요.")
    int birthMonth,

    @Min(value = 1, message = "출생일 범위를 확인해주세요.")
    @Max(value = 31, message = "출생일 범위를 확인해주세요.")
    int birthDay,

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "^(male|female|none)$", message = "성별 값이 올바르지 않습니다.")
    String gender,

    @NotBlank(message = "은행명은 필수입니다.")
    String bankName,

    @NotBlank(message = "계좌번호는 필수입니다.")
    String accountNumber
) {
}
