package com.hiddenloner.backend.upload.dto;

import java.util.List;

public record UploadImagesResponse(
    List<String> paths
) {
}
