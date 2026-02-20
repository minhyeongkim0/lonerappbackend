package com.hiddenloner.backend.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.admin.dto.AdminRecommendationItemResponse;
import com.hiddenloner.backend.admin.service.AdminService;

@RestController
@RequestMapping("/api/admin/recommendations")
public class AdminRecommendationController {

    private final AdminService adminService;

    public AdminRecommendationController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<AdminRecommendationItemResponse>> getRecommendations(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(adminService.getRecommendations(authorizationHeader));
    }
}
