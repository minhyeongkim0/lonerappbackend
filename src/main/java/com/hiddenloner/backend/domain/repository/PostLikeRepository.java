package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hiddenloner.backend.domain.entity.PostLike;
import com.hiddenloner.backend.domain.entity.PostLikeId;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    @Query("""
        select pl.postId as postId, count(pl) as count
        from PostLike pl
        where pl.postId in :postIds
        group by pl.postId
    """)
    List<PostCountRow> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    interface PostCountRow {
        UUID getPostId();
        long getCount();
    }
}
