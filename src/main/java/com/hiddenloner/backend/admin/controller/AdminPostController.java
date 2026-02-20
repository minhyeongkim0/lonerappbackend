package com.hiddenloner.backend.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.admin.dto.AdminPostItemResponse;
import com.hiddenloner.backend.admin.dto.ApprovePostResponse;
import com.hiddenloner.backend.admin.service.AdminService;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final AdminService adminService;

    public AdminPostController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<AdminPostItemResponse>> getPendingPosts(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(adminService.getPendingPosts(authorizationHeader));
    }

    @PostMapping("/{postId}/approve")
    public ResponseEntity<ApprovePostResponse> approvePost(
        @PathVariable UUID postId,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(adminService.approvePost(postId, authorizationHeader));
    }
}
