package com.hiddenloner.backend.dev.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiddenloner.backend.auth.dto.RegisterRequest;
import com.hiddenloner.backend.auth.service.AuthService;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.config.AppDevProperties;
import com.hiddenloner.backend.dev.dto.ApprovalSeedResponse;
import com.hiddenloner.backend.dev.dto.TestAccountBootstrapResponse;
import com.hiddenloner.backend.domain.entity.AppUser;
import com.hiddenloner.backend.domain.entity.Mission;
import com.hiddenloner.backend.domain.entity.Post;
import com.hiddenloner.backend.domain.repository.AppUserRepository;
import com.hiddenloner.backend.domain.repository.MissionRepository;
import com.hiddenloner.backend.domain.repository.PostRepository;

@Service
public class TestAccountBootstrapService {

    private final AppDevProperties appDevProperties;
    private final AppUserRepository appUserRepository;
    private final AuthService authService;
    private final MissionRepository missionRepository;
    private final PostRepository postRepository;

    public TestAccountBootstrapService(
        AppDevProperties appDevProperties,
        AppUserRepository appUserRepository,
        AuthService authService,
        MissionRepository missionRepository,
        PostRepository postRepository
    ) {
        this.appDevProperties = appDevProperties;
        this.appUserRepository = appUserRepository;
        this.authService = authService;
        this.missionRepository = missionRepository;
        this.postRepository = postRepository;
    }

    public TestAccountBootstrapResponse bootstrap() {
        ensureBootstrapEnabled();

        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (SeedAccount seed : seedAccounts()) {
            if (appUserRepository.existsByUsername(seed.username())) {
                skipped.add(seed.username());
                continue;
            }

            try {
                authService.register(new RegisterRequest(
                    seed.username(),
                    seed.password(),
                    seed.birthYear(),
                    seed.birthMonth(),
                    seed.birthDay(),
                    seed.gender(),
                    seed.bankName(),
                    seed.accountNumber()
                ));
                created.add(seed.username());
            } catch (Exception ex) {
                failed.add(seed.username() + ": " + ex.getMessage());
            }
        }

        return new TestAccountBootstrapResponse(created, skipped, failed);
    }

    @Transactional
    public ApprovalSeedResponse seedApprovalCase() {
        ensureBootstrapEnabled();

        AppUser user1 = appUserRepository.findByUsername("user1")
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user1 계정이 없습니다. bootstrap을 먼저 실행하세요."));

        Post existingPendingPost = postRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user1.getId(), "pending")
            .orElse(null);

        if (existingPendingPost != null) {
            return new ApprovalSeedResponse(existingPendingPost.getMissionId(), existingPendingPost.getId(), existingPendingPost.getUserId(), existingPendingPost.getStatus());
        }

        Mission mission = missionRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream().findFirst().orElse(null);
        if (mission == null) {
            mission = new Mission();
            mission.setId(UUID.randomUUID());
            mission.setCategory("테스트");
            mission.setTitle("관리자 승인 테스트 미션");
            mission.setDescription("관리자 포인트 지급 트랜잭션 검증용");
            mission.setVerifyCondition("테스트 게시글 승인");
            mission.setRewardPoint(2000);
            mission.setActive(true);
            mission.setCreatedAt(OffsetDateTime.now());
            missionRepository.save(mission);
        }

        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setMissionId(mission.getId());
        post.setUserId(user1.getId());
        post.setContent("관리자 승인 테스트용 pending 게시글");
        post.setStatus("pending");
        post.setCreatedAt(OffsetDateTime.now());
        postRepository.save(post);

        return new ApprovalSeedResponse(mission.getId(), post.getId(), post.getUserId(), post.getStatus());
    }

    private void ensureBootstrapEnabled() {
        if (!appDevProperties.bootstrapEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BOOTSTRAP_DISABLED", "테스트 계정 부트스트랩이 비활성화되어 있습니다.");
        }
    }

    private List<SeedAccount> seedAccounts() {
        return List.of(
            new SeedAccount("user1", "123456", 2000, 1, 1, "male", "농협은행", "352-1743-2172-23"),
            new SeedAccount("user2", "123456", 1999, 1, 1, "male", "국민은행", "123-4567-7890-12"),
            new SeedAccount("master", "123456", 1998, 1, 1, "none", "신한은행", "999-0000-1111-22")
        );
    }

    private record SeedAccount(
        String username,
        String password,
        int birthYear,
        int birthMonth,
        int birthDay,
        String gender,
        String bankName,
        String accountNumber
    ) {
    }
}
