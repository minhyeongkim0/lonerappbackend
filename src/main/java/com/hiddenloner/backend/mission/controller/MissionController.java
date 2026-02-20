package com.hiddenloner.backend.mission.controller;

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

import com.hiddenloner.backend.mission.dto.CreatePostRequest;
import com.hiddenloner.backend.mission.dto.CreatePostResponse;
import com.hiddenloner.backend.mission.dto.MissionDetailResponse;
import com.hiddenloner.backend.mission.dto.MissionSummaryResponse;
import com.hiddenloner.backend.mission.dto.PostSummaryResponse;
import com.hiddenloner.backend.mission.service.MissionService;

@RestController
@RequestMapping("/api")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping("/missions")
    public ResponseEntity<List<MissionSummaryResponse>> getMissions(
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(missionService.getMissions(authorizationHeader));
    }

    @GetMapping("/missions/{missionId}")
    public ResponseEntity<MissionDetailResponse> getMission(
        @PathVariable UUID missionId,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(missionService.getMission(missionId, authorizationHeader));
    }

    @GetMapping("/missions/{missionId}/posts")
    public ResponseEntity<List<PostSummaryResponse>> getMissionPosts(@PathVariable UUID missionId) {
        return ResponseEntity.ok(missionService.getMissionPosts(missionId));
    }

    @PostMapping("/missions/{missionId}/posts")
    public ResponseEntity<CreatePostResponse> createMissionPost(
        @PathVariable UUID missionId,
        @Validated @RequestBody CreatePostRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(missionService.createMissionPost(missionId, request, authorizationHeader));
    }
}
