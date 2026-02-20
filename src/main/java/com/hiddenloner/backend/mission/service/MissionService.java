package com.hiddenloner.backend.mission.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiddenloner.backend.auth.client.SupabaseAuthClient;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.domain.entity.AppUser;
import com.hiddenloner.backend.domain.entity.Mission;
import com.hiddenloner.backend.domain.entity.Post;
import com.hiddenloner.backend.domain.entity.PostComment;
import com.hiddenloner.backend.domain.entity.PostImage;
import com.hiddenloner.backend.domain.entity.PostLike;
import com.hiddenloner.backend.domain.repository.AppUserRepository;
import com.hiddenloner.backend.domain.repository.MissionRepository;
import com.hiddenloner.backend.domain.repository.PostCommentRepository;
import com.hiddenloner.backend.domain.repository.PostImageRepository;
import com.hiddenloner.backend.domain.repository.PostLikeRepository;
import com.hiddenloner.backend.domain.repository.PostRepository;
import com.hiddenloner.backend.mission.dto.CommentResponse;
import com.hiddenloner.backend.mission.dto.CreatePostRequest;
import com.hiddenloner.backend.mission.dto.CreatePostResponse;
import com.hiddenloner.backend.mission.dto.CreateCommentRequest;
import com.hiddenloner.backend.mission.dto.MissionDetailResponse;
import com.hiddenloner.backend.mission.dto.MissionSummaryResponse;
import com.hiddenloner.backend.mission.dto.PostDetailResponse;
import com.hiddenloner.backend.mission.dto.PostSummaryResponse;
import com.hiddenloner.backend.mission.dto.ToggleLikeResponse;
import com.hiddenloner.backend.upload.client.SupabaseStorageClient;

@Service
public class MissionService {

    private final MissionRepository missionRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final AppUserRepository appUserRepository;
    private final SupabaseAuthClient supabaseAuthClient;
    private final SupabaseStorageClient supabaseStorageClient;

    public MissionService(
        MissionRepository missionRepository,
        PostRepository postRepository,
        PostImageRepository postImageRepository,
        PostLikeRepository postLikeRepository,
        PostCommentRepository postCommentRepository,
        AppUserRepository appUserRepository,
        SupabaseAuthClient supabaseAuthClient,
        SupabaseStorageClient supabaseStorageClient
    ) {
        this.missionRepository = missionRepository;
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.appUserRepository = appUserRepository;
        this.supabaseAuthClient = supabaseAuthClient;
        this.supabaseStorageClient = supabaseStorageClient;
    }

    @Transactional(readOnly = true)
    public List<MissionSummaryResponse> getMissions(String authorizationHeader) {
        UUID userId = resolveOptionalUserId(authorizationHeader);

        List<Mission> missions = missionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        List<UUID> missionIds = missions.stream().map(Mission::getId).toList();

        Map<UUID, Long> missionPostCounts = toMissionCountMap(postRepository.countByMissionIds(missionIds));
        Set<UUID> completedMissionIds = userId == null
            ? Collections.emptySet()
            : new HashSet<>(postRepository.findCompletedMissionIds(userId));

        return missions.stream()
            .map(mission -> new MissionSummaryResponse(
                mission.getId(),
                mission.getCategory(),
                mission.getTitle(),
                mission.getRewardPoint(),
                missionPostCounts.getOrDefault(mission.getId(), 0L),
                completedMissionIds.contains(mission.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public MissionDetailResponse getMission(UUID missionId, String authorizationHeader) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        Mission mission = getActiveMissionOrThrow(missionId);

        long postCount = postRepository.findByMissionIdOrderByCreatedAtDesc(missionId).size();
        boolean completed = userId != null && postRepository.existsByMissionIdAndUserId(missionId, userId);

        return new MissionDetailResponse(
            mission.getId(),
            mission.getCategory(),
            mission.getTitle(),
            mission.getDescription(),
            mission.getVerifyCondition(),
            mission.getRewardPoint(),
            postCount,
            completed
        );
    }

    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getMissionPosts(UUID missionId) {
        getActiveMissionOrThrow(missionId);

        List<Post> posts = postRepository.findByMissionIdOrderByCreatedAtDesc(missionId);
        return toPostSummaryResponses(posts);
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        Mission mission = getActiveMissionOrThrow(post.getMissionId());
        AppUser user = appUserRepository.findById(post.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));

        long likes = postLikeRepository.countByPostId(postId);
        long commentCount = postCommentRepository.countByPostIds(List.of(postId)).stream()
            .findFirst().map(PostCommentRepository.PostCountRow::getCount).orElse(0L);
        List<String> images = postImageRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
            .map(PostImage::getStoragePath)
            .map(this::toAccessibleImageUrl)
            .toList();

        return new PostDetailResponse(
            post.getId(),
            mission.getId(),
            post.getUserId(),
            maskUsername(user.getUsername()),
            post.getContent(),
            images,
            likes,
            commentCount,
            post.getStatus(),
            post.getCreatedAt()
        );
    }

    @Transactional
    public CreatePostResponse createMissionPost(UUID missionId, CreatePostRequest request, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);
        getActiveMissionOrThrow(missionId);

        if (request.imagePaths() == null || request.imagePaths().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED", "이미지는 최소 1장이 필요합니다.");
        }

        if (postRepository.existsByMissionIdAndUserId(missionId, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "MISSION_ALREADY_COMPLETED", "이미 완료한 미션입니다.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        UUID postId = UUID.randomUUID();

        Post post = new Post();
        post.setId(postId);
        post.setMissionId(missionId);
        post.setUserId(userId);
        post.setContent(request.content().trim());
        post.setStatus("pending");
        post.setCreatedAt(now);

        postRepository.save(post);

        for (int i = 0; i < request.imagePaths().size(); i++) {
            String path = request.imagePaths().get(i);
            PostImage postImage = new PostImage();
            postImage.setId(UUID.randomUUID());
            postImage.setPostId(postId);
            postImage.setStoragePath(path);
            postImage.setSortOrder((short) i);
            postImage.setCreatedAt(now);
            postImageRepository.save(postImage);
        }

        return new CreatePostResponse(postId, post.getStatus(), post.getCreatedAt());
    }

    @Transactional
    public ToggleLikeResponse togglePostLike(UUID postId, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);

        postRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        boolean liked;
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            liked = false;
        } else {
            PostLike postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setUserId(userId);
            postLike.setCreatedAt(OffsetDateTime.now());
            postLikeRepository.save(postLike);
            liked = true;
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        return new ToggleLikeResponse(postId, liked, likeCount);
    }

    @Transactional
    public CommentResponse createComment(UUID postId, CreateCommentRequest request, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));

        PostComment comment = new PostComment();
        comment.setId(UUID.randomUUID());
        comment.setPostId(post.getId());
        comment.setUserId(userId);
        comment.setContent(request.content().trim());
        comment.setCreatedAt(OffsetDateTime.now());

        PostComment saved = postCommentRepository.save(comment);
        return new CommentResponse(
            saved.getId(),
            saved.getPostId(),
            saved.getUserId(),
            maskUsername(user.getUsername()),
            saved.getContent(),
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID postId) {
        postRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        List<PostComment> comments = postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Set<UUID> userIds = comments.stream()
            .map(PostComment::getUserId)
            .collect(Collectors.toSet());
        Map<UUID, String> usernameMap = appUserRepository.findAllByIdIn(userIds).stream()
            .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));

