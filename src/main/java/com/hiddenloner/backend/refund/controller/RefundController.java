package com.hiddenloner.backend.refund.controller;

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

import com.hiddenloner.backend.refund.dto.CreateRefundRequest;
import com.hiddenloner.backend.refund.dto.CreateRefundResponse;
import com.hiddenloner.backend.refund.dto.RefundRequestResponse;
import com.hiddenloner.backend.refund.service.RefundService;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ResponseEntity<List<RefundRequestResponse>> getMyRefunds(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(refundService.getMyRefunds(authorizationHeader));
    }

    @PostMapping
    public ResponseEntity<CreateRefundResponse> createRefund(
        @Validated @RequestBody CreateRefundRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(refundService.createRefund(request, authorizationHeader));
    }
}
