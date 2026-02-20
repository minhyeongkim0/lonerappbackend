package com.hiddenloner.backend.dev.dto;

import java.util.List;

public record TestAccountBootstrapResponse(
    List<String> created,
    List<String> skipped,
    List<String> failed
) {
}
