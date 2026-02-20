package com.hiddenloner.backend.recommendation.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiddenloner.backend.admin.dto.AdminRecommendationItemResponse;
import com.hiddenloner.backend.auth.client.SupabaseAuthClient;
import com.hiddenloner.backend.common.error.ApiException;
import com.hiddenloner.backend.domain.entity.AppUser;
import com.hiddenloner.backend.domain.entity.RecommendationPost;
import com.hiddenloner.backend.domain.entity.RecommendationVote;
import com.hiddenloner.backend.domain.entity.RecommendationVoteId;
import com.hiddenloner.backend.domain.repository.AppUserRepository;
import com.hiddenloner.backend.domain.repository.RecommendationPostRepository;
import com.hiddenloner.backend.domain.repository.RecommendationVoteRepository;
import com.hiddenloner.backend.recommendation.dto.CreateRecommendationRequest;
import com.hiddenloner.backend.recommendation.dto.RecommendationItemResponse;
import com.hiddenloner.backend.recommendation.dto.VoteRecommendationRequest;
import com.hiddenloner.backend.recommendation.dto.VoteRecommendationResponse;

@Service
public class RecommendationService {

    private static final short VOTE_UP = 1;
    private static final short VOTE_DOWN = -1;

    private final RecommendationPostRepository recommendationPostRepository;
    private final RecommendationVoteRepository recommendationVoteRepository;
    private final AppUserRepository appUserRepository;
    private final SupabaseAuthClient supabaseAuthClient;

    public RecommendationService(
        RecommendationPostRepository recommendationPostRepository,
        RecommendationVoteRepository recommendationVoteRepository,
        AppUserRepository appUserRepository,
        SupabaseAuthClient supabaseAuthClient
    ) {
        this.recommendationPostRepository = recommendationPostRepository;
        this.recommendationVoteRepository = recommendationVoteRepository;
        this.appUserRepository = appUserRepository;
        this.supabaseAuthClient = supabaseAuthClient;
    }

    @Transactional(readOnly = true)
    public List<RecommendationItemResponse> getRecommendations(String authorizationHeader) {
        UUID currentUserId = resolveOptionalUserId(authorizationHeader);
        List<RecommendationPost> posts = recommendationPostRepository.findAllByOrderByCreatedAtDesc();
        if (posts.isEmpty()) {
            return List.of();
        }

        Map<UUID, VoteAggregate> aggregateMap = buildVoteAggregateMap(posts);
        Map<UUID, String> usernameMap = buildUsernameMap(posts);

        List<RecommendationItemResponse> responses = new ArrayList<>();
        for (RecommendationPost post : posts) {
            VoteAggregate aggregate = aggregateMap.getOrDefault(post.getId(), new VoteAggregate(0L, 0L));
            String myVote = null;
            if (currentUserId != null) {
                RecommendationVote myVoteEntity = recommendationVoteRepository
                    .findByIdPostIdAndIdUserId(post.getId(), currentUserId)
                    .orElse(null);
                if (myVoteEntity != null) {
                    myVote = myVoteEntity.getVoteType() == VOTE_UP ? "up" : "down";
                }
            }

            responses.add(new RecommendationItemResponse(
                post.getId(),
                post.getUserId(),
                maskUsername(usernameMap.getOrDefault(post.getUserId(), "unknown")),
                post.getTitle(),
                post.getContent(),
                aggregate.upvoteCount(),
                aggregate.downvoteCount(),
                aggregate.score(),
                myVote,
                post.getCreatedAt()
            ));
        }

        responses.sort(Comparator
            .comparingLong(RecommendationItemResponse::upvoteCount).reversed()
            .thenComparingLong(RecommendationItemResponse::score).reversed()
            .thenComparing(RecommendationItemResponse::createdAt).reversed());
        return responses;
    }

    @Transactional
    public RecommendationItemResponse createRecommendation(CreateRecommendationRequest request, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);

