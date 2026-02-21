package com.hiddenloner.backend.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.admin.dto.AdminRefundItemResponse;
import com.hiddenloner.backend.admin.dto.ProcessRefundResponse;
import com.hiddenloner.backend.admin.dto.RejectRefundRequest;
import com.hiddenloner.backend.refund.service.RefundService;

@RestController
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {

    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ResponseEntity<List<AdminRefundItemResponse>> getPendingRefunds(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(refundService.getPendingRefunds(authorizationHeader));
    }

    @PostMapping("/{refundId}/approve")
    public ResponseEntity<ProcessRefundResponse> approveRefund(
        @PathVariable UUID refundId,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(refundService.approveRefund(refundId, authorizationHeader));
    }

    @PostMapping("/{refundId}/reject")
    public ResponseEntity<ProcessRefundResponse> rejectRefund(
        @PathVariable UUID refundId,
        @Validated @RequestBody(required = false) RejectRefundRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(refundService.rejectRefund(refundId, request, authorizationHeader));
    }
}
