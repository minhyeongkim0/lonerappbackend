package com.hiddenloner.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.admin.dto.AdminMissionItemResponse;
import com.hiddenloner.backend.admin.dto.CreateMissionRequest;
import com.hiddenloner.backend.admin.dto.CreateMissionResponse;
import com.hiddenloner.backend.admin.service.AdminService;

@RestController
@RequestMapping("/api/admin/missions")
public class AdminMissionController {

    private final AdminService adminService;

    public AdminMissionController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<AdminMissionItemResponse>> getMissions(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(adminService.getMissions(authorizationHeader));
    }

    @PostMapping
    public ResponseEntity<CreateMissionResponse> createMission(
        @Validated @RequestBody CreateMissionRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.createMission(request, authorizationHeader));
    }
}
