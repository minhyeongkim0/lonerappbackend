package com.hiddenloner.backend.upload.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hiddenloner.backend.upload.dto.UploadImagesResponse;
import com.hiddenloner.backend.upload.service.ImageUploadService;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageUploadService imageUploadService;

    public UploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/post-images")
    public ResponseEntity<UploadImagesResponse> uploadPostImages(
        @RequestParam("files") List<MultipartFile> files,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(imageUploadService.uploadPostImages(files, authorizationHeader));
    }
}
