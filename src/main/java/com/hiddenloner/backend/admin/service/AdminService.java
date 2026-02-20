package com.hiddenloner.backend.admin.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiddenloner.backend.admin.dto.AdminPostItemResponse;
import com.hiddenloner.backend.admin.dto.ApprovePostResponse;
import com.hiddenloner.backend.admin.dto.AdminMissionItemResponse;
import com.hiddenloner.backend.admin.dto.CreateMissionRequest;
import com.hiddenloner.backend.admin.dto.CreateMissionResponse;
import com.hiddenloner.backend.admin.dto.AdminRecommendationItemResponse;
import com.hiddenloner.backend.auth.client.SupabaseAuthClient;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.config.AppAdminProperties;
import com.hiddenloner.backend.domain.entity.AppUser;
import com.hiddenloner.backend.domain.entity.Mission;
import com.hiddenloner.backend.domain.entity.PointHistory;
import com.hiddenloner.backend.domain.entity.Post;
import com.hiddenloner.backend.domain.repository.AppUserRepository;
import com.hiddenloner.backend.domain.repository.MissionRepository;
import com.hiddenloner.backend.domain.repository.PointHistoryRepository;
import com.hiddenloner.backend.domain.repository.PostRepository;
import com.hiddenloner.backend.recommendation.service.RecommendationService;

@Service
public class AdminService {

    private final PostRepository postRepository;
    private final MissionRepository missionRepository;
    private final AppUserRepository appUserRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SupabaseAuthClient supabaseAuthClient;
    private final AppAdminProperties appAdminProperties;
    private final RecommendationService recommendationService;

    public AdminService(
        PostRepository postRepository,
        MissionRepository missionRepository,
        AppUserRepository appUserRepository,
        PointHistoryRepository pointHistoryRepository,
        SupabaseAuthClient supabaseAuthClient,
        AppAdminProperties appAdminProperties,
        RecommendationService recommendationService
    ) {
        this.postRepository = postRepository;
        this.missionRepository = missionRepository;
        this.appUserRepository = appUserRepository;
        this.pointHistoryRepository = pointHistoryRepository;
        this.supabaseAuthClient = supabaseAuthClient;
        this.appAdminProperties = appAdminProperties;
        this.recommendationService = recommendationService;
    }

    @Transactional(readOnly = true)
    public List<AdminPostItemResponse> getPendingPosts(String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        List<Post> posts = postRepository.findByStatusOrderByCreatedAtDesc("pending");
        if (posts.isEmpty()) {
            return List.of();
        }

        Set<UUID> missionIds = new HashSet<>();
        Set<UUID> userIds = new HashSet<>();
        for (Post post : posts) {
            missionIds.add(post.getMissionId());
            userIds.add(post.getUserId());
        }

        Map<UUID, Mission> missionMap = new HashMap<>();
        for (Mission mission : missionRepository.findAllById(missionIds)) {
            missionMap.put(mission.getId(), mission);
        }

        Map<UUID, AppUser> userMap = new HashMap<>();
        for (AppUser user : appUserRepository.findAllByIdIn(userIds)) {
            userMap.put(user.getId(), user);
        }

        return posts.stream()
            .map(post -> {
                Mission mission = missionMap.get(post.getMissionId());
                AppUser user = userMap.get(post.getUserId());
                return new AdminPostItemResponse(
                    post.getId(),
                    post.getMissionId(),
                    mission == null ? "-" : mission.getTitle(),
                    post.getUserId(),
                    maskUsername(user == null ? "unknown" : user.getUsername()),
                    post.getContent(),
                    post.getStatus(),
                    post.getCreatedAt()
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminMissionItemResponse> getMissions(String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        List<Mission> missions = missionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        List<UUID> missionIds = missions.stream().map(Mission::getId).toList();
        Map<UUID, Long> postCountMap = new HashMap<>();
        for (PostRepository.MissionCountRow row : postRepository.countByMissionIds(missionIds)) {
            postCountMap.put(row.getMissionId(), row.getCount());
        }

        return missions.stream()
            .map(mission -> new AdminMissionItemResponse(
                mission.getId(),
                mission.getCategory(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getVerifyCondition(),
                mission.getRewardPoint(),
                postCountMap.getOrDefault(mission.getId(), 0L),
                mission.isActive(),
                mission.getCreatedAt()
            ))
            .toList();
    }

    @Transactional
    public CreateMissionResponse createMission(CreateMissionRequest request, String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        OffsetDateTime now = OffsetDateTime.now();
        Mission mission = new Mission();
        mission.setId(UUID.randomUUID());
        mission.setCategory(request.category().trim());
        mission.setTitle(request.title().trim());
        mission.setDescription(request.description().trim());
        mission.setVerifyCondition(request.verifyCondition().trim());
        mission.setRewardPoint(request.rewardPoint());
        mission.setActive(true);
        mission.setCreatedAt(now);
        missionRepository.save(mission);

        return new CreateMissionResponse(
            mission.getId(),
            mission.getCategory(),
            mission.getTitle(),
            mission.getDescription(),
            mission.getVerifyCondition(),
            mission.getRewardPoint(),
            0L,
            mission.isActive(),
            mission.getCreatedAt()
        );
    }

    @Transactional
    public ApprovePostResponse approvePost(UUID postId, String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);

        Post post = postRepository.findByIdForUpdate(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        if (!"pending".equals(post.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "POST_ALREADY_APPROVED", "이미 승인 처리된 게시글입니다.");
        }

        Mission mission = missionRepository.findById(post.getMissionId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MISSION_NOT_FOUND", "미션을 찾을 수 없습니다."));

        AppUser user = appUserRepository.findById(post.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        post.setStatus("approved");
        postRepository.save(post);

        int grantedPoint = mission.getRewardPoint();
        user.setPoint(user.getPoint() + grantedPoint);
        appUserRepository.save(user);

        PointHistory pointHistory = new PointHistory();
        pointHistory.setId(UUID.randomUUID());
        pointHistory.setUserId(user.getId());
        pointHistory.setAmount(grantedPoint);
        pointHistory.setReason("미션 인증 승인");
        pointHistory.setRefPostId(post.getId());
        pointHistory.setCreatedAt(OffsetDateTime.now());
        pointHistoryRepository.save(pointHistory);

        return new ApprovePostResponse(post.getId(), user.getId(), grantedPoint, user.getPoint(), post.getStatus());
    }

    @Transactional(readOnly = true)
    public List<AdminRecommendationItemResponse> getRecommendations(String authorizationHeader) {
        UUID adminUserId = resolveRequiredUserId(authorizationHeader);
        validateAdmin(adminUserId);
        return recommendationService.getRecommendationsForAdmin();
    }

    private UUID resolveRequiredUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        return supabaseAuthClient.getUserIdFromAccessToken(token);
    }

    private void validateAdmin(UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다."));

        List<String> admins = appAdminProperties.usernames() == null ? List.of() : appAdminProperties.usernames();
        boolean isAdmin = admins.stream()
            .map(name -> name == null ? "" : name.trim())
            .filter(name -> !name.isEmpty())
            .anyMatch(name -> name.equals(user.getUsername()));

        if (!isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다.");
        }
    }

    private String maskUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() <= 4) {
            return normalized + "_**";
        }
        return normalized.substring(0, 4) + "_**";
    }
}