        RecommendationPost post = new RecommendationPost();
        post.setId(UUID.randomUUID());
        post.setUserId(userId);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setCreatedAt(OffsetDateTime.now());
        recommendationPostRepository.save(post);

        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));

        return new RecommendationItemResponse(
            post.getId(),
            post.getUserId(),
            maskUsername(user.getUsername()),
            post.getTitle(),
            post.getContent(),
            0L,
            0L,
            0L,
            null,
            post.getCreatedAt()
        );
    }

    @Transactional
    public VoteRecommendationResponse vote(UUID postId, VoteRecommendationRequest request, String authorizationHeader) {
        UUID userId = resolveRequiredUserId(authorizationHeader);
        RecommendationPost post = recommendationPostRepository.findById(postId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECOMMENDATION_NOT_FOUND", "추천 글을 찾을 수 없습니다."));

        recommendationVoteRepository.findByIdPostIdAndIdUserId(post.getId(), userId).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_VOTED", "추천/비추천은 한 번만 가능합니다.");
        });

        short voteType = parseVoteType(request.voteType());
        RecommendationVote vote = new RecommendationVote();
        vote.setId(new RecommendationVoteId(post.getId(), userId));
        vote.setVoteType(voteType);
        vote.setCreatedAt(OffsetDateTime.now());
        recommendationVoteRepository.save(vote);

        long upvoteCount = countVotesByType(post.getId(), VOTE_UP);
        long downvoteCount = countVotesByType(post.getId(), VOTE_DOWN);

        return new VoteRecommendationResponse(
            post.getId(),
            voteType == VOTE_UP ? "up" : "down",
            upvoteCount,
            downvoteCount,
            upvoteCount - downvoteCount
        );
    }

    @Transactional(readOnly = true)
    public List<AdminRecommendationItemResponse> getRecommendationsForAdmin() {
        List<RecommendationPost> posts = recommendationPostRepository.findAllByOrderByCreatedAtDesc();
        if (posts.isEmpty()) {
            return List.of();
        }

        Map<UUID, VoteAggregate> aggregateMap = buildVoteAggregateMap(posts);
        Map<UUID, String> usernameMap = buildUsernameMap(posts);

        List<AdminRecommendationItemResponse> responses = posts.stream()
            .map(post -> {
                VoteAggregate aggregate = aggregateMap.getOrDefault(post.getId(), new VoteAggregate(0L, 0L));
                return new AdminRecommendationItemResponse(
                    post.getId(),
                    post.getUserId(),
                    maskUsername(usernameMap.getOrDefault(post.getUserId(), "unknown")),
                    post.getTitle(),
                    post.getContent(),
                    aggregate.upvoteCount(),
                    aggregate.downvoteCount(),
                    aggregate.score(),
                    post.getCreatedAt()
                );
            })
            .collect(Collectors.toCollection(ArrayList::new));

        responses.sort(Comparator
            .comparingLong(AdminRecommendationItemResponse::upvoteCount).reversed()
            .thenComparingLong(AdminRecommendationItemResponse::score).reversed()
            .thenComparing(AdminRecommendationItemResponse::createdAt).reversed());
        return responses;
    }

    private Map<UUID, VoteAggregate> buildVoteAggregateMap(List<RecommendationPost> posts) {
        List<UUID> postIds = posts.stream().map(RecommendationPost::getId).toList();
        Map<UUID, Long> upMap = toVoteCountMap(recommendationVoteRepository.countByPostIdsAndVoteType(postIds, VOTE_UP));
        Map<UUID, Long> downMap = toVoteCountMap(recommendationVoteRepository.countByPostIdsAndVoteType(postIds, VOTE_DOWN));

        Map<UUID, VoteAggregate> aggregateMap = new HashMap<>();
        for (UUID postId : postIds) {
            long up = upMap.getOrDefault(postId, 0L);
            long down = downMap.getOrDefault(postId, 0L);
            aggregateMap.put(postId, new VoteAggregate(up, down));
        }
        return aggregateMap;
    }

    private Map<UUID, Long> toVoteCountMap(List<RecommendationVoteRepository.VoteCountRow> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (RecommendationVoteRepository.VoteCountRow row : rows) {
            map.put(row.getPostId(), row.getCount());
        }
        return map;
    }

    private Map<UUID, String> buildUsernameMap(List<RecommendationPost> posts) {
        List<UUID> userIds = posts.stream().map(RecommendationPost::getUserId).distinct().toList();
        return appUserRepository.findAllByIdIn(new java.util.HashSet<>(userIds)).stream()
            .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));
    }

    private long countVotesByType(UUID postId, short voteType) {
        return recommendationVoteRepository.countByPostIdsAndVoteType(List.of(postId), voteType).stream()
            .findFirst()
            .map(RecommendationVoteRepository.VoteCountRow::getCount)
            .orElse(0L);
    }

    private short parseVoteType(String rawVoteType) {
        String normalized = rawVoteType == null ? "" : rawVoteType.trim().toLowerCase();
        return switch (normalized) {
            case "up" -> VOTE_UP;
            case "down" -> VOTE_DOWN;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VOTE_TYPE", "voteType은 up/down만 가능합니다.");
        };
    }

    private UUID resolveRequiredUserId(String authorizationHeader) {
        UUID userId = resolveOptionalUserId(authorizationHeader);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        return userId;
    }

    private UUID resolveOptionalUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
        return supabaseAuthClient.getUserIdFromAccessToken(token);
    }

    private String maskUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() <= 4) {
            return normalized + "_**";
        }
        return normalized.substring(0, 4) + "_**";
    }

    private record VoteAggregate(long upvoteCount, long downvoteCount) {
        long score() {
            return upvoteCount - downvoteCount;
        }
    }
}
