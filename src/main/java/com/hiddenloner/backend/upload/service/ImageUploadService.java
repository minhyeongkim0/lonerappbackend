package com.hiddenloner.backend.upload.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hiddenloner.backend.auth.client.SupabaseAuthClient;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.upload.client.SupabaseStorageClient;
import com.hiddenloner.backend.upload.dto.UploadImagesResponse;

@Service
public class ImageUploadService {

    private static final int MAX_FILES = 3;
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final DateTimeFormatter PATH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SupabaseAuthClient supabaseAuthClient;
    private final SupabaseStorageClient supabaseStorageClient;

    public ImageUploadService(SupabaseAuthClient supabaseAuthClient, SupabaseStorageClient supabaseStorageClient) {
        this.supabaseAuthClient = supabaseAuthClient;
        this.supabaseStorageClient = supabaseStorageClient;
    }

    public UploadImagesResponse uploadPostImages(List<MultipartFile> files, String authorizationHeader) {
        String accessToken = extractAccessToken(authorizationHeader);
        UUID userId = supabaseAuthClient.getUserIdFromAccessToken(accessToken);

        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED", "이미지는 최소 1장이 필요합니다.");
        }
        if (files.size() > MAX_FILES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_LIMIT_EXCEEDED", "이미지는 최대 3장까지 업로드할 수 있습니다.");
        }

        List<String> uploadedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            validateImageFile(file);
            String objectPath = buildObjectPath(userId, file.getOriginalFilename());
            try {
                String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
                supabaseStorageClient.upload(objectPath, file.getBytes(), contentType, accessToken);
                uploadedPaths.add(objectPath);
            } catch (IOException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_READ_ERROR", "이미지 파일을 읽을 수 없습니다.");
            }
        }

        return new UploadImagesResponse(uploadedPaths);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "비어있는 파일은 업로드할 수 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_SIZE_EXCEEDED", "파일 크기는 5MB 이하여야 합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE", "이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String buildObjectPath(UUID userId, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename == null ? "image" : originalFilename);
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).format(PATH_TIME_FORMATTER);
        return "posts/" + userId + "/" + timestamp + "_" + UUID.randomUUID() + "_" + safeName;
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        return token;
    }
}
