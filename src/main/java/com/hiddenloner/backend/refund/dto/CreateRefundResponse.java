package com.hiddenloner.backend.refund.dto;

public record CreateRefundResponse(
    RefundRequestResponse refund,
    int currentPoint
) {
}
