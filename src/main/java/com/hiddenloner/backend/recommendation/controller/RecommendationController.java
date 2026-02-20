package com.hiddenloner.backend.recommendation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.recommendation.dto.CreateRecommendationRequest;
import com.hiddenloner.backend.recommendation.dto.RecommendationItemResponse;
import com.hiddenloner.backend.recommendation.dto.VoteRecommendationRequest;
import com.hiddenloner.backend.recommendation.dto.VoteRecommendationResponse;
import com.hiddenloner.backend.recommendation.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<RecommendationItemResponse>> getRecommendations(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(recommendationService.getRecommendations(authorizationHeader));
    }

    @PostMapping
    public ResponseEntity<RecommendationItemResponse> createRecommendation(
        @Validated @RequestBody CreateRecommendationRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(recommendationService.createRecommendation(request, authorizationHeader));
    }

    @PostMapping("/{postId}/vote")
    public ResponseEntity<VoteRecommendationResponse> vote(
        @PathVariable UUID postId,
        @Validated @RequestBody VoteRecommendationRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(recommendationService.vote(postId, request, authorizationHeader));
    }
}
