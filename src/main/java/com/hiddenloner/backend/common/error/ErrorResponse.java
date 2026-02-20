package com.hiddenloner.backend.common.error;

import java.time.OffsetDateTime;

public record ErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp
) {
}
