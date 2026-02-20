package com.hiddenloner.backend.dev.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.dev.dto.ApprovalSeedResponse;
import com.hiddenloner.backend.dev.dto.TestAccountBootstrapResponse;
import com.hiddenloner.backend.dev.service.TestAccountBootstrapService;

@RestController
@RequestMapping("/api/dev/test-accounts")
public class DevAccountController {

    private final TestAccountBootstrapService testAccountBootstrapService;

    public DevAccountController(TestAccountBootstrapService testAccountBootstrapService) {
        this.testAccountBootstrapService = testAccountBootstrapService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<TestAccountBootstrapResponse> bootstrap() {
        return ResponseEntity.ok(testAccountBootstrapService.bootstrap());
    }

    @PostMapping("/seed-approval-case")
    public ResponseEntity<ApprovalSeedResponse> seedApprovalCase() {
        return ResponseEntity.ok(testAccountBootstrapService.seedApprovalCase());
    }
}
