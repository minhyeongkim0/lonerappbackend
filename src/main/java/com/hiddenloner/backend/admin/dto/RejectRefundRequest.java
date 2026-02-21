package com.hiddenloner.backend.admin.dto;

import jakarta.validation.constraints.Size;

public record RejectRefundRequest(
    @Size(max = 200, message = "반려 사유는 200자 이하여야 합니다.")
    String reason
) {
}
