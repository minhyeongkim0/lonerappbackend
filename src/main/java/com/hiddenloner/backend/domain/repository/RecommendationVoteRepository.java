package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hiddenloner.backend.domain.entity.RecommendationVote;
import com.hiddenloner.backend.domain.entity.RecommendationVoteId;

public interface RecommendationVoteRepository extends JpaRepository<RecommendationVote, RecommendationVoteId> {
    Optional<RecommendationVote> findByIdPostIdAndIdUserId(UUID postId, UUID userId);

    @Query("""
        select v.id.postId as postId, count(v) as count
        from RecommendationVote v
        where v.id.postId in :postIds and v.voteType = :voteType
        group by v.id.postId
    """)
    List<VoteCountRow> countByPostIdsAndVoteType(@Param("postIds") Collection<UUID> postIds, @Param("voteType") short voteType);

    interface VoteCountRow {
        UUID getPostId();
        long getCount();
    }
}
