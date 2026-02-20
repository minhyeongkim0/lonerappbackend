package com.hiddenloner.backend.mission.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hiddenloner.backend.mission.dto.CommentResponse;
import com.hiddenloner.backend.mission.dto.CreateCommentRequest;
import com.hiddenloner.backend.mission.dto.PostDetailResponse;
import com.hiddenloner.backend.mission.dto.ToggleLikeResponse;
import com.hiddenloner.backend.mission.service.MissionService;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final MissionService missionService;

    public PostController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(missionService.getPost(postId));
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<ToggleLikeResponse> toggleLike(
        @PathVariable UUID postId,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(missionService.togglePostLike(postId, authorizationHeader));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID postId) {
        return ResponseEntity.ok(missionService.getComments(postId));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
        @PathVariable UUID postId,
        @Validated @RequestBody CreateCommentRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(missionService.createComment(postId, request, authorizationHeader));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable UUID postId,
        @PathVariable UUID commentId,
        @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        missionService.deleteComment(postId, commentId, authorizationHeader);
        return ResponseEntity.noContent().build();
    }
}
