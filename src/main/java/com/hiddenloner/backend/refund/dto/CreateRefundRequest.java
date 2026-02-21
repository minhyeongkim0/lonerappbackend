package com.hiddenloner.backend.refund.dto;

import jakarta.validation.constraints.Min;

public record CreateRefundRequest(
    @Min(value = 1, message = "환급 금액은 1원 이상이어야 합니다.")
    int amount
) {
}
