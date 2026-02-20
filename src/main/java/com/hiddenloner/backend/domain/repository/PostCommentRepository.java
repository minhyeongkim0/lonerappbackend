package com.hiddenloner.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hiddenloner.backend.domain.entity.PostComment;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
    List<PostComment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    long countByPostId(UUID postId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Query("""
        select c.postId as postId, count(c) as count
        from PostComment c
        where c.postId in :postIds
        group by c.postId
    """)
    List<PostCountRow> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    interface PostCountRow {
        UUID getPostId();
        long getCount();
    }
}