        return comments.stream()
            .map(comment -> new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                maskUsername(usernameMap.getOrDefault(comment.getUserId(), "unknown")),
                comment.getContent(),
                comment.getCreatedAt()
            ))
            .toList();
    }

    @Transactional
    public void deleteComment(UUID postId, UUID commentId, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);
        postRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        PostComment comment = postCommentRepository.findById(commentId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));

        if (!comment.getPostId().equals(postId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMMENT_DELETE_FORBIDDEN", "본인 댓글만 삭제할 수 있습니다.");
        }

        postCommentRepository.delete(comment);
    }

    private Mission getActiveMissionOrThrow(UUID missionId) {
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MISSION_NOT_FOUND", "미션을 찾을 수 없습니다."));

        if (!mission.isActive()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MISSION_NOT_FOUND", "미션을 찾을 수 없습니다.");
        }

        return mission;
    }

    private List<PostSummaryResponse> toPostSummaryResponses(List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Set<UUID> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());

        Map<UUID, Long> likeCountMap = toLikeCountMap(postLikeRepository.countByPostIds(postIds));
        Map<UUID, Long> commentCountMap = toCommentCountMap(postCommentRepository.countByPostIds(postIds));
        Map<UUID, List<String>> imageMap = toPostImageMap(postImageRepository.findByPostIdInOrderByPostIdAscSortOrderAsc(postIds));

        Map<UUID, String> usernameMap = appUserRepository.findAllByIdIn(userIds).stream()
            .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));

        return posts.stream()
            .map(post -> new PostSummaryResponse(
                post.getId(),
                post.getMissionId(),
                maskUsername(usernameMap.getOrDefault(post.getUserId(), "unknown")),
                post.getContent(),
                imageMap.getOrDefault(post.getId(), List.of()).stream().findFirst().orElse(null),
                likeCountMap.getOrDefault(post.getId(), 0L),
                commentCountMap.getOrDefault(post.getId(), 0L),
                post.getStatus(),
                post.getCreatedAt()
            ))
            .toList();
    }

    private Map<UUID, Long> toMissionCountMap(List<PostRepository.MissionCountRow> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (PostRepository.MissionCountRow row : rows) {
            map.put(row.getMissionId(), row.getCount());
        }
        return map;
    }

    private Map<UUID, Long> toLikeCountMap(List<PostLikeRepository.PostCountRow> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (PostLikeRepository.PostCountRow row : rows) {
            map.put(row.getPostId(), row.getCount());
        }
        return map;
    }

    private Map<UUID, Long> toCommentCountMap(List<PostCommentRepository.PostCountRow> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (PostCommentRepository.PostCountRow row : rows) {
            map.put(row.getPostId(), row.getCount());
        }
        return map;
    }

    private Map<UUID, List<String>> toPostImageMap(List<PostImage> images) {
        Map<UUID, List<String>> imageMap = new HashMap<>();
        for (PostImage image : images) {
            imageMap.computeIfAbsent(image.getPostId(), ignored -> new java.util.ArrayList<>())
                .add(image.getStoragePath());
        }
        return imageMap;
    }

    private UUID resolveOptionalUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Authorization 헤더 형식이 올바르지 않습니다.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }

        return supabaseAuthClient.getUserIdFromAccessToken(token);
    }

    private UUID resolveRequiredUserId(String authorizationHeader) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        return userId;
    }

    private String maskUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() <= 4) {
            return normalized + "_**";
        }
        return normalized.substring(0, 4) + "_**";
    }

    private String toAccessibleImageUrl(String storagePath) {
        try {
            return supabaseStorageClient.createSignedUrl(storagePath, 3600);
        } catch (Exception ex) {
            return storagePath;
        }
    }
}
